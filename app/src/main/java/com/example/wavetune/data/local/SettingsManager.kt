package com.example.wavetune.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.wavetune.data.model.SongSortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("wavetune_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "DARK") ?: "DARK")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _dynamicTheme = MutableStateFlow(prefs.getBoolean("dynamic_theme", true))
    val dynamicTheme: StateFlow<Boolean> = _dynamicTheme.asStateFlow()

    private val _gaplessPlayback = MutableStateFlow(prefs.getBoolean("gapless_playback", true))
    val gaplessPlayback: StateFlow<Boolean> = _gaplessPlayback.asStateFlow()
    val isGaplessPlayback: StateFlow<Boolean> = _gaplessPlayback.asStateFlow()

    private val _resumeLastSong = MutableStateFlow(prefs.getBoolean("resume_last_song", true))
    val resumeLastSong: StateFlow<Boolean> = _resumeLastSong.asStateFlow()

    private val _autoPlay = MutableStateFlow(prefs.getBoolean("auto_play", true))
    val autoPlay: StateFlow<Boolean> = _autoPlay.asStateFlow()
    val isAutoPlayOnHeadset: StateFlow<Boolean> = _autoPlay.asStateFlow()

    private val _shakeToSkip = MutableStateFlow(prefs.getBoolean("shake_to_skip", false))
    val isShakeToSkip: StateFlow<Boolean> = _shakeToSkip.asStateFlow()

    private val _aiRecommendationEnabled = MutableStateFlow(prefs.getBoolean("ai_recommendation_enabled", true))
    val aiRecommendationEnabled: StateFlow<Boolean> = _aiRecommendationEnabled.asStateFlow()
    val isAiRecommendationsEnabled: StateFlow<Boolean> = _aiRecommendationEnabled.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(prefs.getString("gemini_api_key", "") ?: "")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _cloudBackupEnabled = MutableStateFlow(prefs.getBoolean("cloud_backup_enabled", false))
    val cloudBackupEnabled: StateFlow<Boolean> = _cloudBackupEnabled.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_completed", false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _vaultPin = MutableStateFlow(prefs.getString("vault_pin", "") ?: "")
    val vaultPin: StateFlow<String> = _vaultPin.asStateFlow()

    private val _sortOrder = MutableStateFlow(SongSortOrder.valueOf(prefs.getString("sort_order", SongSortOrder.TITLE_ASC.name) ?: SongSortOrder.TITLE_ASC.name))
    val sortOrder: StateFlow<SongSortOrder> = _sortOrder.asStateFlow()

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
        _isDarkMode.value = (mode == "DARK")
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("is_dark_mode", enabled).apply()
        _isDarkMode.value = enabled
        _themeMode.value = if (enabled) "DARK" else "LIGHT"
    }

    fun setDynamicTheme(enabled: Boolean) {
        prefs.edit().putBoolean("dynamic_theme", enabled).apply()
        _dynamicTheme.value = enabled
    }

    fun setGaplessPlayback(enabled: Boolean) {
        prefs.edit().putBoolean("gapless_playback", enabled).apply()
        _gaplessPlayback.value = enabled
    }

    fun setResumeLastSong(enabled: Boolean) {
        prefs.edit().putBoolean("resume_last_song", enabled).apply()
        _resumeLastSong.value = enabled
    }

    fun setAutoPlay(enabled: Boolean) {
        prefs.edit().putBoolean("auto_play", enabled).apply()
        _autoPlay.value = enabled
    }

    fun setAutoPlayOnHeadset(enabled: Boolean) {
        setAutoPlay(enabled)
    }

    fun setShakeToSkip(enabled: Boolean) {
        prefs.edit().putBoolean("shake_to_skip", enabled).apply()
        _shakeToSkip.value = enabled
    }

    fun setAiRecommendationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("ai_recommendation_enabled", enabled).apply()
        _aiRecommendationEnabled.value = enabled
    }

    fun setAiRecommendationsEnabled(enabled: Boolean) {
        setAiRecommendationEnabled(enabled)
    }

    fun setGeminiApiKey(key: String) {
        prefs.edit().putString("gemini_api_key", key).apply()
        _geminiApiKey.value = key
    }

    fun setCloudBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("cloud_backup_enabled", enabled).apply()
        _cloudBackupEnabled.value = enabled
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
        _isOnboardingCompleted.value = completed
    }

    fun setVaultPin(pin: String) {
        prefs.edit().putString("vault_pin", pin).apply()
        _vaultPin.value = pin
    }

    fun setSortOrder(order: SongSortOrder) {
        prefs.edit().putString("sort_order", order.name).apply()
        _sortOrder.value = order
    }

    fun saveLastPlaybackState(songId: Long, position: Long) {
        prefs.edit()
            .putLong("last_song_id", songId)
            .putLong("last_position", position)
            .apply()
    }

    fun getLastSongId(): Long = prefs.getLong("last_song_id", -1L)
    fun getLastPosition(): Long = prefs.getLong("last_position", 0L)
}
