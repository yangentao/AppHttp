@file:Suppress("unused")

package dev.entao.app.http

import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File

/**
 * Created by entaoyang@163.com on 2016-11-12.
 */

//file, key, filename, mime, NOT NULL
class FileParam(
    val key: String,
    val file: Uri,
    var filename: String = file.lastPathSegment ?: "",
    var mime: String = "application/octet-stream"
) {


    var progress: HttpProgress? = null

    constructor(key: String, file: File) : this(
        key,
        Uri.fromFile(file),
        file.name,
        mimeOfFile(file)
    )

    fun mime(mime: String?): FileParam {
        if (mime != null) {
            this.mime = mime
        }
        return this
    }

    fun fileName(filename: String?): FileParam {
        if (filename != null) {
            this.filename = filename
        }
        return this
    }

    fun progress(progress: HttpProgress?): FileParam {
        this.progress = progress
        return this
    }

    override fun toString(): String {
        return "key=$key, filename=$filename, mime=$mime, file=${file}"
    }
}

fun mimeOfFile(file: File): String {
    val ext = file.extension
    if (ext != file.name && ext.isNotEmpty()) {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: "application/octet-stream"
    }
    return "application/octet-stream"
}
