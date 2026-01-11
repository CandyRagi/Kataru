package com.project.kataru.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import android.content.ComponentName
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.project.kataru.data.AudioBook
import com.project.kataru.data.AudioRepository
import com.project.kataru.service.PlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val historyManager = com.project.kataru.data.HistoryManager(application)

    private val _historyItems = MutableStateFlow<List<com.project.kataru.data.HistoryItem>>(emptyList())
    val historyItems: StateFlow<List<com.project.kataru.data.HistoryItem>> = _historyItems.asStateFlow()

    init {
        refreshHistory()
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

    private fun refreshHistory() {
        _historyItems.value = historyManager.getHistory()
    }

    private fun saveCurrentProgress() {
        val book = _currentBook.value ?: return
        val position = _currentPosition.value
        val duration = _duration.value
        val item = com.project.kataru.data.HistoryItem(
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
        refreshHistory()
    }

    fun clearHistory() {
        historyManager.clearHistory()
        refreshHistory()
    }

    fun resumeBook(item: com.project.kataru.data.HistoryItem) {
        val book = AudioBook(
            id = item.id,
            title = item.title,
            author = item.author,
            albumArtUri = android.net.Uri.parse(item.albumArtUri),
            uri = android.net.Uri.parse(item.uri),
            duration = item.duration
        )
        
        playAudioBook(book)
        seekTo(item.position)
    }

    private fun startPositionUpdater() {
        viewModelScope.launch {
            while (_isPlaying.value) {
                _currentPosition.value = controller?.currentPosition ?: 0L
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

    fun playAudioBook(book: AudioBook) {
        val historyItem = historyManager.getHistory().find { it.id == book.id }
        val startPosition = historyItem?.position ?: 0L

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

        controller?.setMediaItem(mediaItem)
        controller?.prepare()
        if (startPosition > 0) {
            controller?.seekTo(startPosition)
        }
        controller?.play()
        _currentBook.value = book
        _currentPosition.value = startPosition
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
            val newPosition = (it.currentPosition + 10000).coerceAtMost(it.duration)
            it.seekTo(newPosition)
            _currentPosition.value = newPosition
        }
    }

    fun skipBackward() {
        controller?.let {
            val newPosition = (it.currentPosition - 10000).coerceAtLeast(0)
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

    override fun onCleared() {
        saveCurrentProgress()
        super.onCleared()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
