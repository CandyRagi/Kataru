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

    init {
        val sessionToken = SessionToken(application, ComponentName(application, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    // Update current book based on media id
                    mediaItem?.mediaId?.let { id ->
                        _currentBook.value = _audioBooks.value.find { it.id == id }
                    }
                }
            })
        }, MoreExecutors.directExecutor())
    }

    fun loadAudioBooks() {
        viewModelScope.launch {
            val books = repository.getAudioBooks()
            _audioBooks.value = books
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

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
