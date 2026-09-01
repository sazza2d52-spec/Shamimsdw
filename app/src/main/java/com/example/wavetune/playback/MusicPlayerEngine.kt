package com.example.wavetune.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.wavetune.data.model.Song
import com.example.wavetune.data.repository.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class MusicPlayerEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    private val historyRepository: HistoryRepository,
    val queueManager: QueueManager,
    val audioEffectManager: AudioEffectManager
) {
    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared: Boolean = false
    private var isPreparing: Boolean = false
    private var isSimulated: Boolean = false

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState = _playbackState.asStateFlow()

    private var progressJob: Job? = null
    private var playStartTime: Long = 0L

    val sleepTimerManager = SleepTimerManager(scope)

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }

    private var isNoisyReceiverRegistered = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (isPrepared) {
                    try {
                        mediaPlayer?.setVolume(0.2f, 0.2f)
                    } catch (e: Exception) {
                        Log.w("MusicPlayerEngine", "Error ducking audio volume: ${e.message}")
                    }
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (isPrepared) {
                    try {
                        mediaPlayer?.setVolume(1.0f, 1.0f)
                    } catch (e: Exception) {
                        Log.w("MusicPlayerEngine", "Error restoring audio volume: ${e.message}")
                    }
                }
            }
        }
    }

    init {
        scope.launch {
            queueManager.currentQueue.collect { q ->
                _playbackState.value = _playbackState.value.copy(queue = q)
            }
        }
        scope.launch {
            queueManager.currentIndex.collect { idx ->
                _playbackState.value = _playbackState.value.copy(queueIndex = idx)
            }
        }
        scope.launch {
            queueManager.isShuffle.collect { sh ->
                _playbackState.value = _playbackState.value.copy(isShuffle = sh)
            }
        }
        scope.launch {
            queueManager.repeatMode.collect { rep ->
                _playbackState.value = _playbackState.value.copy(repeatMode = rep)
            }
        }
        scope.launch {
            sleepTimerManager.remainingSeconds.collect { sec ->
                _playbackState.value = _playbackState.value.copy(sleepTimerRemainingSeconds = sec)
            }
        }
    }

    fun playSongList(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        queueManager.setQueue(songs, startIndex)
        val songToPlay = queueManager.getCurrentSong()
        if (songToPlay != null) {
            playSong(songToPlay)
        }
    }

    fun playSong(song: Song) {
        stopProgressTracker()
        recordCurrentPlayHistory()

        // Cleanly reset any existing player instance to prevent invalid state calls
        cleanupMediaPlayer()

        if (!requestAudioFocus()) {
            Log.w("MusicPlayerEngine", "Failed to obtain audio focus")
            return
        }

        registerNoisyReceiver()

        _playbackState.value = _playbackState.value.copy(
            currentSong = song,
            isBuffering = true,
            currentPosition = 0L,
            duration = song.duration
        )

        var dataSourceSet = false
        val player = MediaPlayer()

        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )

            // Resolve target audio source safely
            val directFile = if (song.dataPath.isNotBlank()) File(song.dataPath) else null
            if (directFile != null && directFile.exists() && directFile.length() > 0) {
                player.setDataSource(directFile.absolutePath)
                dataSourceSet = true
            } else if (song.contentUriString.startsWith("file://")) {
                val file = File(Uri.parse(song.contentUriString).path ?: "")
                if (file.exists() && file.length() > 0) {
                    player.setDataSource(file.absolutePath)
                    dataSourceSet = true
                }
            } else if (song.contentUriString.startsWith("content://")) {
                val uri = Uri.parse(song.contentUriString)
                try {
                    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                        player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        dataSourceSet = true
                    }
                } catch (e: Exception) {
                    Log.w("MusicPlayerEngine", "Could not open content URI: ${e.message}")
                }
            }

            if (!dataSourceSet) {
                // Try fallback via context & URI directly
                val fallbackUri = if (song.contentUriString.isNotBlank()) Uri.parse(song.contentUriString) else song.contentUri
                try {
                    player.setDataSource(context, fallbackUri)
                    dataSourceSet = true
                } catch (e: Exception) {
                    Log.w("MusicPlayerEngine", "Direct URI load failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w("MusicPlayerEngine", "Error setting data source on MediaPlayer: ${e.message}")
            dataSourceSet = false
        }

        if (!dataSourceSet) {
            // If data source could not be set, safely release player and switch to simulated playback
            try {
                player.reset()
                player.release()
            } catch (ignored: Exception) {}
            mediaPlayer = null
            isPrepared = false
            isPreparing = false
            simulatePlayback(song)
            return
        }

        try {
            isPreparing = true
            isPrepared = false
            isSimulated = false

            player.setOnPreparedListener { mp ->
                isPreparing = false
                isPrepared = true
                val actualDuration = mp.duration.toLong().takeIf { it > 0 } ?: song.duration
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = true,
                    isBuffering = false,
                    duration = actualDuration,
                    audioSessionId = mp.audioSessionId
                )
                audioEffectManager.bindAudioSession(mp.audioSessionId)
                try {
                    mp.start()
                } catch (e: Exception) {
                    Log.w("MusicPlayerEngine", "Error starting MediaPlayer in onPrepared: ${e.message}")
                }
                playStartTime = System.currentTimeMillis()
                startProgressTracker()
            }

            player.setOnCompletionListener {
                onSongCompleted()
            }

            player.setOnErrorListener { mp, what, extra ->
                Log.e("MusicPlayerEngine", "MediaPlayer error: what=$what extra=$extra")
                isPrepared = false
                isPreparing = false
                _playbackState.value = _playbackState.value.copy(
                    isBuffering = false,
                    isPlaying = false
                )
                try {
                    mp.reset()
                    mp.release()
                } catch (ignored: Exception) {}
                mediaPlayer = null
                true
            }

            mediaPlayer = player
            player.prepareAsync()
        } catch (e: Exception) {
            Log.e("MusicPlayerEngine", "Error preparing MediaPlayer: ${e.message}")
            cleanupMediaPlayer()
            simulatePlayback(song)
        }
    }

    private fun simulatePlayback(song: Song) {
        isSimulated = true
        isPrepared = false
        isPreparing = false
        _playbackState.value = _playbackState.value.copy(
            currentSong = song,
            isPlaying = true,
            isBuffering = false,
            duration = song.duration,
            currentPosition = 0L,
            audioSessionId = 1
        )
        playStartTime = System.currentTimeMillis()
        startProgressTracker()
    }

    fun play() {
        val curr = _playbackState.value.currentSong
        if (curr == null) {
            val next = queueManager.getCurrentSong() ?: queueManager.getNextSong()
            if (next != null) playSong(next)
            return
        }

        if (requestAudioFocus()) {
            if (isPrepared && mediaPlayer != null) {
                try {
                    mediaPlayer?.start()
                } catch (e: Exception) {
                    Log.w("MusicPlayerEngine", "Error resuming MediaPlayer: ${e.message}")
                }
            }
            _playbackState.value = _playbackState.value.copy(isPlaying = true)
            registerNoisyReceiver()
            startProgressTracker()
        }
    }

    fun pause() {
        if (isPrepared && mediaPlayer != null) {
            try {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                }
            } catch (e: Exception) {
                Log.w("MusicPlayerEngine", "Error pausing MediaPlayer: ${e.message}")
            }
        }
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
        stopProgressTracker()
        recordCurrentPlayHistory()
    }

    fun togglePlayPause() {
        if (_playbackState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun playNext() {
        val nextSong = queueManager.getNextSong()
        if (nextSong != null) {
            playSong(nextSong)
        } else {
            pause()
            seekTo(0L)
        }
    }

    fun playPrevious() {
        val prevSong = queueManager.getPreviousSong(_playbackState.value.currentPosition)
        if (prevSong != null) {
            playSong(prevSong)
        } else {
            seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _playbackState.value.duration.coerceAtLeast(1000L))
        if (isPrepared && mediaPlayer != null) {
            try {
                mediaPlayer?.seekTo(clamped.toInt())
            } catch (e: Exception) {
                Log.w("MusicPlayerEngine", "Error seeking MediaPlayer: ${e.message}")
            }
        }
        _playbackState.value = _playbackState.value.copy(currentPosition = clamped)
    }

    fun toggleShuffle() {
        queueManager.toggleShuffle()
    }

    fun cycleRepeatMode() {
        queueManager.cycleRepeatMode()
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerManager.startTimer(minutes) {
            pause()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerManager.cancelTimer()
    }

    private fun onSongCompleted() {
        recordCurrentPlayHistory()
        val repeatMode = _playbackState.value.repeatMode
        if (repeatMode == RepeatMode.ONE) {
            seekTo(0L)
            play()
        } else {
            playNext()
        }
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch(Dispatchers.Default) {
            while (isActive && _playbackState.value.isPlaying) {
                delay(500L)
                val pos = if (isPrepared && mediaPlayer != null) {
                    try {
                        mediaPlayer?.currentPosition?.toLong() ?: (_playbackState.value.currentPosition + 500L)
                    } catch (e: Exception) {
                        _playbackState.value.currentPosition + 500L
                    }
                } else {
                    _playbackState.value.currentPosition + 500L
                }

                val dur = _playbackState.value.duration
                if (dur > 0 && pos >= dur) {
                    launch(Dispatchers.Main) { onSongCompleted() }
                    break
                } else {
                    _playbackState.value = _playbackState.value.copy(currentPosition = pos)
                }
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun recordCurrentPlayHistory() {
        val current = _playbackState.value.currentSong ?: return
        val pos = _playbackState.value.currentPosition
        if (pos > 5000L) { // Only record if listened for > 5 seconds
            scope.launch {
                historyRepository.recordPlay(current, pos / 1000L)
            }
        }
    }

    private fun cleanupMediaPlayer() {
        isPrepared = false
        isPreparing = false
        try {
            mediaPlayer?.apply {
                try { stop() } catch (ignored: Exception) {}
                try { reset() } catch (ignored: Exception) {}
                try { release() } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            Log.w("MusicPlayerEngine", "Error releasing MediaPlayer: ${e.message}")
        }
        mediaPlayer = null
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = req
            audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun registerNoisyReceiver() {
        if (!isNoisyReceiverRegistered) {
            try {
                context.registerReceiver(
                    noisyReceiver,
                    IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                )
                isNoisyReceiverRegistered = true
            } catch (e: Exception) {
                Log.w("MusicPlayerEngine", "Failed to register noisy receiver", e)
            }
        }
    }

    private fun unregisterNoisyReceiver() {
        if (isNoisyReceiverRegistered) {
            try {
                context.unregisterReceiver(noisyReceiver)
                isNoisyReceiverRegistered = false
            } catch (e: Exception) {
                Log.w("MusicPlayerEngine", "Failed to unregister noisy receiver", e)
            }
        }
    }

    fun release() {
        stopProgressTracker()
        recordCurrentPlayHistory()
        unregisterNoisyReceiver()
        abandonAudioFocus()
        audioEffectManager.releaseEffects()
        cleanupMediaPlayer()
    }
}
