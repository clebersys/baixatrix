package com.cleber.baixatrix.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

data class LibraryItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val durationMillis: Long,
    val sizeBytes: Long,
    val dateAdded: Long,
    val width: Int,
    val height: Int
)

/** Consulta e remove os vídeos salvos pela pasta Filmes/BaixaTrix. */
class LibraryRepository(private val context: Context) {

    fun list(): List<LibraryItem> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        )

        val selection: String
        val selectionArgs: Array<String>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection += MediaStore.Video.Media.RELATIVE_PATH
            selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
            selectionArgs = arrayOf("%Movies/${DownloadRepository.FOLDER_NAME}/%")
        } else {
            projection += MediaStore.Video.Media.DATA
            selection = "${MediaStore.Video.Media.DATA} LIKE ?"
            selectionArgs = arrayOf("%/Movies/${DownloadRepository.FOLDER_NAME}/%")
        }

        val items = mutableListOf<LibraryItem>()
        context.contentResolver.query(
            collection,
            projection.toTypedArray(),
            selection,
            selectionArgs,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                items += LibraryItem(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    name = cursor.getString(nameColumn).orEmpty(),
                    durationMillis = cursor.getLong(durationColumn),
                    sizeBytes = cursor.getLong(sizeColumn),
                    dateAdded = cursor.getLong(dateColumn),
                    width = cursor.getInt(widthColumn),
                    height = cursor.getInt(heightColumn)
                )
            }
        }
        return items
    }

    fun delete(item: LibraryItem): Boolean = runCatching {
        context.contentResolver.delete(item.uri, null, null) > 0
    }.getOrDefault(false)
}
