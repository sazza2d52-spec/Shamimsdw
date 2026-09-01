package com.example.wavetune.ui.screens.artists

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wavetune.ui.components.ArtistCard
import com.example.wavetune.ui.components.SongListItem
import com.example.wavetune.ui.components.WaveArtwork
import com.example.wavetune.ui.theme.CyanAccent
import com.example.wavetune.ui.viewmodel.MusicPlayerViewModel

@Composable
fun ArtistsScreen(
    viewModel: MusicPlayerViewModel,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val artists by viewModel.artists.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("artists_screen_container")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Artists (${artists.size})",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .testTag("artists_grid"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(artists, key = { it.id }) { artist ->
                ArtistCard(
                    artist = artist,
                    onClick = { onArtistClick(artist.name) }
                )
            }
        }
    }
}

@Composable
fun ArtistDetailScreen(
    artistName: String,
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allSongs by viewModel.rawSongs.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val currentSong = playbackState.currentSong

    val artistSongs = remember(allSongs, artistName) {
        allSongs.filter { it.artist.equals(artistName, ignoreCase = true) }
    }

    val albumsCount = remember(artistSongs) {
        artistSongs.map { it.album }.distinct().size
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("artist_detail_screen"),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(start = 8.dp, top = 8.dp)
                    .testTag("artist_detail_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WaveArtwork(
                    artworkUri = null,
                    title = artistName,
                    artist = artistName,
                    size = 140.dp,
                    shape = CircleShape
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = artistName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${artistSongs.size} Songs • $albumsCount Albums",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.playSongList(artistSongs, 0) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF00363D)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Play All",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00363D)
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.toggleShuffle()
                            viewModel.playSongList(artistSongs.shuffled(), 0)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle", color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        items(artistSongs, key = { it.id }) { song ->
            SongListItem(
                song = song,
                isCurrentSong = currentSong?.id == song.id,
                isPlaying = playbackState.isPlaying,
                onClick = {
                    val idx = artistSongs.indexOf(song)
                    viewModel.playSongList(artistSongs, idx)
                },
                onFavoriteToggle = { viewModel.toggleFavorite(song) },
                onPlayNext = { viewModel.addToNext(song) },
                onAddToQueue = { viewModel.addToQueueEnd(song) },
                onAddToPlaylist = {},
                onShowDetails = {}
            )
        }
    }
}
