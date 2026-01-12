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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.project.kataru.ui.LibraryScreen
import com.project.kataru.ui.MainViewModel
import com.project.kataru.ui.PlayerScreen
import com.project.kataru.ui.SettingsScreen
import com.project.kataru.ui.theme.KataruTheme
import com.project.kataru.ui.theme.*

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
                    color = Color.Transparent
                ) {
                    // Premium animated gradient background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        PurpleGradientStart,
                                        PurpleGradientMid,
                                        PurpleGradientEnd
                                    )
                                )
                            )
                    ) {
                        PermissionHandler {
                            viewModel.loadAudioBooks()
                            val audioBooks by viewModel.audioBooks.collectAsState()
                            val currentBook by viewModel.currentBook.collectAsState()
                            val isPlaying by viewModel.isPlaying.collectAsState()
                            
                            var showPlayer by remember { mutableStateOf(false) }
                            var showSettings by remember { mutableStateOf(false) }
                            var showHistory by remember { mutableStateOf(false) }

                            // Auto-show player if a book is selected
                            LaunchedEffect(currentBook) {
                                if (currentBook != null) {
                                    showPlayer = true
                                }
                            }

                            BackHandler(enabled = showPlayer || showSettings || showHistory) {
                                if (showSettings) {
                                    showSettings = false
                                } else if (showHistory) {
                                    showHistory = false
                                } else {
                                    showPlayer = false
                                }
                            }

                            AnimatedContent(
                                targetState = when {
                                    showSettings -> "Settings"
                                    showHistory -> "History"
                                    showPlayer -> "Player"
                                    else -> "Library"
                                },
                                label = "ScreenTransition",
                                transitionSpec = {
                                    when {
                                        // Library -> Settings (Smooth slide + fade)
                                        initialState == "Library" && targetState == "Settings" -> {
                                            (slideInHorizontally(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            ) { width -> width } + fadeIn(
                                                animationSpec = tween(300)
                                            )) togetherWith (slideOutHorizontally(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            ) { width -> -width / 3 } + fadeOut(
                                                animationSpec = tween(200)
                                            ))
                                        }
                                        // Settings -> Library (Smooth slide back)
                                        initialState == "Settings" && targetState == "Library" -> {
                                            (slideInHorizontally(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            ) { width -> -width / 3 } + fadeIn(
                                                animationSpec = tween(300)
                                            )) togetherWith (slideOutHorizontally(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            ) { width -> width } + fadeOut(
                                                animationSpec = tween(200)
                                            ))
                                        }
                                        // Library -> History (Smooth slide + fade)
                                        initialState == "Library" && targetState == "History" -> {
                                            (slideInHorizontally(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            ) { width -> width } + fadeIn(
                                                animationSpec = tween(300)
                                            )) togetherWith (slideOutHorizontally(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            ) { width -> -width / 3 } + fadeOut(
                                                animationSpec = tween(200)
                                            ))
                                        }
                                        // History -> Library (Smooth slide back)
                                        initialState == "History" && targetState == "Library" -> {
                                            (slideInHorizontally(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            ) { width -> -width / 3 } + fadeIn(
                                                animationSpec = tween(300)
                                            )) togetherWith (slideOutHorizontally(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            ) { width -> width } + fadeOut(
                                                animationSpec = tween(200)
                                            ))
                                        }
                                        // Library -> Player (Smooth slide up with scale)
                                        initialState == "Library" && targetState == "Player" -> {
                                            (slideInVertically(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            ) { height -> height } + fadeIn(
                                                animationSpec = tween(400)
                                            ) + scaleIn(
                                                initialScale = 0.95f,
                                                animationSpec = tween(400)
                                            )) togetherWith (slideOutVertically(
                                                animationSpec = tween(300)
                                            ) { height -> -height / 4 } + fadeOut(
                                                animationSpec = tween(200)
                                            ) + scaleOut(
                                                targetScale = 0.9f,
                                                animationSpec = tween(300)
                                            ))
                                        }
                                        // Player -> Library (Smooth slide down)
                                        else -> {
                                            (slideInVertically(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            ) { height -> -height / 4 } + fadeIn(
                                                animationSpec = tween(300)
                                            ) + scaleIn(
                                                initialScale = 0.9f,
                                                animationSpec = tween(300)
                                            )) togetherWith (slideOutVertically(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            ) { height -> height } + fadeOut(
                                                animationSpec = tween(200)
                                            ))
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
                                    "History" -> {
                                        val historyItems by viewModel.historyItems.collectAsState()
                                        com.project.kataru.ui.HistoryScreen(
                                            historyItems = historyItems,
                                            onItemClick = { item ->
                                                viewModel.resumeBook(item)
                                                showHistory = false
                                                showPlayer = true
                                            },
                                            onBackClick = { showHistory = false },
                                            onClearHistory = { viewModel.clearHistory() }
                                        )
                                    }
                                    "Player" -> {
                                        if (currentBook != null) {
                                            val currentPosition by viewModel.currentPosition.collectAsState()
                                            val duration by viewModel.duration.collectAsState()
                                            val playbackSpeed by viewModel.playbackSpeed.collectAsState()
                                            val volume by viewModel.volume.collectAsState()
                                            PlayerScreen(
                                                book = currentBook!!,
                                                isPlaying = isPlaying,
                                                currentPosition = currentPosition,
                                                duration = duration,
                                                playbackSpeed = playbackSpeed,
                                                volume = volume,
                                                onPlayPauseClick = { viewModel.togglePlayPause() },
                                                onSeek = { viewModel.seekTo(it) },
                                                onSkipForward = { viewModel.skipForward() },
                                                onSkipBackward = { viewModel.skipBackward() },
                                                onSkipNext = { viewModel.skipToNext() },
                                                onSkipPrevious = { viewModel.skipToPrevious() },
                                                onPlaybackSpeedChange = { viewModel.setPlaybackSpeed(it) },
                                                onVolumeChange = { viewModel.setVolume(it) }
                                            )
                                        }
                                    }
                                    "Library" -> {
                                        val isRefreshing by viewModel.isRefreshing.collectAsState()
                                        val activeBook by viewModel.activeBook.collectAsState()
                                        val isPlaying by viewModel.isPlaying.collectAsState()
                                        val isGridView by viewModel.isGridView.collectAsState()
                                        val activeBookProgress by viewModel.activeBookProgress.collectAsState()
                                        val duration by viewModel.duration.collectAsState()

                                        LibraryScreen(
                                            audioBooks = audioBooks,
                                            isRefreshing = isRefreshing,
                                            onBookClick = { book ->
                                                viewModel.playAudioBook(book)
                                                showPlayer = true
                                            },
                                            onRefresh = { viewModel.loadAudioBooks() },
                                            onSettingsClick = { showSettings = true },
                                            onHistoryClick = { showHistory = true },
                                            activeBook = activeBook,
                                            isPlaying = isPlaying,
                                            currentPosition = activeBookProgress,
                                            duration = if (activeBook?.id == viewModel.currentBook.value?.id) duration else activeBook?.duration ?: 0L,
                                            isGridView = isGridView,
                                            onToggleView = { viewModel.toggleViewMode() },
                                            onPlayPause = {
                                                if (activeBook != null && viewModel.currentBook.value == null) {
                                                    val historyItem = viewModel.historyItems.value.find { it.id == activeBook!!.id }
                                                    if (historyItem != null) {
                                                        viewModel.resumeBook(historyItem)
                                                    } else {
                                                        viewModel.playAudioBook(activeBook!!)
                                                    }
                                                } else {
                                                    viewModel.togglePlayPause()
                                                }
                                            },
                                            onNext = { viewModel.skipToNext() },
                                            onPrev = { viewModel.skipToPrevious() },
                                            onMiniPlayerClick = {
                                                 if (activeBook != null && viewModel.currentBook.value == null) {
                                                    val historyItem = viewModel.historyItems.value.find { it.id == activeBook!!.id }
                                                    if (historyItem != null) {
                                                        viewModel.resumeBook(historyItem)
                                                    } else {
                                                        viewModel.playAudioBook(activeBook!!)
                                                    }
                                                }
                                                showPlayer = true 
                                            }
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

    override fun onPause() {
        super.onPause()
        viewModel.saveCurrentProgress()
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
        // Premium permission request UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            PurpleGradientStart,
                            PurpleGradientMid,
                            PurpleGradientEnd
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = "🎧",
                    style = MaterialTheme.typography.displayLarge
                )
                
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.padding(16.dp)
                )
                
                Text(
                    text = "Permission Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.padding(8.dp)
                )
                
                Text(
                    text = "Kataru needs access to your audio files to play audiobooks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.padding(24.dp)
                )
                
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            launcher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                        } else {
                            launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPrimary
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Grant Permission",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}