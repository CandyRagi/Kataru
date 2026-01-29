package com.project.kataru.service

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.take

/*
 * PlaybackService - The background service that actually plays the audio. Uses ExoPlayer
 * under the hood via Media3. Handles widget commands (play, pause, seek), restores the
 * last played book from history if needed, and keeps the widget updated with current state.
 * Runs as a foreground service so playback continues even when the app is closed.
 * Now also saves playback progress to history so widget-controlled playback is tracked.
 */

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
            com.project.kataru.widget.KataruWidget.ACTION_PAUSE -> {
                mediaSession?.player?.pause()
                // Save progress when paused via widget
                saveCurrentProgress()
            }
            com.project.kataru.widget.KataruWidget.ACTION_FORWARD -> mediaSession?.player?.let { player ->
                val newPosition = (player.currentPosition + 10_000).coerceAtMost(player.duration)
                player.seekTo(newPosition)
                // Save progress after seeking
                saveCurrentProgress()
            }
            com.project.kataru.widget.KataruWidget.ACTION_REWIND -> mediaSession?.player?.let { player ->
                val newPosition = (player.currentPosition - 10_000).coerceAtLeast(0)
                player.seekTo(newPosition)
                // Save progress after seeking
                saveCurrentProgress()
            }
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
                // Save progress when playback stops (paused or ended)
                if (!isPlaying) {
                    saveCurrentProgress()
                }
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

    private fun saveCurrentProgress() {
        val player = mediaSession?.player ?: return
        val mediaItem = player.currentMediaItem ?: return
        val metadata = player.mediaMetadata
        
        val position = player.currentPosition
        val duration = player.duration
        
        // Only save if we have valid data
        if (position <= 0 && duration <= 0) return
        
        serviceScope.launch {
            val item = com.project.kataru.data.HistoryEntity(
                id = mediaItem.mediaId,
                title = metadata.title?.toString() ?: "Unknown",
                author = metadata.artist?.toString() ?: "Unknown",
                albumArtUri = metadata.artworkUri?.toString() ?: "",
                uri = mediaItem.localConfiguration?.uri?.toString() ?: "",
                duration = if (duration > 0) duration else 0L,
                position = position,
                timestamp = System.currentTimeMillis()
            )
            historyManager.addToHistory(item)
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

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Save progress before stopping
        saveCurrentProgress()
        // Stop playback when app is swiped away from recents
        mediaSession?.player?.let { player ->
            if (player.isPlaying) {
                player.pause()
            }
            // Update widget to show paused state
            updateWidget(player)
        }
        // Stop the service so audio doesn't continue
        stopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        // Save progress before destroying
        saveCurrentProgress()
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
