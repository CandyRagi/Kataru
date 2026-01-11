package com.project.kataru.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("kataru_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SOURCE_FOLDER_URI = "source_folder_uri"
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

    fun clearSourceFolder() {
        prefs.edit {
            remove(KEY_SOURCE_FOLDER_URI)
        }
    }
}
