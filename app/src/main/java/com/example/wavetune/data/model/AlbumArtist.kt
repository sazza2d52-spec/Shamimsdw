package com.example.wavetune.data.model

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val songCount: Int,
    val year: Int = 0,
    val albumArtUri: String = "content://media/external/audio/albumart/$id"
)

data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val albumCount: Int = 1
)
