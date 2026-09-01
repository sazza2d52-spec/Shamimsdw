package com.example.wavetune.ui.screens.nowplaying

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wavetune.data.model.Song
import com.example.wavetune.playback.PlaybackState
import com.example.wavetune.playback.RepeatMode
import com.example.wavetune.ui.components.AnimatedEqualizerVisualizer
import com.example.wavetune.ui.components.WaveArtwork
import com.example.wavetune.ui.theme.LavenderAccent
import com.example.wavetune.ui.theme.LavenderDark
import com.example.wavetune.ui.theme.LavenderLight
import com.example.wavetune.ui.theme.PinkAccent
import com.example.wavetune.ui.theme.PurpleAccent
import com.example.wavetune.ui.viewmodel.MusicPlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: MusicPlayerViewModel,
    onDismiss: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val smartRecommendations by viewModel.smartQueueRecommendations.collectAsState()
    val song = playbackState.currentSong

    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1F1F23),
                        Color(0xFF17171A),
                        Color(0xFF121316)
                    )
                )
            )
            .testTag("now_playing_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("now_playing_dismiss_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYING FROM LIBRARY",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = LavenderAccent
                    )
                    Text(
                        text = song?.album ?: "WaveTune",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Equalizer") },
                            leadingIcon = { Icon(Icons.Default.Equalizer, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onNavigateToEqualizer()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Lyrics") },
                            leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onNavigateToLyrics()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sleep Timer") },
                            leadingIcon = { Icon(Icons.Default.Nightlight, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                showSleepTimerDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Track Details") },
                            onClick = {
                                menuExpanded = false
                                showDetailsDialog = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Glowing Large Album Artwork
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                WaveArtwork(
                    artworkUri = song?.albumArtUri,
                    title = song?.title ?: "WaveTune",
                    artist = song?.artist ?: "",
                    size = 280.dp,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title, Artist, and Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song?.title ?: "No Track Selected",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song?.artist ?: "Select a track to start playing",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (song != null) {
                    IconButton(
                        onClick = { viewModel.toggleFavorite(song) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("now_playing_fav_btn")
                    ) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (song.isFavorite) PinkAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Seekbar
            Column(modifier = Modifier.fillMaxWidth()) {
                val currentProgress = if (isDraggingSlider) sliderPosition else playbackState.progress

                Slider(
                    value = currentProgress,
                    onValueChange = {
                        isDraggingSlider = true
                        sliderPosition = it
                    },
                    onValueChangeFinished = {
                        val seekTargetMs = (sliderPosition * playbackState.duration).toLong()
                        viewModel.seekTo(seekTargetMs)
                        isDraggingSlider = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = LavenderAccent,
                        activeTrackColor = LavenderAccent,
                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("now_playing_seekbar")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val displayPositionMs = if (isDraggingSlider) {
                        (sliderPosition * playbackState.duration).toLong()
                    } else {
                        playbackState.currentPosition
                    }

                    Text(
                        text = PlaybackState.formatTime(displayPositionMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = playbackState.durationFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Main Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(
                    onClick = { viewModel.toggleShuffle() },
                    modifier = Modifier.testTag("now_playing_shuffle_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.isShuffle) LavenderAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous Button
                IconButton(
                    onClick = { viewModel.playPrevious() },
                    modifier = Modifier.size(48.dp).testTag("now_playing_prev_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Big Clean Play / Pause FAB
                FloatingActionButton(
                    onClick = { viewModel.togglePlayPause() },
                    containerColor = LavenderLight,
                    contentColor = LavenderDark,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("now_playing_play_pause_fab")
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next Button
                IconButton(
                    onClick = { viewModel.playNext() },
                    modifier = Modifier.size(48.dp).testTag("now_playing_next_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Repeat Button
                IconButton(
                    onClick = { viewModel.cycleRepeatMode() },
                    modifier = Modifier.testTag("now_playing_repeat_btn")
                ) {
                    val (icon, active) = when (playbackState.repeatMode) {
                        RepeatMode.OFF -> Pair(Icons.Default.Repeat, false)
                        RepeatMode.ALL -> Pair(Icons.Default.Repeat, true)
                        RepeatMode.ONE -> Pair(Icons.Default.RepeatOne, true)
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Repeat",
                        tint = if (active) LavenderAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Bottom Auxiliary Bar (Queue, Equalizer, Lyrics, Sleep Timer)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showQueueSheet = true },
                    modifier = Modifier.testTag("now_playing_queue_sheet_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Queue",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }

                IconButton(
                    onClick = onNavigateToEqualizer,
                    modifier = Modifier.testTag("now_playing_eq_screen_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "Equalizer",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }

                IconButton(
                    onClick = onNavigateToLyrics,
                    modifier = Modifier.testTag("now_playing_lyrics_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Lyrics",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }

                IconButton(
                    onClick = { showSleepTimerDialog = true },
                    modifier = Modifier.testTag("now_playing_timer_btn")
                ) {
                    val hasTimer = playbackState.sleepTimerRemainingSeconds != null
                    Icon(
                        imageVector = Icons.Default.Nightlight,
                        contentDescription = "Sleep Timer",
                        tint = if (hasTimer) LavenderAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Queue Bottom Sheet
    if (showQueueSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Queue (${playbackState.queue.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = { showQueueSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(playbackState.queue, key = { index, song -> "q_${song.id}_$index" }) { index, qSong ->
                        val isPlayingThis = index == playbackState.queueIndex
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.playQueueIndex(index)
                                },
                            color = if (isPlayingThis) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WaveArtwork(
                                    artworkUri = qSong.albumArtUri,
                                    title = qSong.title,
                                    artist = qSong.artist,
                                    size = 40.dp,
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = qSong.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isPlayingThis) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isPlayingThis) LavenderAccent else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = qSong.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(onClick = { viewModel.removeFromQueue(index) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Smart recommendations at bottom of queue
                    if (smartRecommendations.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "✨ Smart Queue Recommendations",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = LavenderAccent
                            )
                        }

                        itemsIndexed(smartRecommendations, key = { _, s -> "rec_${s.id}" }) { _, rSong ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WaveArtwork(
                                    artworkUri = rSong.albumArtUri,
                                    title = rSong.title,
                                    artist = rSong.artist,
                                    size = 36.dp,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rSong.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = rSong.artist,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                TextButton(onClick = { viewModel.addToQueueEnd(rSong) }) {
                                    Text("+ Add", color = LavenderAccent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        val timerSec = playbackState.sleepTimerRemainingSeconds
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (timerSec != null) {
                        Text(
                            text = "Active: ${timerSec / 60}m ${timerSec % 60}s remaining",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = LavenderAccent
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    val presets = listOf(5, 10, 15, 30, 45, 60)
                    presets.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { min ->
                                Button(
                                    onClick = {
                                        viewModel.startSleepTimer(min)
                                        showSleepTimerDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("${min}m", color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (timerSec != null) {
                    TextButton(onClick = {
                        viewModel.cancelSleepTimer()
                        showSleepTimerDialog = false
                    }) {
                        Text("Turn Off Timer", color = PinkAccent)
                    }
                } else {
                    TextButton(onClick = { showSleepTimerDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Song Details Dialog
    if (showDetailsDialog && song != null) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = { Text("Track Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Title: ${song.title}", fontWeight = FontWeight.SemiBold)
                    Text("Artist: ${song.artist}")
                    Text("Album: ${song.album}")
                    Text("Genre: ${song.genre}")
                    Text("Year: ${if (song.year > 0) song.year.toString() else "Unknown"}")
                    Text("Duration: ${song.durationFormatted}")
                    Text("Play Count: ${song.playCount}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
