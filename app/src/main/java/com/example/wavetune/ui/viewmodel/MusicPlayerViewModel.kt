package com.example.wavetune.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wavetune.WaveTuneApp
import com.example.wavetune.data.model.Album
import com.example.wavetune.data.model.Artist
import com.example.wavetune.data.model.Playlist
import com.example.wavetune.data.model.Song
import com.example.wavetune.data.model.SongSortOrder
import com.example.wavetune.playback.EqPreset
import com.example.wavetune.playback.EqualizerState
import com.example.wavetune.playback.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WaveTuneApp
    private val musicRepo = app.musicRepository
    private val playlistRepo = app.playlistRepository
    private val historyRepo = app.historyRepository
    private val lyricsRepo = app.lyricsRepository
    private val vaultRepo = app.vaultRepository
    val settingsManager = app.settingsManager
    private val playerEngine = app.playerEngine
    private val audioEffectManager = app.audioEffectManager

    val playbackState: StateFlow<PlaybackState> = playerEngine.playbackState
    val eqState: StateFlow<EqualizerState> = audioEffectManager.eqState

    val isScanning: StateFlow<Boolean> = musicRepo.isScanning
    val rawSongs: StateFlow<List<Song>> = musicRepo.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sortOrder: StateFlow<SongSortOrder> = settingsManager.sortOrder

    val sortedSongs: StateFlow<List<Song>> = combine(rawSongs, sortOrder) { songs, order ->
        musicRepo.sortSongs(songs, order)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = rawSongs.combine(rawSongs) { songs, _ ->
        musicRepo.extractAlbums(songs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<Artist>> = rawSongs.combine(rawSongs) { songs, _ ->
        musicRepo.extractArtists(songs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = playlistRepo.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentHistory = historyRepo.recentHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPlays = historyRepo.totalPlays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalListeningTime = historyRepo.totalListeningTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val vaultSongIds = vaultRepo.vaultSongIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultPlaylistIds = vaultRepo.vaultPlaylistIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Smart Queue recommendations
    private val _smartQueueRecommendations = MutableStateFlow<List<Song>>(emptyList())
    val smartQueueRecommendations = _smartQueueRecommendations.asStateFlow()

    // Vault unlock status
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked = _isVaultUnlocked.asStateFlow()

    init {
        // Initial scan
        viewModelScope.launch {
            musicRepo.scanMusic(includeDemoFallbackIfEmpty = true)
        }

        // Update smart queue recommendations when song changes
        viewModelScope.launch {
            playbackState.collect { state ->
                if (state.currentSong != null) {
                    _smartQueueRecommendations.value =
                        playerEngine.queueManager.generateSmartQueueRecommendations(rawSongs.value)
                }
            }
        }
    }

    fun scanMusic() {
        viewModelScope.launch {
            musicRepo.scanMusic(includeDemoFallbackIfEmpty = true)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: SongSortOrder) {
        settingsManager.setSortOrder(order)
    }

    // Playback Controls
    fun playSong(song: Song) {
        playerEngine.playSong(song)
    }

    fun playSongList(songs: List<Song>, startIndex: Int = 0) {
        playerEngine.playSongList(songs, startIndex)
    }

    fun togglePlayPause() {
        playerEngine.togglePlayPause()
    }

    fun playNext() {
        playerEngine.playNext()
    }

    fun playPrevious() {
        playerEngine.playPrevious()
    }

    fun seekTo(positionMs: Long) {
        playerEngine.seekTo(positionMs)
    }

    fun toggleShuffle() {
        playerEngine.toggleShuffle()
    }

    fun cycleRepeatMode() {
        playerEngine.cycleRepeatMode()
    }

    fun playQueueIndex(index: Int) {
        val song = playerEngine.queueManager.playSongAt(index)
        if (song != null) {
            playerEngine.playSong(song)
        }
    }

    fun addToNext(song: Song) {
        playerEngine.queueManager.addToNext(song)
    }

    fun addToQueueEnd(song: Song) {
        playerEngine.queueManager.addToQueueEnd(song)
    }

    fun removeFromQueue(index: Int) {
        playerEngine.queueManager.removeFromQueue(index)
    }

    fun reorderQueue(from: Int, to: Int) {
        playerEngine.queueManager.reorderQueue(from, to)
    }

    // Sleep Timer
    fun startSleepTimer(minutes: Int) {
        playerEngine.startSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        playerEngine.cancelSleepTimer()
    }

    // Favorites
    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            musicRepo.toggleFavorite(song)
        }
    }

    fun toggleAlbumFavorite(album: Album, isFavorite: Boolean) {
        viewModelScope.launch {
            musicRepo.toggleAlbumFavorite(album, isFavorite)
        }
    }

    fun toggleArtistFavorite(artist: Artist, isFavorite: Boolean) {
        viewModelScope.launch {
            musicRepo.toggleArtistFavorite(artist, isFavorite)
        }
    }

    // Playlists
    fun createPlaylist(name: String, onCreated: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = playlistRepo.createPlaylist(name)
            onCreated?.invoke(id)
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            playlistRepo.renamePlaylist(playlistId, newName)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistRepo.deletePlaylist(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            playlistRepo.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            playlistRepo.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun getSongsForPlaylist(playlistId: Long, onResult: (List<Song>) -> Unit) {
        viewModelScope.launch {
            val ids = playlistRepo.getSongIdsForPlaylistOnce(playlistId).toSet()
            val songs = rawSongs.value.filter { ids.contains(it.id) }
            onResult(songs)
        }
    }

    // Equalizer Controls
    fun setEqPreset(preset: EqPreset) {
        audioEffectManager.setPreset(preset)
    }

    fun setEqBandLevel(bandId: Short, level: Short) {
        audioEffectManager.setBandLevel(bandId, level)
    }

    fun setBassStrength(strength: Int) {
        audioEffectManager.setBassStrength(strength)
    }

    fun setVirtualizerStrength(strength: Int) {
        audioEffectManager.setVirtualizerStrength(strength)
    }

    fun setTrebleStrength(strength: Int) {
        audioEffectManager.setTrebleStrength(strength)
    }

    fun setVocalStrength(strength: Int) {
        audioEffectManager.setVocalStrength(strength)
    }

    fun setBalance(balance: Float) {
        audioEffectManager.setBalance(balance)
    }

    fun toggleEqEnabled(enabled: Boolean) {
        audioEffectManager.setEnabled(enabled)
    }

    // Lyrics
    fun loadLyricsForSong(song: Song, onLoaded: (String) -> Unit) {
        viewModelScope.launch {
            val lyrics = lyricsRepo.getOrGenerateLyrics(song)
            onLoaded(lyrics)
        }
    }

    fun saveCustomLyrics(song: Song, lyricsText: String) {
        viewModelScope.launch {
            lyricsRepo.saveCustomLyrics(song, lyricsText)
        }
    }

    // Vault
    fun unlockVault(pin: String): Boolean {
        val savedPin = settingsManager.vaultPin.value
        return if (savedPin.isEmpty() || savedPin == pin) {
            _isVaultUnlocked.value = true
            true
        } else {
            false
        }
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    fun setVaultPin(pin: String) {
        settingsManager.setVaultPin(pin)
        _isVaultUnlocked.value = true
    }

    fun protectSong(songId: Long) {
        viewModelScope.launch {
            vaultRepo.protectSong(songId)
        }
    }

    fun unprotectSong(songId: Long) {
        viewModelScope.launch {
            vaultRepo.unprotectSong(songId)
        }
    }

    fun protectPlaylist(playlistId: Long) {
        viewModelScope.launch {
            vaultRepo.protectPlaylist(playlistId)
        }
    }

    fun unprotectPlaylist(playlistId: Long) {
        viewModelScope.launch {
            vaultRepo.unprotectPlaylist(playlistId)
        }
    }
}
