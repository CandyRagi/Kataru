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

    init {
        val sessionToken = SessionToken(application, ComponentName(application, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) {
                        startPositionUpdater()
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
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
        controller?.play()
        _currentBook.value = book
    }

    fun togglePlayPause() {
        if (controller?.isPlaying == true) {
            controller?.pause()
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
        val current = _currentBook.value ?: return
        val list = _audioBooks.value
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex < list.size - 1) {
            playAudioBook(list[currentIndex + 1])
        }
    }

    fun skipToPrevious() {
        val current = _currentBook.value ?: return
        val list = _audioBooks.value
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            playAudioBook(list[currentIndex - 1])
        }
    }

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
