package com.example.ui.util

import android.net.Uri
import java.io.File

object MediaUriHelper {
    fun isContentUri(path: String): Boolean {
        return path.startsWith("content://", ignoreCase = true) ||
            path.startsWith("file://", ignoreCase = true)
    }

    fun toCoilModel(path: String): Any {
        return if (isContentUri(path)) Uri.parse(path) else File(path)
    }

    fun toAndroidUri(path: String): Uri {
        return if (isContentUri(path)) Uri.parse(path) else Uri.fromFile(File(path))
    }
}
