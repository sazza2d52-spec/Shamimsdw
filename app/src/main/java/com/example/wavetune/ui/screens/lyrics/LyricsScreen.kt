package com.example.wavetune.ui.screens.lyrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wavetune.ui.components.WaveArtwork
import com.example.wavetune.ui.theme.CyanAccent
import com.example.wavetune.ui.theme.PinkAccent
import com.example.wavetune.ui.viewmodel.MusicPlayerViewModel

data class LyricLine(val timeMs: Long, val text: String)

@Composable
fun LyricsScreen(
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val song = playbackState.currentSong

    var lyricsText by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var editInput by remember { mutableStateOf("") }

    LaunchedEffect(song?.id) {
        if (song != null) {
            viewModel.loadLyricsForSong(song) { loaded ->
                lyricsText = loaded
                editInput = loaded
            }
        }
    }

    val parsedLines = remember(lyricsText) {
        parseLrcLyrics(lyricsText)
    }

    val currentLineIndex = remember(parsedLines, playbackState.currentPosition) {
        if (parsedLines.isEmpty()) -1
        else {
            val idx = parsedLines.indexOfLast { it.timeMs <= playbackState.currentPosition }
            if (idx != -1) idx else 0
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0 && currentLineIndex < parsedLines.size) {
            listState.animateScrollToItem((currentLineIndex - 2).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("lyrics_screen_container")
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = "Lyrics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (song != null) {
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Lyrics",
                        tint = CyanAccent
                    )
                }
            }
        }

        if (song == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Play a song to view lyrics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Track Mini Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WaveArtwork(
                        artworkUri = song.albumArtUri,
                        title = song.title,
                        artist = song.artist,
                        size = 44.dp,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Lyrics Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("lyrics_list"),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (parsedLines.isEmpty()) {
                    item {
                        Text(
                            text = lyricsText.ifBlank { "No lyrics available. Tap the edit icon to add lyrics." },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp
                        )
                    }
                } else {
                    items(parsedLines.indices.toList()) { index ->
                        val line = parsedLines[index]
                        val isHighlighted = index == currentLineIndex

                        Text(
                            text = line.text,
                            style = if (isHighlighted) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                            fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isHighlighted) CyanAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    if (showEditDialog && song != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Song Lyrics") },
            text = {
                OutlinedTextField(
                    value = editInput,
                    onValueChange = { editInput = it },
                    placeholder = { Text("Type plain or [mm:ss] synced lyrics...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    maxLines = 15
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCustomLyrics(song, editInput)
                        lyricsText = editInput
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Save", color = Color(0xFF00363D))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun parseLrcLyrics(lrc: String): List<LyricLine> {
    if (lrc.isBlank()) return emptyList()
    val lines = lrc.lines()
    val result = mutableListOf<LyricLine>()

    val regex = Regex("""\[(\d{2}):(\d{2})\.?(\d{2,3})?\](.*)""")
    for (raw in lines) {
        val match = regex.find(raw.trim())
        if (match != null) {
            val min = match.groupValues[1].toLongOrNull() ?: 0L
            val sec = match.groupValues[2].toLongOrNull() ?: 0L
            val ms = (match.groupValues[3].takeIf { it.isNotBlank() }?.toLongOrNull() ?: 0L) * 10
            val timeMs = (min * 60 + sec) * 1000 + ms
            val text = match.groupValues[4].trim()
            if (text.isNotBlank()) {
                result.add(LyricLine(timeMs, text))
            }
        }
    }
    return result.sortedBy { it.timeMs }
}
