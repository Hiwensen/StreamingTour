package com.shaowei.streaming

import android.util.Log
import java.io.File

private const val TAG = "FileUtils"

fun deleteFileOrDirectory(file: File) {
    if (!file.exists()) {
        return
    }

    if (file.isFile) {
        try {
            file.delete()
        } catch (securityException: SecurityException) {
            Log.e(TAG, securityException.toString())
            return
        }
    }

    if (file.isDirectory) {
        file.deleteRecursively()
    }
}