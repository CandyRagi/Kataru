package com.project.kataru.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_items")
data class HistoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val albumArtUri: String,
    val uri: String,
    val duration: Long,
    val position: Long,
    val timestamp: Long
)
