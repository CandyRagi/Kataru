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
        val retriever = android.media.MediaMetadataRetriever()
        val supportedExtensions = listOf(".mp3", ".m4b", ".m4a", ".aac", ".flac", ".ogg", ".wav")
        val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".webp")

        if (documentFile.isDirectory) {
            documentFile.listFiles().forEach { file ->
                val fileName = file.name?.lowercase() ?: ""
                
                // Check if it's an audio file directly in the source folder
                if (file.isFile && supportedExtensions.any { fileName.endsWith(it) }) {
                    addAudioBookFromFile(file, retriever, audioBooks)
                }
                
                // Check if it's a subfolder (1 level deep only)
                if (file.isDirectory) {
                    val subFiles = file.listFiles()
                    
                    // Find audio file in subfolder
                    val audioFile = subFiles.find { subFile ->
                        val subFileName = subFile.name?.lowercase() ?: ""
                        subFile.isFile && supportedExtensions.any { subFileName.endsWith(it) }
                    }
                    
                    // Find cover image in subfolder
                    val coverFile = subFiles.find { subFile ->
                        val subFileName = subFile.name?.lowercase() ?: ""
                        subFile.isFile && imageExtensions.any { subFileName.endsWith(it) }
                    }
                    
                    if (audioFile != null) {
                        try {
                            retriever.setDataSource(context, audioFile.uri)
                            val title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE) 
                                ?: file.name // Use folder name as title
                                ?: "Unknown Title"
                            val artist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Author"
                            val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                            val duration = durationStr?.toLongOrNull() ?: 0L
                            
                            val id = audioFile.uri.toString().hashCode().toString()
                            
                            // Use cover image from folder if available, otherwise try embedded art
                            val albumArtUri = if (coverFile != null) {
                                coverFile.uri
                            } else {
                                getEmbeddedAlbumArt(context, audioFile.uri, id)
                            }
                            
                            audioBooks.add(
                                AudioBook(
                                    id = id,
                                    title = title,
                                    author = artist,
                                    uri = audioFile.uri,
                                    duration = duration,
                                    albumArtUri = albumArtUri
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            audioBooks.add(
                                AudioBook(
                                    id = audioFile.uri.toString().hashCode().toString(),
                                    title = file.name ?: "Unknown Title",
                                    author = "Unknown Author",
                                    uri = audioFile.uri,
                                    duration = 0L,
                                    albumArtUri = coverFile?.uri ?: Uri.EMPTY
                                )
                            )
                        }
                    }
                }
            }
        }
        try {
            retriever.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return audioBooks.sortedBy { it.title.lowercase() }
    }

    private fun addAudioBookFromFile(file: DocumentFile, retriever: android.media.MediaMetadataRetriever, audioBooks: MutableList<AudioBook>) {
        try {
            retriever.setDataSource(context, file.uri)
            val title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE) ?: file.name ?: "Unknown Title"
            val artist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Author"
            val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L
            
            val id = file.uri.toString().hashCode().toString()
            val albumArtUri = getEmbeddedAlbumArt(context, file.uri, id)
            
            audioBooks.add(
                AudioBook(
                    id = id,
                    title = title,
                    author = artist,
                    uri = file.uri,
                    duration = duration,
                    albumArtUri = albumArtUri
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            audioBooks.add(
                AudioBook(
                    id = file.uri.toString().hashCode().toString(),
                    title = file.name ?: "Unknown Title",
                    author = "Unknown Author",
                    uri = file.uri,
                    duration = 0L,
                    albumArtUri = Uri.EMPTY
                )
            )
        }
    }

    private fun getEmbeddedAlbumArt(context: Context, fileUri: Uri, id: String): Uri {
        try {
            val cacheDir = java.io.File(context.cacheDir, "album_art")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val artFile = java.io.File(cacheDir, "$id.jpg")
            
            if (artFile.exists()) {
                return Uri.fromFile(artFile)
            }

            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, fileUri)
            val art = retriever.embeddedPicture
            retriever.release()

            if (art != null) {
                java.io.FileOutputStream(artFile).use { it.write(art) }
                return Uri.fromFile(artFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Uri.EMPTY
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
