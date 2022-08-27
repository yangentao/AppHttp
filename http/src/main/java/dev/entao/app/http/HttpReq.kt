@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package dev.entao.app.http

import android.os.NetworkOnMainThreadException
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.ProtocolException
import java.net.URL
import java.util.zip.GZIPInputStream


class HttpGet(url: String) : HttpReq(url, "GET") {
    override fun onSend(connection: HttpURLConnection) {
    }
}

class HttpPost(url: String) : HttpReq(url, "POST") {

    init {
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

class HttpRaw(url: String) : HttpReq(url, "POST") {
    private lateinit var rawData: ByteArray

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


internal const val UTF8 = "UTF-8"
internal val charsetUTF8 = Charsets.UTF_8


abstract class HttpReq(val url: String, private val method: String = "GET") {
    val headers: HttpHeaders = HttpHeaders()
    val allArgs = LinkedHashMap<String, String>()

    var timeoutConnect = 20000
    var timeoutRead = 20000

    var saveToFile: File? = null
    var progress: Progress? = null

    var dumpReq: Boolean = true
    var dumpResp: Boolean = true


    init {
        headers.userAgent = "Android AppHttp"
        headers.accept = "application/json,text/plain,text/html,*/*"
        headers.acceptCharset = "UTF-8,*"
        headers.connection = "close"
    }


    infix fun String.arg(v: String) {
        arg(this, v)
    }

    infix fun String.arg(v: Number) {
        arg(this, v)
    }

    infix fun String.arg(v: Boolean) {
        arg(this, v)
    }

    fun arg(key: String, value: Any): HttpReq {
        allArgs[key] = value.toString()
        return this
    }


    fun args(vararg args: Pair<String, String>): HttpReq {
        for ((k, v) in args) {
            allArgs[k] = v
        }
        return this
    }

    fun args(map: Map<String, String>): HttpReq {
        allArgs.putAll(map)
        return this
    }

    protected fun buildArgs(): String {
        return allArgs.map {
            it.key.urlEncoded + "=" + it.value.urlEncoded
        }.joinToString("&")
    }

    @Throws(MalformedURLException::class)
    fun buildGetUrl(): String {
        val sArgs = buildArgs()
        if (sArgs.isEmpty()) return url
        val sb = StringBuilder(url.length + sArgs.length + 8)
        sb.append(url)
        if ('?' !in sb) {
            sb.append('?')
        }
        if (sb.last() != '?') {
            sb.append('&')
        }
        sb.append(sArgs)
        return sb.toString()
    }

    open fun dumpReq() {
        if (!dumpReq) {
            return
        }
        logd("Http Request:", url)
        for ((k, v) in headers.allMap) {
            logd("--head:", k, "=", v)
        }
        for ((k, v) in allArgs) {
            logd("--arg:", k, "=", v)
        }
    }

    @Throws(ProtocolException::class, UnsupportedEncodingException::class)
    protected open fun preConnect(connection: HttpURLConnection) {
        HttpURLConnection.setFollowRedirects(true)
        connection.doOutput = method != "GET"
        connection.doInput = true
        connection.connectTimeout = timeoutConnect
        connection.readTimeout = timeoutRead
        connection.requestMethod = method
        connection.useCaches = false

        for (e in headers.allMap.entries) {
            connection.setRequestProperty(e.key, e.value)
        }
    }

    @Throws(IOException::class)
    protected fun write(os: OutputStream, vararg arr: String) {
        for (s in arr) {
            os.write(s.toByteArray(charsetUTF8))
        }
    }

    protected fun writeln(os: OutputStream, vararg arr: String) {
        for (s in arr) {
            os.write(s.toByteArray(charsetUTF8))
        }
        os.write("\r\n".toByteArray(charsetUTF8))
    }

    @Throws(IOException::class)
    private fun onResponse(connection: HttpURLConnection): HttpResult {
        val result = HttpResult(this.url).apply {
            responseCode = connection.responseCode
            responseMsg = connection.responseMessage
            contentType = connection.contentType
            headerMap = connection.headerFields
            contentLength = connection.contentLength
        }
        val total = result.contentLength
        try {
            val saveFile = this.saveToFile
            saveFile?.also {
                val dir = it.parentFile
                if (dir != null) {
                    if (!dir.exists()) {
                        if (!dir.mkdirs()) {
                            loge("创建目录失败")
                            throw IOException("创建目录失败!")
                        }
                    }
                }
            }
            val os: OutputStream = if (saveFile != null) {
                FileOutputStream(saveFile)
            } else {
                ByteArrayOutputStream(if (total > 0) total else 64)
            }
            //TODO  4xx, 5xx时直接返回
            var input = connection.inputStream
            val mayGzip = connection.contentEncoding
            if (mayGzip != null && mayGzip.contains("gzip")) {
                input = GZIPInputStream(input)
            }
            input.copyToProgress(os, progress, total)
            input.closeSafe()
            os.flush()
            os.closeSafe()
            if (os is ByteArrayOutputStream) {
                result.response = os.toByteArray()
            }
        } catch (ex: Exception) {
            result.exception = ex
            ex.printStackTrace()
        }
        return result
    }

    @Throws(IOException::class)
    protected abstract fun onSend(connection: HttpURLConnection)

    fun request(): HttpResult {
        if (dumpReq) {
            dumpReq()
        }
        var connection: HttpURLConnection? = null
        try {
            val con = if (this is HttpGet || this is HttpRaw) {
                URL(buildGetUrl()).openConnection() as HttpURLConnection
            } else {
                URL(url).openConnection() as HttpURLConnection
            }
            connection = con
            preConnect(con)
            con.connect()
            onSend(con)
            val r = onResponse(con)
            if (dumpResp) {
                r.dump()
            }
            return r
        } catch (ex: Exception) {
            if (ex is NetworkOnMainThreadException) {
                loge("主线程中使用了网络请求!")
            }
            ex.printStackTrace()
            loge(ex)
            val result = HttpResult(this.url)
            result.exception = ex
            return result
        } finally {
            connection?.disconnect()
        }
    }

    fun download(saveto: File, progress: Progress?): HttpResult {
        this.saveToFile = saveto
        this.progress = progress
        return request()
    }


}

