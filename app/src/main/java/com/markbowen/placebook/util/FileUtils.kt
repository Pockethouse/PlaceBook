package com.markbowen.placebook.util

import android.content.Context
import java.io.File

object FileUtils {
    fun deleteFile(context: Context, filename: String) {
        val dir = context.filesDir
        val file = File(dir, filename)
        file.delete()
    }
}