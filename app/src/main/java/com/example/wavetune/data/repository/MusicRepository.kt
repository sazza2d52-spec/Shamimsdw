package com.example.wavetune.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.wavetune.data.local.FavoritesDao
import com.example.wavetune.data.local.VaultDao
import com.example.wavetune.data.model.Album
import com.example.wavetune.data.model.Artist
import com.example.wavetune.data.model.FavoriteEntity
import com.example.wavetune.data.model.Song
import com.example.wavetune.data.model.SongSortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(
    private val context: Context,
    private val favoritesDao: FavoritesDao,
    private val vaultDao: VaultDao
) {
    private val _rawSongs = MutableStateFlow<List<Song>>(emptyList())
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    val allSongs: Flow<List<Song>> = combine(
        _rawSongs,
        favoritesDao.getFavoriteSongs(),
        vaultDao.getVaultSongIds()
    ) { songs, favs, vaultIds ->
        val favKeys = favs.map { it.itemId }.toSet()
        val vaultIdSet = vaultIds.toSet()
        songs.map { song ->
            song.copy(
                isFavorite = favKeys.contains(song.id.toString()),
                isVaultProtected = vaultIdSet.contains(song.id)
            )
        }
    }

    suspend fun scanMusic(includeDemoFallbackIfEmpty: Boolean = true): List<Song> = withContext(Dispatchers.IO) {
        _isScanning.value = true
        val songList = mutableListOf<Song>()

        try {
            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.SIZE
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            context.contentResolver.query(
                uri,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val trackCol = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
                val yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Track"
                    val artist = cursor.getString(artistCol)?.takeIf { it != "<unknown>" } ?: "Unknown Artist"
                    val album = cursor.getString(albumCol)?.takeIf { it != "<unknown>" } ?: "Unknown Album"
                    val albumId = cursor.getLong(albumIdCol)
                    val duration = cursor.getLong(durationCol)
                    val dataPath = cursor.getString(dataCol) ?: ""
                    val trackNumber = if (trackCol != -1) cursor.getInt(trackCol) else 0
                    val year = if (yearCol != -1) cursor.getInt(yearCol) else 0
                    val dateAdded = if (dateAddedCol != -1) cursor.getLong(dateAddedCol) else 0L
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    songList.add(
                        Song(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            albumId = albumId,
                            duration = duration,
                            contentUriString = contentUri.toString(),
                            dataPath = dataPath,
                            trackNumber = trackNumber,
                            year = year,
                            dateAdded = dateAdded,
                            size = size
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error scanning media store", e)
        }

        // If no songs found on device/emulator, generate rich demo library for instant showcase
        if (songList.isEmpty() && includeDemoFallbackIfEmpty) {
            songList.addAll(generateDemoSongs())
        }

        _rawSongs.value = songList
        _isScanning.value = false
        songList
    }

    suspend fun toggleFavorite(song: Song) {
        val key = "SONG_${song.id}"
        if (song.isFavorite) {
            favoritesDao.deleteFavorite(key)
        } else {
            favoritesDao.insertFavorite(
                FavoriteEntity(
                    key = key,
                    type = "SONG",
                    itemId = song.id.toString()
                )
            )
        }
    }

    suspend fun toggleAlbumFavorite(album: Album, isFavorite: Boolean) {
        val key = "ALBUM_${album.id}"
        if (isFavorite) {
            favoritesDao.deleteFavorite(key)
        } else {
            favoritesDao.insertFavorite(
                FavoriteEntity(
                    key = key,
                    type = "ALBUM",
                    itemId = album.id.toString()
                )
            )
        }
    }

    suspend fun toggleArtistFavorite(artist: Artist, isFavorite: Boolean) {
        val key = "ARTIST_${artist.name}"
        if (isFavorite) {
            favoritesDao.deleteFavorite(key)
        } else {
            favoritesDao.insertFavorite(
                FavoriteEntity(
                    key = key,
                    type = "ARTIST",
                    itemId = artist.name
                )
            )
        }
    }

    fun sortSongs(songs: List<Song>, sortOrder: SongSortOrder): List<Song> {
        return when (sortOrder) {
            SongSortOrder.TITLE_ASC -> songs.sortedBy { it.title.lowercase() }
            SongSortOrder.TITLE_DESC -> songs.sortedByDescending { it.title.lowercase() }
            SongSortOrder.ARTIST_ASC -> songs.sortedBy { it.artist.lowercase() }
            SongSortOrder.ALBUM_ASC -> songs.sortedBy { it.album.lowercase() }
            SongSortOrder.DURATION_DESC -> songs.sortedByDescending { it.duration }
            SongSortOrder.DURATION_ASC -> songs.sortedBy { it.duration }
            SongSortOrder.DATE_ADDED_DESC -> songs.sortedByDescending { it.dateAdded }
            SongSortOrder.PLAY_COUNT_DESC -> songs.sortedByDescending { it.playCount }
        }
    }

    fun extractAlbums(songs: List<Song>): List<Album> {
        return songs.groupBy { it.album }
            .map { (albumName, albumSongs) ->
                val firstSong = albumSongs.first()
                Album(
                    id = firstSong.albumId,
                    title = albumName,
                    artist = firstSong.artist,
                    songCount = albumSongs.size,
                    year = firstSong.year
                )
            }
            .sortedBy { it.title.lowercase() }
    }

    fun extractArtists(songs: List<Song>): List<Artist> {
        return songs.groupBy { it.artist }.entries
            .mapIndexed { index, entry ->
                val artistName = entry.key
                val artistSongs = entry.value
                val distinctAlbums = artistSongs.map { it.album }.distinct().size
                Artist(
                    id = index.toLong() + 1,
                    name = artistName,
                    songCount = artistSongs.size,
                    albumCount = distinctAlbums
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    private suspend fun generateDemoSongs(): List<Song> {
        val demoSpecs = listOf(
            Triple("Neon Horizon", "Aether Waves", "Cyber Synth Vol. 1" to "Synthwave"),
            Triple("Midnight Echoes", "Luna Eclipse", "Velvet Nights" to "Lo-Fi Beats"),
            Triple("Solar Drift", "Aether Waves", "Cyber Synth Vol. 1" to "Synthwave"),
            Triple("Cosmic Reverie", "Starlight Syndicate", "Galactic Odyssey" to "Ambient Chill"),
            Triple("Urban Rhythm", "Pulse Collective", "Metropolis Grooves" to "Electronic"),
            Triple("Ocean Breeze", "Luna Eclipse", "Velvet Nights" to "Lo-Fi Beats"),
            Triple("Astral Projection", "Starlight Syndicate", "Galactic Odyssey" to "Ambient Chill"),
            Triple("Electric Symphony", "Pulse Collective", "Metropolis Grooves" to "Electronic")
        )

        val albumIdMap = mapOf(
            "Cyber Synth Vol. 1" to 501L,
            "Velvet Nights" to 502L,
            "Galactic Odyssey" to 503L,
            "Metropolis Grooves" to 504L
        )

        val durationList = listOf(214000L, 188000L, 245000L, 195000L, 230000L, 202000L, 268000L, 179000L)
        val playCounts = listOf(18, 24, 12, 31, 9, 15, 7, 42)

        return demoSpecs.mapIndexed { idx, (title, artist, albumGenre) ->
            val (album, genre) = albumGenre
            val audioFile = com.example.wavetune.playback.DemoAudioGenerator.getDemoAudioFile(context, idx)
            val uriStr = if (audioFile.exists()) Uri.fromFile(audioFile).toString() else ""

            Song(
                id = 1001L + idx,
                title = title,
                artist = artist,
                album = album,
                albumId = albumIdMap[album] ?: 500L,
                duration = durationList.getOrElse(idx) { 200000L },
                contentUriString = uriStr,
                dataPath = audioFile.absolutePath,
                genre = genre,
                year = 2024,
                playCount = playCounts.getOrElse(idx) { 10 },
                dateAdded = System.currentTimeMillis() - 86400000L * (idx + 1)
            )
        }
    }
}
