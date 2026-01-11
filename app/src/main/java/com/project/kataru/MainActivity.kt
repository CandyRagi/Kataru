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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
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
import com.project.kataru.ui.theme.KataruTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

                        // Auto-show player if a book is selected (and we aren't already there)
                        LaunchedEffect(currentBook) {
                            if (currentBook != null) {
                                showPlayer = true
                            }
                        }

                        BackHandler(enabled = showPlayer) {
                            showPlayer = false
                        }

                        AnimatedContent(
                            targetState = showPlayer,
                            label = "ScreenTransition",
                            transitionSpec = {
                                if (targetState) {
                                    slideInVertically { height -> height } togetherWith slideOutVertically { height -> -height }
                                } else {
                                    slideInVertically { height -> -height } togetherWith slideOutVertically { height -> height }
                                }
                            }
                        ) { isPlayerVisible ->
                            if (isPlayerVisible && currentBook != null) {
                                PlayerScreen(
                                    book = currentBook!!,
                                    isPlaying = isPlaying,
                                    onPlayPauseClick = { viewModel.togglePlayPause() }
                                )
                            } else {
                                if (audioBooks.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "No Audiobooks found.\nAdd MP3s to your device.",
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                } else {
                                    LibraryScreen(
                                        audioBooks = audioBooks,
                                        onBookClick = { book ->
                                            viewModel.playAudioBook(book)
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