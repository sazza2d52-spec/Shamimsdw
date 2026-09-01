package com.example.wavetune.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val key: String, // e.g. "SONG_123", "ALBUM_456", "ARTIST_Queen"
    val type: String, // "SONG", "ALBUM", "ARTIST"
    val itemId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val songId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val playedAt: Long = System.currentTimeMillis(),
    val listenedSeconds: Long = 0L
)

@Entity(tableName = "lyrics_cache")
data class LyricsEntity(
    @PrimaryKey
    val songId: Long,
    val title: String,
    val artist: String,
    val plainLyrics: String,
    val isSynced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey
    val itemKey: String, // "SONG_123" or "PLAYLIST_456"
    val itemType: String, // "SONG" or "PLAYLIST"
    val itemId: Long,
    val addedAt: Long = System.currentTimeMillis()
)
