package com.example.wavetune.ui.screens.search

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wavetune.ui.components.AlbumCard
import com.example.wavetune.ui.components.ArtistCard
import com.example.wavetune.ui.components.SongListItem
import com.example.wavetune.ui.theme.CyanAccent
import com.example.wavetune.ui.viewmodel.MusicPlayerViewModel

@Composable
fun SearchScreen(
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) } // 0: All, 1: Songs, 2: Albums, 3: Artists

    val allSongs by viewModel.rawSongs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val currentSong = playbackState.currentSong

    val matchingSongs = remember(allSongs, query) {
        if (query.isBlank()) emptyList()
        else allSongs.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true) ||
            it.genre.contains(query, ignoreCase = true)
        }
    }

    val matchingAlbums = remember(albums, query) {
        if (query.isBlank()) emptyList()
        else albums.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true)
        }
    }

    val matchingArtists = remember(artists, query) {
        if (query.isBlank()) emptyList()
        else artists.filter {
            it.name.contains(query, ignoreCase = true)
        }
    }

    val categories = listOf("All", "Songs (${matchingSongs.size})", "Albums (${matchingAlbums.size})", "Artists (${matchingArtists.size})")

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("search_screen_container")
    ) {
        // Search Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("search_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search songs, artists, albums...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
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
                    .testTag("search_query_input")
            )
        }

        // Category Filter Chips
        if (query.isNotBlank()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories.size) { index ->
                    FilterChip(
                        selected = selectedCategoryIndex == index,
                        onClick = { selectedCategoryIndex = index },
                        label = { Text(categories[index]) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent.copy(alpha = 0.2f),
                            selectedLabelColor = CyanAccent
                        )
                    )
                }
            }
        }

        if (query.isBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Type a song, artist, album, or genre name",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("search_results_list"),
                contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
            ) {
                // Artists
                if ((selectedCategoryIndex == 0 || selectedCategoryIndex == 3) && matchingArtists.isNotEmpty()) {
                    item {
                        Text(
                            text = "Artists",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    items(matchingArtists, key = { "search_art_${it.id}" }) { artist ->
                        ArtistCard(
                            artist = artist,
                            onClick = { onNavigateToArtist(artist.name) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                // Albums
                if ((selectedCategoryIndex == 0 || selectedCategoryIndex == 2) && matchingAlbums.isNotEmpty()) {
                    item {
                        Text(
                            text = "Albums",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    items(matchingAlbums, key = { "search_alb_${it.id}" }) { album ->
                        AlbumCard(
                            album = album,
                            onClick = { onNavigateToAlbum(album.id) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                // Songs
                if ((selectedCategoryIndex == 0 || selectedCategoryIndex == 1) && matchingSongs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Songs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    items(matchingSongs, key = { "search_song_${it.id}" }) { song ->
                        SongListItem(
                            song = song,
                            isCurrentSong = currentSong?.id == song.id,
                            isPlaying = playbackState.isPlaying,
                            onClick = {
                                val idx = matchingSongs.indexOf(song)
                                viewModel.playSongList(matchingSongs, idx)
                            },
                            onFavoriteToggle = { viewModel.toggleFavorite(song) },
                            onPlayNext = { viewModel.addToNext(song) },
                            onAddToQueue = { viewModel.addToQueueEnd(song) },
                            onAddToPlaylist = {},
                            onShowDetails = {}
                        )
                    }
                }

                if (matchingSongs.isEmpty() && matchingAlbums.isEmpty() && matchingArtists.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No matching tracks, albums, or artists found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
