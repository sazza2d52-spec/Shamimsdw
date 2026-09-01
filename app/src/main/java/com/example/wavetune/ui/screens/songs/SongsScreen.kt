package com.example.wavetune.ui.screens.songs

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wavetune.data.model.Song
import com.example.wavetune.data.model.SongSortOrder
import com.example.wavetune.ui.components.SongListItem
import com.example.wavetune.ui.theme.CyanAccent
import com.example.wavetune.ui.viewmodel.MusicPlayerViewModel

@Composable
fun SongsScreen(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val sortedSongs by viewModel.sortedSongs.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val currentSong = playbackState.currentSong

    var searchQuery by remember { mutableStateOf("") }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }
    var selectedSongForDetails by remember { mutableStateOf<Song?>(null) }

    val filteredSongs = remember(sortedSongs, searchQuery) {
        if (searchQuery.isBlank()) sortedSongs
        else sortedSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true) ||
            it.album.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("songs_screen_container")
    ) {
        // Search & Filter Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search ${sortedSongs.size} tracks...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("songs_search_input")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Sort Menu Button
            Box {
                IconButton(
                    onClick = { sortMenuExpanded = true },
                    modifier = Modifier.testTag("sort_menu_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort Options",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Title (A-Z)") },
                        onClick = {
                            viewModel.setSortOrder(SongSortOrder.TITLE_ASC)
                            sortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Artist (A-Z)") },
                        onClick = {
                            viewModel.setSortOrder(SongSortOrder.ARTIST_ASC)
                            sortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Album (A-Z)") },
                        onClick = {
                            viewModel.setSortOrder(SongSortOrder.ALBUM_ASC)
                            sortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duration (Longest first)") },
                        onClick = {
                            viewModel.setSortOrder(SongSortOrder.DURATION_DESC)
                            sortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Recently Added") },
                        onClick = {
                            viewModel.setSortOrder(SongSortOrder.DATE_ADDED_DESC)
                            sortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Most Played") },
                        onClick = {
                            viewModel.setSortOrder(SongSortOrder.PLAY_COUNT_DESC)
                            sortMenuExpanded = false
                        }
                    )
                }
            }
        }

        // Action Bar (Play All / Shuffle All)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredSongs.size} Songs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.playSongList(filteredSongs, 0) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("songs_play_all_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF00363D),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Play All",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00363D)
                    )
                }

                Button(
                    onClick = {
                        viewModel.toggleShuffle()
                        viewModel.playSongList(filteredSongs.shuffled(), 0)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("songs_shuffle_all_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Shuffle",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Songs List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("songs_lazy_list"),
            contentPadding = PaddingValues(bottom = 100.dp, top = 4.dp)
        ) {
            items(filteredSongs, key = { it.id }) { song ->
                SongListItem(
                    song = song,
                    isCurrentSong = currentSong?.id == song.id,
                    isPlaying = playbackState.isPlaying,
                    onClick = {
                        val idx = filteredSongs.indexOf(song)
                        viewModel.playSongList(filteredSongs, idx)
                    },
                    onFavoriteToggle = { viewModel.toggleFavorite(song) },
                    onPlayNext = { viewModel.addToNext(song) },
                    onAddToQueue = { viewModel.addToQueueEnd(song) },
                    onAddToPlaylist = { selectedSongForPlaylist = song },
                    onShowDetails = { selectedSongForDetails = song }
                )
            }
        }
    }

    // Add to Playlist Dialog
    selectedSongForPlaylist?.let { song ->
        AlertDialog(
            onDismissRequest = { selectedSongForPlaylist = null },
            title = { Text("Add to Playlist") },
            text = {
                Column {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (playlists.isEmpty()) {
                        Text("No playlists found. Create one from the Playlists tab.")
                    } else {
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(playlists, key = { it.id }) { pl ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.addSongToPlaylist(pl.id, song.id)
                                            selectedSongForPlaylist = null
                                        },
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = pl.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSongForPlaylist = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Song Details Dialog
    selectedSongForDetails?.let { song ->
        AlertDialog(
            onDismissRequest = { selectedSongForDetails = null },
            title = { Text("Track Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("Title", song.title)
                    DetailRow("Artist", song.artist)
                    DetailRow("Album", song.album)
                    DetailRow("Genre", song.genre)
                    DetailRow("Duration", song.durationFormatted)
                    DetailRow("Play Count", "${song.playCount} times")
                    if (song.dataPath.isNotBlank()) {
                        DetailRow("Path", song.dataPath)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSongForDetails = null }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
