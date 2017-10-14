/*
 * Copyright (C) 2017 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.shared.util

import android.graphics.Bitmap
import android.util.Log

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Util for working with files.
 */
object FileUtils {

    fun copy(src: File, dst: File) {
        try {
            val `in` = FileInputStream(src)
            val out = FileOutputStream(dst)
            `in`.writeToOutputAndCleanup(out)
        } catch (e: IOException) {
            Log.e("File", "error copying file", e)
        }
    }

    @Throws(IOException::class)
    fun copy(`in`: InputStream, dst: File) {
        val out = FileOutputStream(dst)
        `in`.writeToOutputAndCleanup(out)
    }

    fun writeBitmap(file: File, bmp: Bitmap) {
        var out: FileOutputStream? = null
        try {
            if (!file.exists()) {
                file.createNewFile()
            }

            out = FileOutputStream(file)
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                bmp.recycle()

                if (out != null) {
                    out.closeSilent()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

}