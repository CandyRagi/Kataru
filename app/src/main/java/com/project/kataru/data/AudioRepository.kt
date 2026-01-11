package com.project.kataru.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioRepository(private val context: Context) {

    private val settingsManager = SettingsManager(context)

    fun getAudioBooks(): List<AudioBook> {
        val sourceFolderUri = settingsManager.sourceFolderUri
        return if (sourceFolderUri != null) {
            scanFolder(sourceFolderUri)
        } else {
            scanMediaStore()
        }
    }

    private fun scanFolder(treeUri: Uri): List<AudioBook> {
        val audioBooks = mutableListOf<AudioBook>()
        val documentFile = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()

        if (documentFile.isDirectory) {
            documentFile.listFiles().forEach { file ->
                if (file.isFile && file.name?.endsWith(".mp3", ignoreCase = true) == true) {
                    audioBooks.add(
                        AudioBook(
                            id = file.uri.toString().hashCode().toString(),
                            title = file.name ?: "Unknown Title",
                            author = "Unknown Author", // DocumentFile doesn't easily give metadata without extra work
                            uri = file.uri,
                            duration = 0L, // Metadata extraction would require MediaMetadataRetriever
                            albumArtUri = Uri.EMPTY // No album art from file system scan easily
                        )
                    )
                }
            }
        }
        return audioBooks
    }

    private fun scanMediaStore(): List<AudioBook> {
        val audioBooks = mutableListOf<AudioBook>()
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn)
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )

                audioBooks.add(
                    AudioBook(
                        id = id.toString(),
                        title = title,
                        author = artist,
                        uri = contentUri,
                        duration = duration,
                        albumArtUri = albumArtUri
                    )
                )
            }
        }
        return audioBooks
    }
}
