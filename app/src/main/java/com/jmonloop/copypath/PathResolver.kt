package com.jmonloop.copypath

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import java.io.File

object PathResolver {
    private const val TAG = "PathResolver"

    fun resolve(context: Context, uri: Uri): String {
        Log.d(TAG, "Resolving URI: $uri")

        // Strategy 1: Direct file:// URI
        if (uri.scheme == "file") {
            val path = uri.path
            if (path != null) {
                Log.d(TAG, "Resolved via file:// scheme: $path")
                return path
            }
        }

        // Strategy 2: MediaStore _DATA column query
        val mediaStorePath = queryMediaStore(context, uri)
        if (mediaStorePath != null) {
            Log.d(TAG, "Resolved via MediaStore: $mediaStorePath")
            return mediaStorePath
        }

        // Strategy 3: DocumentsContract parsing for download documents
        if (DocumentsContract.isDocumentUri(context, uri)) {
            val documentsPath = resolveDocumentsContract(context, uri)
            if (documentsPath != null) {
                Log.d(TAG, "Resolved via DocumentsContract: $documentsPath")
                return documentsPath
            }
        }

        // Strategy 4: /proc/self/fd symlink resolution
        val fdPath = resolveProcSelfFd(context, uri)
        if (fdPath != null) {
            Log.d(TAG, "Resolved via /proc/self/fd: $fdPath")
            return fdPath
        }

        // Strategy 5: Fallback to content URI string
        val fallback = uri.toString()
        Log.d(TAG, "Fallback to content URI: $fallback")
        return fallback
    }

    private fun queryMediaStore(context: Context, uri: Uri): String? {
        if (uri.scheme != "content") return null

        return try {
            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (columnIndex != -1) {
                        cursor.getString(columnIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore", e)
            null
        }
    }

    private fun resolveDocumentsContract(context: Context, uri: Uri): String? {
        return try {
            val docId = DocumentsContract.getDocumentId(uri)

            // Handle downloads provider
            if (uri.authority == "com.android.providers.downloads.documents") {
                val contentUriPrefixesToTry = arrayOf(
                    "content://downloads/public_downloads",
                    "content://downloads/my_downloads",
                    "content://downloads/all_downloads"
                )

                for (prefix in contentUriPrefixesToTry) {
                    try {
                        val contentUri = Uri.parse("$prefix/$docId")
                        val path = queryMediaStore(context, contentUri)
                        if (path != null) return path
                    } catch (e: Exception) {
                        // Try next prefix
                    }
                }
            }

            // Handle external storage provider
            if (uri.authority == "com.android.externalstorage.documents") {
                val split = docId.split(":")
                if (split.size >= 2) {
                    val type = split[0]
                    val path = split[1]

                    if ("primary".equals(type, ignoreCase = true)) {
                        return "${android.os.Environment.getExternalStorageDirectory()}/$path"
                    }
                }
            }

            // Handle media documents
            if (uri.authority == "com.android.providers.media.documents") {
                val split = docId.split(":")
                if (split.size >= 2) {
                    val type = split[0]
                    val id = split[1]

                    val contentUri = when (type) {
                        "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> null
                    }

                    if (contentUri != null) {
                        val selection = "_id=?"
                        val selectionArgs = arrayOf(id)
                        return queryPath(context, contentUri, selection, selectionArgs)
                    }
                }
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving DocumentsContract", e)
            null
        }
    }

    private fun queryPath(context: Context, uri: Uri, selection: String, selectionArgs: Array<String>): String? {
        return try {
            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (columnIndex != -1) {
                        cursor.getString(columnIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying path", e)
            null
        }
    }

    private fun resolveProcSelfFd(context: Context, uri: Uri): String? {
        if (uri.scheme != "content") return null

        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val fd = pfd.fd
                val procPath = "/proc/self/fd/$fd"

                try {
                    val file = File(procPath)
                    val canonicalPath = file.canonicalPath

                    // Verify it's a real path, not a pipe or socket
                    if (canonicalPath.startsWith("/")) {
                        canonicalPath
                    } else null
                } catch (e: Exception) {
                    Log.e(TAG, "Error resolving /proc/self/fd", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening file descriptor", e)
            null
        }
    }
}
