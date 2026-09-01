package com.example.wavetune

import android.app.Application
import com.example.wavetune.data.local.SettingsManager
import com.example.wavetune.data.local.WaveTuneDatabase
import com.example.wavetune.data.repository.HistoryRepository
import com.example.wavetune.data.repository.LyricsRepository
import com.example.wavetune.data.repository.MusicRepository
import com.example.wavetune.data.repository.PlaylistRepository
import com.example.wavetune.data.repository.VaultRepository
import com.example.wavetune.playback.AudioEffectManager
import com.example.wavetune.playback.MusicPlayerEngine
import com.example.wavetune.playback.QueueManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class WaveTuneApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { WaveTuneDatabase.getDatabase(this) }
    val settingsManager by lazy { SettingsManager(this) }

    val playlistRepository by lazy { PlaylistRepository(database.playlistDao()) }
    val historyRepository by lazy { HistoryRepository(database.historyDao()) }
    val lyricsRepository by lazy { LyricsRepository(database.lyricsDao()) }
    val vaultRepository by lazy { VaultRepository(database.vaultDao()) }

    val musicRepository by lazy {
        MusicRepository(
            context = this,
            favoritesDao = database.favoritesDao(),
            vaultDao = database.vaultDao()
        )
    }

    val queueManager by lazy { QueueManager() }
    val audioEffectManager by lazy { AudioEffectManager(this) }

    val playerEngine by lazy {
        MusicPlayerEngine(
            context = this,
            scope = applicationScope,
            historyRepository = historyRepository,
            queueManager = queueManager,
            audioEffectManager = audioEffectManager
        )
    }

    override fun onCreate() {
        super.onCreate()
    }
}
