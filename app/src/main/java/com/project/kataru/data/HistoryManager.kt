package com.project.kataru.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class HistoryItem(
    val id: String,
    val title: String,
    val author: String,
    val albumArtUri: String,
    val uri: String,
    val duration: Long,
    val position: Long,
    val timestamp: Long
)

class HistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("kataru_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_HISTORY = "playback_history"
        private const val MAX_HISTORY_SIZE = 20
    }

    fun addToHistory(item: HistoryItem) {
        val history = getHistory().toMutableList()
        // Remove existing entry for the same book if it exists
        history.removeAll { it.id == item.id }
        // Add new item to the beginning
        history.add(0, item)
        // Trim to max size
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(history.lastIndex)
        }
        saveHistory(history)
    }

    fun getHistory(): List<HistoryItem> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val type = object : TypeToken<List<HistoryItem>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun saveHistory(history: List<HistoryItem>) {
        val json = gson.toJson(history)
        prefs.edit {
            putString(KEY_HISTORY, json)
        }
    }

    fun clearHistory() {
        prefs.edit {
            remove(KEY_HISTORY)
        }
    }
}
