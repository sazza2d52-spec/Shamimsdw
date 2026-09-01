package com.example.wavetune.playback

import com.example.wavetune.data.model.Song

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val isBuffering: Boolean = false,
    val audioSessionId: Int = 0,
    val sleepTimerRemainingSeconds: Long? = null
) {
    val progress: Float
        get() = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    val positionFormatted: String
        get() = formatTime(currentPosition)

    val durationFormatted: String
        get() = formatTime(duration)

    companion object {
        fun formatTime(ms: Long): String {
            val totalSeconds = (ms / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
    }
}
