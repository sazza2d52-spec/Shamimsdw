package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.theme.DarkPillActive
import com.example.ui.theme.LavenderAccent
import com.example.ui.theme.WaveTuneTheme
import com.example.wavetune.playback.MusicService
import com.example.wavetune.ui.components.MiniPlayer
import com.example.wavetune.ui.screens.albums.AlbumDetailScreen
import com.example.wavetune.ui.screens.albums.AlbumsScreen
import com.example.wavetune.ui.screens.artists.ArtistDetailScreen
import com.example.wavetune.ui.screens.artists.ArtistsScreen
import com.example.wavetune.ui.screens.equalizer.EqualizerScreen
import com.example.wavetune.ui.screens.favorites.FavoritesScreen
import com.example.wavetune.ui.screens.home.HomeScreen
import com.example.wavetune.ui.screens.lyrics.LyricsScreen
import com.example.wavetune.ui.screens.nowplaying.NowPlayingScreen
import com.example.wavetune.ui.screens.onboarding.OnboardingScreen
import com.example.wavetune.ui.screens.playlists.PlaylistDetailScreen
import com.example.wavetune.ui.screens.playlists.PlaylistsScreen
import com.example.wavetune.ui.screens.search.SearchScreen
import com.example.wavetune.ui.screens.settings.SettingsScreen
import com.example.wavetune.ui.screens.songs.SongsScreen
import com.example.wavetune.ui.screens.stats.StatisticsScreen
import com.example.wavetune.ui.screens.vault.PrivateVaultScreen
import com.example.wavetune.ui.theme.CyanAccent
import com.example.wavetune.ui.viewmodel.MusicPlayerViewModel

sealed class Screen(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Songs : Screen("songs", "Songs", Icons.Filled.MusicNote, Icons.Outlined.MusicNote)
    object Albums : Screen("albums", "Albums", Icons.Filled.Album, Icons.Outlined.Album)
    object Artists : Screen("artists", "Artists", Icons.Filled.Person, Icons.Outlined.Person)
    object Playlists : Screen("playlists", "Playlists", Icons.Filled.QueueMusic, Icons.Outlined.QueueMusic)

    object Search : Screen("search", "Search", Icons.Filled.Home, Icons.Outlined.Home)
    object Favorites : Screen("favorites", "Favorites", Icons.Filled.Home, Icons.Outlined.Home)
    object Equalizer : Screen("equalizer", "Equalizer", Icons.Filled.Home, Icons.Outlined.Home)
    object Lyrics : Screen("lyrics", "Lyrics", Icons.Filled.Home, Icons.Outlined.Home)
    object Stats : Screen("stats", "Stats", Icons.Filled.Home, Icons.Outlined.Home)
    object Vault : Screen("vault", "Vault", Icons.Filled.Home, Icons.Outlined.Home)
    object Settings : Screen("settings", "Settings", Icons.Filled.Home, Icons.Outlined.Home)
    object Onboarding : Screen("onboarding", "Onboarding", Icons.Filled.Home, Icons.Outlined.Home)
    object AlbumDetail : Screen("album_detail/{albumId}", "Album", Icons.Filled.Album, Icons.Outlined.Album) {
        fun createRoute(albumId: Long) = "album_detail/$albumId"
    }
    object ArtistDetail : Screen("artist_detail/{artistName}", "Artist", Icons.Filled.Person, Icons.Outlined.Person) {
        fun createRoute(artistName: String) = "artist_detail/$artistName"
    }
    object PlaylistDetail : Screen("playlist_detail/{playlistId}", "Playlist", Icons.Filled.QueueMusic, Icons.Outlined.QueueMusic) {
        fun createRoute(playlistId: Long) = "playlist_detail/$playlistId"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            MusicService.start(this)
        } catch (e: Exception) {
            // Foreground service start gracefully handled
        }

        setContent {
            val viewModel: MusicPlayerViewModel = viewModel()
            val isDarkTheme by viewModel.settingsManager.isDarkMode.collectAsState()
            val isOnboardingCompleted by viewModel.settingsManager.isOnboardingCompleted.collectAsState()

            WaveTuneTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WaveTuneMainContent(
                        viewModel = viewModel,
                        isOnboardingCompleted = isOnboardingCompleted
                    )
                }
            }
        }
    }
}

@Composable
fun WaveTuneMainContent(
    viewModel: MusicPlayerViewModel,
    isOnboardingCompleted: Boolean
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val playbackState by viewModel.playbackState.collectAsState()
    var isNowPlayingExpanded by remember { mutableStateOf(false) }

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Songs,
        Screen.Albums,
        Screen.Artists,
        Screen.Playlists
    )

    val isBottomBarVisible = currentRoute in bottomNavItems.map { it.route } && !isNowPlayingExpanded

    BackHandler(enabled = isNowPlayingExpanded) {
        isNowPlayingExpanded = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (isBottomBarVisible) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        // Floating Mini Player
                        MiniPlayer(
                            playbackState = playbackState,
                            onMiniPlayerClick = { isNowPlayingExpanded = true },
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onNextClick = { viewModel.playNext() }
                        )

                        // Bottom Navigation Bar
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            bottomNavItems.forEach { screen ->
                                val selected = currentRoute == screen.route
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                            contentDescription = screen.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = screen.title,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    selected = selected,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = LavenderAccent,
                                        selectedTextColor = LavenderAccent,
                                        indicatorColor = DarkPillActive,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    onClick = {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("nav_item_${screen.route}")
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            val startDestination = if (isOnboardingCompleted) Screen.Home.route else Screen.Onboarding.route

            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        onFinish = {
                            viewModel.settingsManager.setOnboardingCompleted(true)
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                        onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                        onNavigateToPlaylists = { navController.navigate(Screen.Playlists.route) },
                        onNavigateToEqualizer = { navController.navigate(Screen.Equalizer.route) },
                        onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                        onNavigateToVault = { navController.navigate(Screen.Vault.route) },
                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                        onNavigateToAlbum = { albumId ->
                            navController.navigate(Screen.AlbumDetail.createRoute(albumId))
                        }
                    )
                }

                composable(Screen.Songs.route) {
                    SongsScreen(viewModel = viewModel)
                }

                composable(Screen.Albums.route) {
                    AlbumsScreen(
                        viewModel = viewModel,
                        onAlbumClick = { albumId ->
                            navController.navigate(Screen.AlbumDetail.createRoute(albumId))
                        }
                    )
                }

                composable(Screen.Artists.route) {
                    ArtistsScreen(
                        viewModel = viewModel,
                        onArtistClick = { artistName ->
                            navController.navigate(Screen.ArtistDetail.createRoute(artistName))
                        }
                    )
                }

                composable(Screen.Playlists.route) {
                    PlaylistsScreen(
                        viewModel = viewModel,
                        onPlaylistClick = { playlistId ->
                            navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                        }
                    )
                }

                composable(Screen.Search.route) {
                    SearchScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToAlbum = { albumId ->
                            navController.navigate(Screen.AlbumDetail.createRoute(albumId))
                        },
                        onNavigateToArtist = { artistName ->
                            navController.navigate(Screen.ArtistDetail.createRoute(artistName))
                        }
                    )
                }

                composable(Screen.Favorites.route) {
                    FavoritesScreen(viewModel = viewModel)
                }

                composable(Screen.Equalizer.route) {
                    EqualizerScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Lyrics.route) {
                    LyricsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Stats.route) {
                    StatisticsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Vault.route) {
                    PrivateVaultScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.AlbumDetail.route,
                    arguments = listOf(navArgument("albumId") { type = NavType.LongType })
                ) { backStack ->
                    val albumId = backStack.arguments?.getLong("albumId") ?: 0L
                    AlbumDetailScreen(
                        albumId = albumId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.ArtistDetail.route,
                    arguments = listOf(navArgument("artistName") { type = NavType.StringType })
                ) { backStack ->
                    val artistName = backStack.arguments?.getString("artistName") ?: ""
                    ArtistDetailScreen(
                        artistName = artistName,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.PlaylistDetail.route,
                    arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                ) { backStack ->
                    val playlistId = backStack.arguments?.getLong("playlistId") ?: 0L
                    PlaylistDetailScreen(
                        playlistId = playlistId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        // Full Screen Now Playing overlay modal
        AnimatedVisibility(
            visible = isNowPlayingExpanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            NowPlayingScreen(
                viewModel = viewModel,
                onDismiss = { isNowPlayingExpanded = false },
                onNavigateToEqualizer = {
                    isNowPlayingExpanded = false
                    navController.navigate(Screen.Equalizer.route)
                },
                onNavigateToLyrics = {
                    isNowPlayingExpanded = false
                    navController.navigate(Screen.Lyrics.route)
                }
            )
        }
    }
}
