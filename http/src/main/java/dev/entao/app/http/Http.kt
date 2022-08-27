@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package dev.entao.app.http


import android.content.Context
import android.net.Uri
import android.os.NetworkOnMainThreadException
import android.util.Base64
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.ProtocolException
import java.net.URL
import java.util.*
import java.util.zip.GZIPInputStream

/**
 * Created by entaoyang@163.com on 2015-11-20.
 */
fun httpGet(url: String, block: HttpGet.() -> Unit): HttpResult {
    val h = HttpGet(url)
    h.block()
    return h.request()
}

fun httpPost(url: String, block: HttpPost.() -> Unit): HttpResult {
    val h = HttpPost(url)
    h.block()
    return h.request()
}

fun httpRaw(url: String, block: HttpRaw.() -> Unit): HttpResult {
    val h = HttpRaw(url)
    h.block()
    return h.request()
}

fun httpMultipart(context: Context, url: String, block: HttpMultipart.() -> Unit): HttpResult {
    val h = HttpMultipart(context, url)
    h.block()
    return h.request()
}

class HttpGet(url: String) : HttpReq(url) {
    init {
        method = "GET"
    }

    override fun onSend(connection: HttpURLConnection) {
    }
}

class HttpPost(url: String) : HttpReq(url) {

    init {
        method = "POST"
        headers.contentType = "application/x-www-form-urlencoded;charset=utf-8"
    }

    override fun onSend(connection: HttpURLConnection) {
        val os = connection.outputStream
        try {
            val s = buildArgs()
            if (s.isNotEmpty()) {
                write(os, s)
                if (dumpReq) {
                    logd("--body:", s)
                }
            }
            os.flush()
        } finally {
            os.closeSafe()
        }
    }
}

class HttpRaw(url: String) : HttpReq(url) {
    private lateinit var rawData: ByteArray

    init {
        method = "POST"
    }

    fun data(contentType: String, data: ByteArray): HttpRaw {
        headers.contentType = contentType
        this.rawData = data
        return this
    }


    fun json(json: String): HttpRaw {
        return data("application/json;charset=utf-8", json.toByteArray(charsetUTF8))
    }

    fun xml(xml: String): HttpRaw {
        return data("application/xml;charset=utf-8", xml.toByteArray(charsetUTF8))
    }

    override fun onSend(connection: HttpURLConnection) {
        val os = connection.outputStream
        try {
            os.write(rawData)
            if (dumpReq && allowDump(this.headers.contentType)) {
                logd("--body:", String(rawData, Charsets.UTF_8))
            }
            os.flush()
        } finally {
            os.closeSafe()
        }
    }
}

class HttpMultipart(val context: Context, url: String) : HttpReq(url) {
    private val BOUNDARY = UUID.randomUUID().toString()
    private val BOUNDARY_START = "--$BOUNDARY\r\n"
    private val BOUNDARY_END = "--$BOUNDARY--\r\n"

    private val fileList = ArrayList<FileParam>()

    init {
        method = "POST"
        headers.contentType = "multipart/form-data; boundary=$BOUNDARY"
    }

    fun file(fileParam: FileParam): HttpMultipart {
        fileList.add(fileParam)
        return this
    }

    fun file(key: String, file: Uri): HttpMultipart {
        val p = FileParam(key, file)
        return file(p)
    }

    fun file(key: String, file: Uri, block: FileParam.() -> Unit): HttpMultipart {
        val p = FileParam(key, file)
        p.block()
        return file(p)
    }


    fun file(key: String, file: File): HttpMultipart {
        val p = FileParam(key, file)
        return file(p)
    }


    fun file(key: String, file: File, block: FileParam.() -> Unit): HttpMultipart {
        val p = FileParam(key, file)
        p.block()
        return file(p)
    }

    override fun onSend(connection: HttpURLConnection) {
        val os = connection.outputStream
        try {
            sendMultipart(os)
            os.flush()
        } finally {
            os.closeSafe()
        }
    }

    override fun dumpReq() {
        super.dumpReq()
        for (fp in fileList) {
            logd("--file:", fp)
        }
    }

    override fun preConnect(connection: HttpURLConnection) {
        super.preConnect(connection)
        if (fileList.size > 0) {
            val os = SizeStream()
            sendMultipart(os)
            connection.setFixedLengthStreamingMode(os.size)
        }
    }

    @Throws(IOException::class)
    private fun sendMultipart(os: OutputStream) {

        if (argMap.size > 0) {
            for (e in argMap.entries) {
                write(os, BOUNDARY_START)
                write(os, "Content-Disposition: form-data; name=\"", e.key, "\"\r\n")
                write(os, "Content-Type:text/plain;charset=utf-8\r\n")
                write(os, "\r\n")
                write(os, e.value, "\r\n")
            }
        }
        if (fileList.size > 0) {
            for (fp in fileList) {
                val fis = context.contentResolver.openInputStream(fp.file) ?: continue
                write(os, BOUNDARY_START)
                write(os, "Content-Disposition:form-data;name=\"${fp.key}\";filename=\"${fp.filename}\"\r\n")
                write(os, "Content-Type:${fp.mime}\r\n")
                write(os, "Content-Transfer-Encoding: binary\r\n")
                write(os, "\r\n")

                val total = fis.available()
                if (os is SizeStream) {
                    os.incSize(total)
                    fis.closeSafe()
                } else {
                    fis.copyToProgress(os, fp.progress)
                    fis.closeSafe()

                }
                write(os, "\r\n")
            }
        }
        os.write(BOUNDARY_END.toByteArray())
    }
}
