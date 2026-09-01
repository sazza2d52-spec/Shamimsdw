package com.example.wavetune.playback

import com.example.wavetune.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections

class QueueManager {
    private val _originalQueue = mutableListOf<Song>()
    private val _currentQueue = MutableStateFlow<List<Song>>(emptyList())
    val currentQueue = _currentQueue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex = _currentIndex.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode = _repeatMode.asStateFlow()

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        _originalQueue.clear()
        _originalQueue.addAll(songs)

        if (_isShuffle.value) {
            val selectedSong = songs.getOrNull(startIndex)
            val shuffled = songs.toMutableList()
            shuffled.shuffle()
            if (selectedSong != null) {
                shuffled.remove(selectedSong)
                shuffled.add(0, selectedSong)
            }
            _currentQueue.value = shuffled
            _currentIndex.value = if (shuffled.isNotEmpty()) 0 else -1
        } else {
            _currentQueue.value = songs
            _currentIndex.value = startIndex.coerceIn(-1, songs.size - 1)
        }
    }

    fun getCurrentSong(): Song? {
        val index = _currentIndex.value
        val list = _currentQueue.value
        return if (index in list.indices) list[index] else null
    }

    fun getNextSong(): Song? {
        val list = _currentQueue.value
        if (list.isEmpty()) return null

        val currIndex = _currentIndex.value
        when (_repeatMode.value) {
            RepeatMode.ONE -> return getCurrentSong()
            RepeatMode.ALL -> {
                val nextIndex = (currIndex + 1) % list.size
                _currentIndex.value = nextIndex
                return list[nextIndex]
            }
            RepeatMode.OFF -> {
                if (currIndex + 1 < list.size) {
                    val nextIndex = currIndex + 1
                    _currentIndex.value = nextIndex
                    return list[nextIndex]
                }
                return null
            }
        }
    }

    fun getPreviousSong(positionMs: Long): Song? {
        val list = _currentQueue.value
        if (list.isEmpty()) return null

        val currIndex = _currentIndex.value
        // If played more than 3 seconds, replay the current track
        if (positionMs > 3000L && currIndex in list.indices) {
            return list[currIndex]
        }

        if (currIndex > 0) {
            val prevIndex = currIndex - 1
            _currentIndex.value = prevIndex
            return list[prevIndex]
        } else if (_repeatMode.value == RepeatMode.ALL) {
            val prevIndex = list.size - 1
            _currentIndex.value = prevIndex
            return list[prevIndex]
        } else {
            return list.getOrNull(0)
        }
    }

    fun toggleShuffle() {
        val currentSong = getCurrentSong()
        val newShuffle = !_isShuffle.value
        _isShuffle.value = newShuffle

        if (newShuffle) {
            val shuffled = _originalQueue.toMutableList()
            shuffled.shuffle()
            if (currentSong != null) {
                shuffled.remove(currentSong)
                shuffled.add(0, currentSong)
            }
            _currentQueue.value = shuffled
            _currentIndex.value = if (shuffled.isNotEmpty()) 0 else -1
        } else {
            _currentQueue.value = _originalQueue.toList()
            val newIdx = _originalQueue.indexOfFirst { it.id == currentSong?.id }
            _currentIndex.value = if (newIdx != -1) newIdx else 0
        }
    }

    fun cycleRepeatMode(): RepeatMode {
        val nextMode = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _repeatMode.value = nextMode
        return nextMode
    }

    fun playSongAt(index: Int): Song? {
        val list = _currentQueue.value
        if (index in list.indices) {
            _currentIndex.value = index
            return list[index]
        }
        return null
    }

    fun addToNext(song: Song) {
        val list = _currentQueue.value.toMutableList()
        val currIndex = _currentIndex.value
        val insertIndex = (currIndex + 1).coerceIn(0, list.size)
        list.add(insertIndex, song)
        _currentQueue.value = list
        _originalQueue.add(song)
    }

    fun addToQueueEnd(song: Song) {
        val list = _currentQueue.value.toMutableList()
        list.add(song)
        _currentQueue.value = list
        _originalQueue.add(song)
    }

    fun removeFromQueue(index: Int) {
        val list = _currentQueue.value.toMutableList()
        if (index in list.indices) {
            val removedSong = list.removeAt(index)
            _originalQueue.remove(removedSong)
            if (index < _currentIndex.value) {
                _currentIndex.value = _currentIndex.value - 1
            } else if (index == _currentIndex.value && _currentIndex.value >= list.size) {
                _currentIndex.value = list.size - 1
            }
            _currentQueue.value = list
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val list = _currentQueue.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            Collections.swap(list, fromIndex, toIndex)
            val currentSong = getCurrentSong()
            _currentQueue.value = list
            if (currentSong != null) {
                _currentIndex.value = list.indexOfFirst { it.id == currentSong.id }
            }
        }
    }

    fun generateSmartQueueRecommendations(allLibrarySongs: List<Song>): List<Song> {
        val current = getCurrentSong() ?: return emptyList()
        val queuedIds = _currentQueue.value.map { it.id }.toSet()

        // Score library songs by matching genre, artist, and favorite status
        return allLibrarySongs
            .filter { !queuedIds.contains(it.id) }
            .sortedByDescending { song ->
                var score = 0
                if (song.artist.equals(current.artist, ignoreCase = true)) score += 50
                if (song.genre.isNotBlank() && song.genre.equals(current.genre, ignoreCase = true)) score += 30
                if (song.album.equals(current.album, ignoreCase = true)) score += 20
                if (song.isFavorite) score += 15
                score += (song.playCount.coerceAtMost(20))
                score
            }
            .take(10)
    }
}
