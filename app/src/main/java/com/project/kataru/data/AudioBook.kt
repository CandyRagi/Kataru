package com.project.kataru.data

import android.net.Uri

data class AudioBook(
    val id: String,
    val title: String,
    val author: String,
    val uri: Uri,
    val duration: Long,
    val albumArtUri: Uri? = null
)
