package com.project.kataru

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.project.kataru.ui.LibraryScreen
import com.project.kataru.ui.MainViewModel
import com.project.kataru.ui.PlayerScreen
import com.project.kataru.ui.SettingsScreen
import com.project.kataru.ui.theme.KataruTheme

import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable full screen and hide system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            KataruTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionHandler {
                        viewModel.loadAudioBooks()
                        val audioBooks by viewModel.audioBooks.collectAsState()
                        val currentBook by viewModel.currentBook.collectAsState()
                        val isPlaying by viewModel.isPlaying.collectAsState()
                        
                        var showPlayer by remember { mutableStateOf(false) }
                        var showSettings by remember { mutableStateOf(false) }

                        // Auto-show player if a book is selected (and we aren't already there)
                        LaunchedEffect(currentBook) {
                            if (currentBook != null) {
                                showPlayer = true
                            }
                        }

                        BackHandler(enabled = showPlayer || showSettings) {
                            if (showSettings) {
                                showSettings = false
                            } else {
                                showPlayer = false
                            }
                        }

                        AnimatedContent(
                            targetState = when {
                                showSettings -> "Settings"
                                showPlayer -> "Player"
                                else -> "Library"
                            },
                            label = "ScreenTransition",
                            transitionSpec = {
                                when {
                                    // Library -> Settings (Slide Left)
                                    initialState == "Library" && targetState == "Settings" -> {
                                        slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
                                    }
                                    // Settings -> Library (Slide Right)
                                    initialState == "Settings" && targetState == "Library" -> {
                                        slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
                                    }
                                    // Library -> Player (Slide Up)
                                    initialState == "Library" && targetState == "Player" -> {
                                        slideInVertically { height -> height } togetherWith slideOutVertically { height -> -height }
                                    }
                                    // Player -> Library (Slide Down)
                                    else -> {
                                        slideInVertically { height -> -height } togetherWith slideOutVertically { height -> height }
                                    }
                                }
                            }
                        ) { screen ->
                            when (screen) {
                                "Settings" -> {
                                    SettingsScreen(
                                        onBackClick = { showSettings = false },
                                        onRescanClick = {
                                            viewModel.loadAudioBooks()
                                            showSettings = false
                                        }
                                    )
                                }
                                "Player" -> {
                                    if (currentBook != null) {
                                        val currentPosition by viewModel.currentPosition.collectAsState()
                                        val duration by viewModel.duration.collectAsState()
                                        PlayerScreen(
                                            book = currentBook!!,
                                            isPlaying = isPlaying,
                                            currentPosition = currentPosition,
                                            duration = duration,
                                            onPlayPauseClick = { viewModel.togglePlayPause() },
                                            onSeek = { viewModel.seekTo(it) },
                                            onSkipForward = { viewModel.skipForward() },
                                            onSkipBackward = { viewModel.skipBackward() },
                                            onSkipNext = { viewModel.skipToNext() },
                                            onSkipPrevious = { viewModel.skipToPrevious() }
                                        )
                                    }
                                }
                                "Library" -> {
                                    val isRefreshing by viewModel.isRefreshing.collectAsState()
                                    LibraryScreen(
                                        audioBooks = audioBooks,
                                        isRefreshing = isRefreshing,
                                        onBookClick = { book ->
                                            viewModel.playAudioBook(book)
                                            showPlayer = true
                                        },
                                        onRefresh = { viewModel.loadAudioBooks() },
                                        onSettingsClick = { showSettings = true },
                                        onHistoryClick = { /* TODO: Implement History */ }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionHandler(onPermissionGranted: @Composable () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    if (hasPermission) {
        onPermissionGranted()
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                } else {
                    launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }) {
                Text("Grant Permission to Read Audio")
            }
        }
    }
}