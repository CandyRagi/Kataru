package com.project.kataru.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import android.content.ComponentName
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.project.kataru.data.AudioBook
import com.project.kataru.data.AudioRepository
import com.project.kataru.service.PlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/*
 * MainViewModel - The brain of the app. Handles all the state for playback, library,
 * settings, and history. Connects to the PlaybackService via MediaController and keeps
 * the UI in sync with what's actually playing. Also manages accent colors, playback speed,
 * volume, and refreshing the audiobook list.
 */

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AudioRepository(application)

    private val _audioBooks = MutableStateFlow<List<AudioBook>>(emptyList())
    val audioBooks: StateFlow<List<AudioBook>> = _audioBooks.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController?
        get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentBook = MutableStateFlow<AudioBook?>(null)
    val currentBook: StateFlow<AudioBook?> = _currentBook.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val sharedPreferences = application.getSharedPreferences("kataru_prefs", android.content.Context.MODE_PRIVATE)
    private val _isGridView = MutableStateFlow(sharedPreferences.getBoolean("is_grid_view", true))
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val settingsManager = com.project.kataru.data.SettingsManager(application)

    private val _accentColor = MutableStateFlow(Color(settingsManager.accentColor))
    val accentColor: StateFlow<Color> = _accentColor.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(settingsManager.playbackSpeed)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val historyManager = com.project.kataru.data.HistoryManager(application)

    private val _historyItems = MutableStateFlow<List<com.project.kataru.data.HistoryEntity>>(emptyList())
    val historyItems: StateFlow<List<com.project.kataru.data.HistoryEntity>> = _historyItems.asStateFlow()

    val activeBook: StateFlow<AudioBook?> = kotlinx.coroutines.flow.combine(_currentBook, _historyItems) { current, history ->
        if (current != null) {
            current
        } else if (history.isNotEmpty()) {
            val item = history.first()
            AudioBook(
                id = item.id,
                title = item.title,
                author = item.author,
                albumArtUri = android.net.Uri.parse(item.albumArtUri),
                uri = android.net.Uri.parse(item.uri),
                duration = item.duration
            )
        } else {
            null
        }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val activeBookProgress: StateFlow<Long> = kotlinx.coroutines.flow.combine(_currentBook, _currentPosition, _historyItems) { current, currentPos, history ->
        if (current != null) {
            currentPos
        } else if (history.isNotEmpty()) {
            history.first().position
        } else {
            0L
        }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    init {
        viewModelScope.launch {
            historyManager.getHistoryFlow().collect { items ->
                _historyItems.value = items
            }
        }
        
        val sessionToken = SessionToken(application, ComponentName(application, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) {
                        startPositionUpdater()
                    } else {
                        saveCurrentProgress()
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    // Save progress of previous book before switching
                    saveCurrentProgress()
                    
                    // Update current book based on media id
                    mediaItem?.mediaId?.let { id ->
                        _currentBook.value = _audioBooks.value.find { it.id == id }
                    }
                    _duration.value = controller?.duration ?: 0L
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                     if (playbackState == Player.STATE_READY) {
                         _duration.value = controller?.duration ?: 0L
                     }
                }
            })
        }, MoreExecutors.directExecutor())
    }

    fun saveCurrentProgress() {
        val book = _currentBook.value ?: return
        val position = _currentPosition.value
        val duration = _duration.value
        
        viewModelScope.launch {
            val item = com.project.kataru.data.HistoryEntity(
                id = book.id,
                title = book.title,
                author = book.author,
                albumArtUri = book.albumArtUri.toString(),
                uri = book.uri.toString(),
                duration = duration,
                position = position,
                timestamp = System.currentTimeMillis()
            )
            historyManager.addToHistory(item)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyManager.clearHistory()
        }
    }

    fun resumeBook(item: com.project.kataru.data.HistoryEntity) {
        val book = AudioBook(
            id = item.id,
            title = item.title,
            author = item.author,
            albumArtUri = android.net.Uri.parse(item.albumArtUri),
            uri = android.net.Uri.parse(item.uri),
            duration = item.duration
        )
        
        playAudioBook(book, item.position)
    }

    private fun startPositionUpdater() {
        viewModelScope.launch {
            while (_isPlaying.value) {
                val currentPos = controller?.currentPosition
                if (currentPos != null && currentPos > 0) {
                    _currentPosition.value = currentPos
                }
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

    fun loadAudioBooks() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // Add a small delay to ensure the refresh animation is visible and UI updates
            kotlinx.coroutines.delay(500)
            val books = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                repository.getAudioBooks()
            }
            _audioBooks.value = books
            _isRefreshing.value = false
        }
    }

    fun playAudioBook(book: AudioBook, startPosition: Long? = null) {
        val finalStartPosition = if (startPosition != null) {
            startPosition
        } else {
            val historyItem = _historyItems.value.find { it.id == book.id }
            historyItem?.position ?: 0L
        }

        val mediaItem = MediaItem.Builder()
            .setMediaId(book.id)
            .setUri(book.uri)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(book.title)
                    .setArtist(book.author)
                    .setArtworkUri(book.albumArtUri)
                    .build()
            )
            .build()

        controller?.setMediaItem(mediaItem, finalStartPosition)
        controller?.prepare()
        controller?.play()
        _currentBook.value = book
        _currentPosition.value = finalStartPosition
    }

    fun togglePlayPause() {
        if (controller?.isPlaying == true) {
            controller?.pause()
            saveCurrentProgress()
        } else {
            controller?.play()
        }
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position)
        _currentPosition.value = position
    }

    fun skipForward() {
        controller?.let {
            val interval = _skipForwardInterval.value
            val newPosition = (it.currentPosition + interval).coerceAtMost(it.duration)
            it.seekTo(newPosition)
            _currentPosition.value = newPosition
        }
    }

    fun skipBackward() {
        controller?.let {
            val interval = _skipBackwardInterval.value
            val newPosition = (it.currentPosition - interval).coerceAtLeast(0)
            it.seekTo(newPosition)
            _currentPosition.value = newPosition
        }
    }

    fun skipToNext() {
        saveCurrentProgress()
        val current = _currentBook.value ?: return
        val list = _audioBooks.value
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex < list.size - 1) {
            playAudioBook(list[currentIndex + 1])
        }
    }

    fun skipToPrevious() {
        saveCurrentProgress()
        val current = _currentBook.value ?: return
        val list = _audioBooks.value
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            playAudioBook(list[currentIndex - 1])
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        settingsManager.playbackSpeed = speed
        controller?.setPlaybackSpeed(speed)
    }

    fun updateAccentColor(color: Color) {
        _accentColor.value = color
        settingsManager.accentColor = color.toArgb().toLong()
    }

    private val _skipForwardInterval = MutableStateFlow(settingsManager.skipForwardInterval)
    val skipForwardInterval: StateFlow<Long> = _skipForwardInterval.asStateFlow()

    private val _skipBackwardInterval = MutableStateFlow(settingsManager.skipBackwardInterval)
    val skipBackwardInterval: StateFlow<Long> = _skipBackwardInterval.asStateFlow()

    fun setSkipForwardInterval(interval: Long) {
        _skipForwardInterval.value = interval
        settingsManager.skipForwardInterval = interval
    }

    fun setSkipBackwardInterval(interval: Long) {
        _skipBackwardInterval.value = interval
        settingsManager.skipBackwardInterval = interval
    }

    fun setVolume(volume: Float) {
        _volume.value = volume
        controller?.volume = volume
    }

    fun toggleViewMode() {
        val newValue = !_isGridView.value
        _isGridView.value = newValue
        sharedPreferences.edit().putBoolean("is_grid_view", newValue).apply()
    }

    override fun onCleared() {
        saveCurrentProgress()
        super.onCleared()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
