@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package dev.entao.app.http

import android.os.NetworkOnMainThreadException
import android.util.Base64
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.ProtocolException
import java.net.URL
import java.util.HashMap
import java.util.zip.GZIPInputStream
import kotlin.reflect.KProperty

internal const val UTF8 = "UTF-8"
internal val charsetUTF8 = Charsets.UTF_8


internal object HeaderDelegate {
    operator fun setValue(thisRef: HttpHeaders, property: KProperty<*>, value: String?) {
        if (value == null || value.isEmpty()) {
            thisRef.allMap.remove(property.headerName)
        } else {
            thisRef.allMap[property.headerName] = value
        }
    }

    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef: HttpHeaders, property: KProperty<*>): String? {
        return thisRef.allMap[property.headerName]
    }
}

class HttpHeaders {
    val allMap = HashMap<String, String>()

    var connection: String? by HeaderDelegate
    var userAgent: String? by HeaderDelegate
    var accept: String? by HeaderDelegate
    var acceptCharset: String? by HeaderDelegate
    var acceptLanguage: String? by HeaderDelegate
    var authorization: String? by HeaderDelegate
    var contentType: String? by HeaderDelegate


    operator fun get(key: String): String? {
        return allMap[key]
    }

    operator fun set(key: String, value: String?) {
        if (value == null || value.isEmpty()) {
            allMap.remove(key)
        } else {
            allMap[key] = value
        }
    }

    fun remove(key: String) {
        allMap.remove(key)
    }

    fun add(key: String, value: String) {
        allMap[key] = value
    }

    fun add(vararg ps: Pair<String, String>) {
        for (p in ps) {
            allMap[p.first] = p.second
        }
    }
}

abstract class HttpReq(val url: String) {
    var timeoutConnect = 20000
    var timeoutRead = 20000

    protected var method: String = "GET"
    val headers: HttpHeaders = HttpHeaders()
    private val headerMap: HashMap<String, String> get() = headers.allMap
    val argMap = HashMap<String, String>()

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


    fun authBasic(user: String, pwd: String): HttpReq {
        headers.authorization = "Basic ${Base64.encodeToString("$user:$pwd".toByteArray(charsetUTF8), Base64.NO_WRAP)}"
        return this
    }

    fun authBearer(token: String): HttpReq {
        headers.authorization = "Bearer $token"
        return this
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
        argMap[key] = value.toString()
        return this
    }


    fun args(vararg args: Pair<String, String>): HttpReq {
        for ((k, v) in args) {
            argMap[k] = v
        }
        return this
    }

    fun args(map: Map<String, String>): HttpReq {
        argMap.putAll(map)
        return this
    }


    //[from, to]
    fun range(from: Int, to: Int): HttpReq {
        headerMap["Range"] = "bytes=$from-$to"
        return this
    }

    fun range(from: Int): HttpReq {
        headerMap["Range"] = "bytes=$from-"
        return this
    }

    protected fun buildArgs(): String {
        return argMap.map {
            it.key.urlEncoded + "=" + it.value.urlEncoded
        }.joinToString("&")
    }

    @Throws(MalformedURLException::class)
    fun buildGetUrl(): String {
        val sArgs = buildArgs()
        var u: String = url
        if (sArgs.isNotEmpty()) {
            val n = u.indexOf('?')
            if (n < 0) {
                u += "?"
            }
            if ('?' != u[u.length - 1]) {
                u += "&"
            }
            u += sArgs
        }
        return u
    }

    open fun dumpReq() {
        if (!dumpReq) {
            return
        }
        logd("Http Request:", url)
        for ((k, v) in headerMap) {
            logd("--head:", k, "=", v)
        }
        for ((k, v) in argMap) {
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

        for (e in headerMap.entries) {
            connection.setRequestProperty(e.key, e.value)
        }
    }

    @Throws(IOException::class)
    protected fun write(os: OutputStream, vararg arr: String) {
        for (s in arr) {
            os.write(s.toByteArray(charsetUTF8))
        }
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
            val os: OutputStream = if (this.saveToFile != null) {
                val dir = this.saveToFile!!.parentFile
                if (dir != null) {
                    if (!dir.exists()) {
                        if (!dir.mkdirs()) {
                            loge("创建目录失败")
                            throw IOException("创建目录失败!")
                        }
                    }
                }
                FileOutputStream(saveToFile!!)
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
        var connection: HttpURLConnection? = null
        try {
            logd("request------------------------")
            if (dumpReq) {
                logd("request----------dumpReq--------------")
                dumpReq()
            }
            connection = if (this is HttpGet || this is HttpRaw) {
                URL(buildGetUrl()).openConnection() as HttpURLConnection
            } else {
                URL(url).openConnection() as HttpURLConnection
            }

            preConnect(connection)
            connection.connect()
            onSend(connection)
            val r = onResponse(connection)
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

