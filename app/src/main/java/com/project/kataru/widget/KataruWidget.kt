package com.project.kataru.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.widget.RemoteViews
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.project.kataru.MainActivity
import com.project.kataru.R
import com.project.kataru.service.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KataruWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_PLAY = "com.project.kataru.action.PLAY"
        const val ACTION_PAUSE = "com.project.kataru.action.PAUSE"
        const val ACTION_FORWARD = "com.project.kataru.action.FORWARD"
        const val ACTION_REWIND = "com.project.kataru.action.REWIND"

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            title: String,
            author: String,
            isPlaying: Boolean,
            albumArtUri: String?
        ) {
            // Save state to SharedPreferences
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("title_$appWidgetId", title)
                putString("author_$appWidgetId", author)
                putString("albumArtUri_$appWidgetId", albumArtUri)
                apply()
            }

            val views = RemoteViews(context.packageName, R.layout.widget_kataru)

            // Update text
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_author, author)

            // Update Play/Pause icon
            val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            views.setImageViewResource(R.id.widget_play_pause, playPauseIcon)

            // Load Album Art
            if (albumArtUri != null) {
                val loader = ImageLoader.Builder(context).build()
                val request = ImageRequest.Builder(context)
                    .data(albumArtUri)
                    .size(256, 256) // Resize to prevent TransactionTooLargeException
                    .allowHardware(false) // RemoteViews requires software bitmaps
                    .build()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val result = loader.execute(request)
                        if (result is SuccessResult) {
                            val bitmap = (result.drawable as BitmapDrawable).bitmap
                            views.setImageViewBitmap(R.id.widget_album_art, bitmap)
                        } else {
                            views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_launcher_foreground)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_launcher_foreground)
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } else {
                views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_launcher_foreground)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            // Click Intents
            views.setOnClickPendingIntent(R.id.widget_play_pause, getPendingIntent(context, if (isPlaying) ACTION_PAUSE else ACTION_PLAY))
            views.setOnClickPendingIntent(R.id.widget_forward, getPendingIntent(context, ACTION_FORWARD))
            views.setOnClickPendingIntent(R.id.widget_rewind, getPendingIntent(context, ACTION_REWIND))
            
            // Open App on Title Click
            val appIntent = Intent(context, MainActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_title, appPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_album_art, appPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun getPendingIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, PlaybackService::class.java).apply {
                this.action = action
            }
            // Use getForegroundService to allow starting service from background on Android 12+
            return PendingIntent.getForegroundService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        for (appWidgetId in appWidgetIds) {
            // Restore state from SharedPreferences
            val title = prefs.getString("title_$appWidgetId", "No Media") ?: "No Media"
            val author = prefs.getString("author_$appWidgetId", "Tap to open") ?: "Tap to open"
            val albumArtUri = prefs.getString("albumArtUri_$appWidgetId", null)
            
            updateWidget(context, appWidgetManager, appWidgetId, title, author, false, albumArtUri)
        }
    }
}
