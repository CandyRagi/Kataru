package com.project.kataru.service

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.take

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var historyManager: com.project.kataru.data.HistoryManager
    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            com.project.kataru.widget.KataruWidget.ACTION_PLAY -> {
                if (mediaSession?.player?.currentMediaItem == null) {
                    restoreLastPlayedBook()
                } else {
                    mediaSession?.player?.play()
                }
            }
            com.project.kataru.widget.KataruWidget.ACTION_PAUSE -> mediaSession?.player?.pause()
            com.project.kataru.widget.KataruWidget.ACTION_NEXT -> mediaSession?.player?.seekToNext()
            com.project.kataru.widget.KataruWidget.ACTION_PREV -> mediaSession?.player?.seekToPrevious()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        historyManager = com.project.kataru.data.HistoryManager(application)
        val player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()
        
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateWidget(player)
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                updateWidget(player)
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                updateWidget(player)
            }
        })
    }

    private fun restoreLastPlayedBook() {
        serviceScope.launch {
            historyManager.getHistoryFlow().take(1).collect { history ->
                if (history.isNotEmpty()) {
                    val lastItem = history.first()
                    val mediaItem = androidx.media3.common.MediaItem.Builder()
                        .setMediaId(lastItem.id)
                        .setUri(android.net.Uri.parse(lastItem.uri))
                        .setMediaMetadata(
                            androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(lastItem.title)
                                .setArtist(lastItem.author)
                                .setArtworkUri(android.net.Uri.parse(lastItem.albumArtUri))
                                .build()
                        )
                        .build()

                    mediaSession?.player?.let { player ->
                        player.setMediaItem(mediaItem, lastItem.position)
                        player.prepare()
                        player.play()
                    }
                }
            }
        }
    }

    private fun updateWidget(player: Player) {
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(this, com.project.kataru.widget.KataruWidget::class.java))
        
        val mediaMetadata = player.mediaMetadata
        val title = mediaMetadata.title?.toString() ?: "No Media"
        val author = mediaMetadata.artist?.toString() ?: "Tap to open"
        val albumArtUri = mediaMetadata.artworkUri?.toString()
        
        for (appWidgetId in appWidgetIds) {
            com.project.kataru.widget.KataruWidget.updateWidget(
                context = this,
                appWidgetManager = appWidgetManager,
                appWidgetId = appWidgetId,
                title = title,
                author = author,
                isPlaying = player.isPlaying,
                albumArtUri = albumArtUri
            )
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
