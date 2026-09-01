package com.example.wavetune.data.repository

import com.example.wavetune.data.local.FavoritesDao
import com.example.wavetune.data.local.HistoryDao
import com.example.wavetune.data.local.LyricsDao
import com.example.wavetune.data.local.PlaylistDao
import com.example.wavetune.data.local.SongPlayCount
import com.example.wavetune.data.local.VaultDao
import com.example.wavetune.data.model.FavoriteEntity
import com.example.wavetune.data.model.HistoryEntity
import com.example.wavetune.data.model.LyricsEntity
import com.example.wavetune.data.model.Playlist
import com.example.wavetune.data.model.PlaylistSongCrossRef
import com.example.wavetune.data.model.Song
import com.example.wavetune.data.model.VaultItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PlaylistRepository(private val playlistDao: PlaylistDao) {
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        val playlist = Playlist(name = name)
        playlistDao.insertPlaylist(playlist)
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        playlistDao.renamePlaylist(playlistId, newName)
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.clearPlaylistSongs(playlistId)
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        val existingSongs = playlistDao.getSongIdsForPlaylistOnce(playlistId)
        if (!existingSongs.contains(songId)) {
            val position = existingSongs.size
            playlistDao.insertSongToPlaylist(PlaylistSongCrossRef(playlistId, songId, position))
            playlistDao.updateSongCount(playlistId, position + 1)
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
        val remainingSongs = playlistDao.getSongIdsForPlaylistOnce(playlistId)
        playlistDao.updateSongCount(playlistId, remainingSongs.size)
    }

    fun getSongIdsForPlaylist(playlistId: Long): Flow<List<Long>> =
        playlistDao.getSongIdsForPlaylist(playlistId)

    suspend fun getSongIdsForPlaylistOnce(playlistId: Long): List<Long> = withContext(Dispatchers.IO) {
        playlistDao.getSongIdsForPlaylistOnce(playlistId)
    }
}

class HistoryRepository(private val historyDao: HistoryDao) {
    val recentHistory: Flow<List<HistoryEntity>> = historyDao.getRecentHistory(50)
    val totalPlays: Flow<Int> = historyDao.getTotalPlaysCount()
    val totalListeningTime: Flow<Long?> = historyDao.getTotalListeningTime()

    suspend fun recordPlay(song: Song, durationSeconds: Long) = withContext(Dispatchers.IO) {
        historyDao.insertHistory(
            HistoryEntity(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                albumId = song.albumId,
                duration = song.duration,
                listenedSeconds = durationSeconds
            )
        )
    }

    suspend fun getMostPlayedSongCounts(limit: Int = 30): List<SongPlayCount> = withContext(Dispatchers.IO) {
        historyDao.getMostPlayedSongIds(limit)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        historyDao.clearHistory()
    }
}

class LyricsRepository(private val lyricsDao: LyricsDao) {
    fun getLyricsForSong(songId: Long): Flow<LyricsEntity?> =
        lyricsDao.getLyricsForSong(songId)

    suspend fun saveCustomLyrics(song: Song, lyricsText: String) = withContext(Dispatchers.IO) {
        lyricsDao.saveLyrics(
            LyricsEntity(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                plainLyrics = lyricsText,
                isSynced = false
            )
        )
    }

    suspend fun getOrGenerateLyrics(song: Song): String = withContext(Dispatchers.IO) {
        val cached = lyricsDao.getLyricsForSongOnce(song.id)
        if (cached != null && cached.plainLyrics.isNotBlank()) {
            return@withContext cached.plainLyrics
        }

        // Generate nice structured placeholder or embedded lyrics template for the song
        val defaultLyrics = """
            [00:08.00] Floating through the neon sky
            [00:16.00] Watching every frequency pass by
            [00:24.00] ${song.title} in the night
            [00:32.00] Electric resonance, glowing bright
            
            [00:48.00] Rhythm in the heart, sound in the soul
            [00:56.00] WaveTune melody takes control
            [01:04.00] With ${song.artist} playing on repeat
            [01:12.00] Moving to the hypnotic beat
            
            [01:28.00] Let the harmony surround
            [01:36.00] Pure perfection in the sound
            [01:44.00] Feel the echo, let it rise
            [01:52.00] Underneath the starlit skies
        """.trimIndent()

        lyricsDao.saveLyrics(
            LyricsEntity(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                plainLyrics = defaultLyrics,
                isSynced = true
            )
        )
        defaultLyrics
    }
}

class VaultRepository(private val vaultDao: VaultDao) {
    val vaultSongIds: Flow<List<Long>> = vaultDao.getVaultSongIds()
    val vaultPlaylistIds: Flow<List<Long>> = vaultDao.getVaultPlaylistIds()

    suspend fun protectSong(songId: Long) = withContext(Dispatchers.IO) {
        vaultDao.addToVault(
            VaultItemEntity(
                itemKey = "SONG_$songId",
                itemType = "SONG",
                itemId = songId
            )
        )
    }

    suspend fun unprotectSong(songId: Long) = withContext(Dispatchers.IO) {
        vaultDao.removeFromVault("SONG_$songId")
    }

    suspend fun protectPlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        vaultDao.addToVault(
            VaultItemEntity(
                itemKey = "PLAYLIST_$playlistId",
                itemType = "PLAYLIST",
                itemId = playlistId
            )
        )
    }

    suspend fun unprotectPlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        vaultDao.removeFromVault("PLAYLIST_$playlistId")
    }
}
