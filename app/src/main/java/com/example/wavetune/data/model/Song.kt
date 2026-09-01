package com.example.wavetune.data.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long = 0L,
    val duration: Long, // milliseconds
    val contentUriString: String,
    val dataPath: String = "",
    val trackNumber: Int = 0,
    val year: Int = 0,
    val genre: String = "Unknown",
    val size: Long = 0L,
    val dateAdded: Long = 0L,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayed: Long = 0L,
    val isVaultProtected: Boolean = false
) {
    val durationFormatted: String
        get() {
            val totalSeconds = (duration / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    val contentUri: Uri
        get() = Uri.parse(contentUriString)

    val albumArtUri: String
        get() = "content://media/external/audio/albumart/$albumId"
}

enum class SongSortOrder {
    TITLE_ASC,
    TITLE_DESC,
    ARTIST_ASC,
    ALBUM_ASC,
    DURATION_DESC,
    DURATION_ASC,
    DATE_ADDED_DESC,
    PLAY_COUNT_DESC
}
