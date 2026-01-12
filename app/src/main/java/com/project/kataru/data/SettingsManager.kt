package com.project.kataru.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("kataru_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SOURCE_FOLDER_URI = "source_folder_uri"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_SKIP_FORWARD = "skip_forward"
        private const val KEY_SKIP_BACKWARD = "skip_backward"
        private const val KEY_PLAYBACK_SPEED = "playback_speed"
    }

    var sourceFolderUri: Uri?
        get() {
            val uriString = prefs.getString(KEY_SOURCE_FOLDER_URI, null)
            return uriString?.let { Uri.parse(it) }
        }
        set(value) {
            prefs.edit {
                putString(KEY_SOURCE_FOLDER_URI, value?.toString())
            }
        }

    var accentColor: Long
        get() = prefs.getLong(KEY_ACCENT_COLOR, 0xFFBB86FC) // Default AccentPrimary
        set(value) {
            prefs.edit { putLong(KEY_ACCENT_COLOR, value) }
        }

    var skipForwardInterval: Long
        get() = prefs.getLong(KEY_SKIP_FORWARD, 10000L)
        set(value) {
            prefs.edit { putLong(KEY_SKIP_FORWARD, value) }
        }

    var skipBackwardInterval: Long
        get() = prefs.getLong(KEY_SKIP_BACKWARD, 10000L)
        set(value) {
            prefs.edit { putLong(KEY_SKIP_BACKWARD, value) }
        }

    var playbackSpeed: Float
        get() = prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f)
        set(value) {
            prefs.edit { putFloat(KEY_PLAYBACK_SPEED, value) }
        }

    fun clearSourceFolder() {
        prefs.edit {
            remove(KEY_SOURCE_FOLDER_URI)
        }
    }
}
