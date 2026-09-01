package com.example.wavetune.ui.screens.albums

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import com.example.wavetune.ui.components.AlbumCard
import com.example.wavetune.ui.components.SongListItem
import com.example.wavetune.ui.components.WaveArtwork
import com.example.wavetune.ui.theme.CyanAccent
import com.example.wavetune.ui.viewmodel.MusicPlayerViewModel

@Composable
fun AlbumsScreen(
    viewModel: MusicPlayerViewModel,
    onAlbumClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val albums by viewModel.albums.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("albums_screen_container")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Albums (${albums.size})",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .testTag("albums_grid"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(albums, key = { it.id }) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onAlbumClick(album.id) }
                )
            }
        }
    }
}

@Composable
fun AlbumDetailScreen(
    albumId: Long,
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allSongs by viewModel.rawSongs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val currentSong = playbackState.currentSong

    val album = remember(albums, albumId) {
        albums.find { it.id == albumId } ?: albums.firstOrNull()
    }

    val albumSongs = remember(allSongs, album) {
        if (album != null) {
            allSongs.filter { it.album.equals(album.title, ignoreCase = true) }
        } else emptyList()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("album_detail_screen"),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(start = 8.dp, top = 8.dp)
                    .testTag("album_detail_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }

        if (album != null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    WaveArtwork(
                        artworkUri = album.albumArtUri,
                        title = album.title,
                        artist = album.artist,
                        size = 180.dp,
                        shape = RoundedCornerShape(20.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${album.artist} • ${albumSongs.size} Songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.playSongList(albumSongs, 0) },
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
                                viewModel.playSongList(albumSongs.shuffled(), 0)
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

            items(albumSongs, key = { it.id }) { song ->
                SongListItem(
                    song = song,
                    isCurrentSong = currentSong?.id == song.id,
                    isPlaying = playbackState.isPlaying,
                    onClick = {
                        val idx = albumSongs.indexOf(song)
                        viewModel.playSongList(albumSongs, idx)
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
}
