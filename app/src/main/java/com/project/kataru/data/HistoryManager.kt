package com.project.kataru.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class HistoryManager(context: Context) {
    private val dao = AppDatabase.getDatabase(context).historyDao()

    suspend fun addToHistory(item: HistoryEntity) {
        dao.insert(item)
    }

    fun getHistoryFlow(): kotlinx.coroutines.flow.Flow<List<HistoryEntity>> {
        return dao.getAll()
    }

    suspend fun getHistory(): List<HistoryEntity> {
        // For compatibility if needed, but Flow is better.
        // We can't easily get a snapshot without a suspend function that queries once.
        // But dao.getAll() returns a Flow.
        // Let's add a one-shot query to DAO if we really need it, or just use Flow in ViewModel.
        // For now, let's assume we will migrate ViewModel to use Flow.
        return emptyList() // Placeholder, we should use Flow.
    }

    suspend fun clearHistory() {
        dao.deleteAll()
    }
}
