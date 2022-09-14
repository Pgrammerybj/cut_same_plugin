package com.cutsame.ui.gallery.album.model.reposity

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.ContentResolverCompat
import androidx.core.os.CancellationSignal
import com.cutsame.ui.gallery.album.model.MediaData
import com.cutsame.ui.gallery.album.model.MediaType
class MediaDataManager {
    fun loadAllData(context: Context): List<MediaData> {
        val resultList = mutableListOf<MediaData>()
        loadImage(context)?.apply {
            val imageList = loadCursor(this, MediaType.TYPE_IMAGE)
            resultList.addAll(imageList)
        }
        loadVideo(context)?.apply {
            val videoList = loadCursor(this, MediaType.TYPE_VIDEO)
            resultList.addAll(videoList)
        }
        return resultList
    }

    private fun loadCursor(cursor: Cursor, mediaType: MediaType): List<MediaData> = cursor.run {
        val resultList = mutableListOf<MediaData>()
        val idIndex = getColumnIndex(MediaStore.MediaColumns._ID)
        val pathIndex = getColumnIndex(MediaStore.MediaColumns.DATA)
        val mimeTypeIndex = getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
        val sizeIndex = getColumnIndex(MediaStore.MediaColumns.SIZE)
        val dateAddIndex = getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
        val dateModifiedIndex = getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
        val widthIndex = getColumnIndex(MediaStore.MediaColumns.WIDTH)
        val heightIndex = getColumnIndex(MediaStore.MediaColumns.HEIGHT)
        val durationIndex = getColumnIndex(MediaStore.Video.Media.DURATION)

        if (idIndex == -1 || mimeTypeIndex == -1) {
            // 必要字段
            emptyList()
        } else {
            while (cursor.moveToNext()) {
                val id = getInt(idIndex)
                val path = getString(pathIndex)
                val mimeType = getString(mimeTypeIndex)
                val size = getLong(sizeIndex)
                val displayName = id.toString()
                val dateAdd = getLong(dateAddIndex)
                val dateModify = getLong(dateModifiedIndex)
                val width = getInt(widthIndex)
                val height = getInt(heightIndex)
                val duration = if (durationIndex == -1) 0 else getLong(durationIndex)
                // val orientation = getInt(orientationIndex)

                val uri = ContentUris.withAppendedId(
                    when (mediaType) {
                        MediaType.TYPE_IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        MediaType.TYPE_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    },
                    id.toLong()
                )

                resultList.add(
                    MediaData(
                        mimeType = mimeType,
                        path = path,
                        uri = uri,
                        dateAdd = dateAdd,
                        dateModify = dateModify,
                        displayName = displayName,
                        width = width,
                        height = height,
                        duration = duration,
                        orientation = 0,
                        size = size
                    )
                )
            }
            cursor.close()
            resultList
        }
    }

    private fun loadImage(
        context: Context
    ): Cursor? {

        var mimeTypeSelection = ""
        val mimeTypeSelectionArgs = arrayOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp"
        )

        for (i in 0 until mimeTypeSelectionArgs.size - 1) {
            mimeTypeSelection += MediaStore.Images.Media.MIME_TYPE + "=? or "
        }
        mimeTypeSelection += MediaStore.Images.Media.MIME_TYPE + "=?"

        return query(
            context,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.WIDTH,
                MediaStore.MediaColumns.HEIGHT
                // MediaStore.MediaColumns.ORIENTATION
            ),
            selection = mimeTypeSelection,
            selectionArgs = mimeTypeSelectionArgs
        )
    }

    private fun loadVideo(
        context: Context
    ): Cursor? {
        return query(
            context,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.WIDTH,
                MediaStore.MediaColumns.HEIGHT,
                MediaStore.Video.Media.DURATION
                // MediaStore.MediaColumns.ORIENTATION
            )
        )
    }

    private fun query(
        context: Context,
        uri: Uri,
        projection: Array<String> = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT
            // MediaStore.MediaColumns.ORIENTATION
        ),
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String? = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC",
        cancelSignal: CancellationSignal? = null
    ): Cursor? {
        return ContentResolverCompat.query(
            context.contentResolver,
            uri,
            projection,
            selection,
            selectionArgs,
            sortOrder,
            cancelSignal
        )
    }
}