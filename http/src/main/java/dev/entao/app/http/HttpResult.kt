package dev.entao.app.http
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.concurrent.TimeoutException

/**
 * Created by entaoyang@163.com on 16/4/29.
 */
class HttpResult(val url: String) {
    var response: ByteArray? = null//如果Http.request参数给定了文件参数, 则,response是null
    var responseCode: Int = 0//200
    var responseMsg: String? = null//OK
    var contentType: String? = null
    var contentLength: Int = 0//如果是gzip格式, 这个值!=response.length
    var headerMap: Map<String, List<String>>? = null
    var exception: Exception? = null

    var needDecode: Boolean = false
    val OK: Boolean get() = responseCode in 200..299
    val errorMsg: String?
        get() {
            return when (val ex = exception) {
                null -> httpMsgByCode(responseCode)
                is NoRouteToHostException -> "网络不可达"
                is TimeoutException -> "请求超时"
                is SocketTimeoutException -> "请求超时"
                is SocketException -> "网络错误"
                is FileNotFoundException -> "请求的网址不存在"
                else -> ex.localizedMessage
            }
        }

    //Content-Type: text/html; charset=GBK
    val contentCharset: Charset?
        get() {
            val ct = contentType ?: return null
            val ls: List<String> = ct.split(";".toRegex()).dropLastWhile { it.isEmpty() }
            for (item in ls) {
                val ss = item.trim()
                if (ss.startsWith("charset")) {
                    val charset = ss.substringAfterLast('=', "").trim()
                    if (charset.length >= 2) {
                        return Charset.forName(charset)
                    }
                }
            }
            return null
        }

    fun responseText(charset: Charset = Charsets.UTF_8): String? {
        val r = this.response ?: return null
        val ch = contentCharset ?: charset
        var s = String(r, ch)
        if (needDecode) {
            s = URLDecoder.decode(s, ch.name())
        }
        return s
    }

    fun dump() {
        logd(">>Response:", this.url)
        logd("  >>status:", responseCode, responseMsg ?: "")
        val map = this.headerMap
        if (map != null) {
            for ((k, v) in map) {
                if (v.size == 1) {
                    logd("  >>head:", k, "=", v.first())
                } else {
                    logd("  >>head:", k, "=", "[" + v.joinToString(",") + "]")
                }
            }
        }
        if (this.exception != null) {
            logd("  >>Exception:", this.exception?.localizedMessage)
        }
        if (allowDump(this.contentType)) {
            logd("  >>body:", this.responseText())
        }

    }

    fun needDecode(): HttpResult {
        this.needDecode = true
        return this
    }

    fun str(charset: Charset): String? {
        if (OK) {
            return this.responseText(charset)
        }
        return null
    }

    fun strISO8859_1(): String? = str(Charsets.ISO_8859_1)
    fun strUtf8(): String? = str(Charsets.UTF_8)

    fun <T> textTo(block: (String) -> T): T? {
        if (OK) {
            val s = strUtf8()
            if (s != null && s.isNotEmpty()) {
                try {
                    return block(s)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }


    fun jsonObject(): JSONObject? {
        if (OK) {
            val s = strUtf8()
            if (s != null && s.isNotEmpty()) {
                try {
                    return JSONObject(s)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            }
        }
        return null
    }

    fun jsonArray(): JSONArray? {
        if (OK) {
            val s = strUtf8()
            if (s != null && s.isNotEmpty()) {
                try {
                    return JSONArray(s)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            }
        }
        return null
    }

    fun bytes(): ByteArray? {
        if (OK) {
            return response
        }
        return null
    }

    fun saveTo(file: File): Boolean {
        val data = this.response ?: return false
        if (OK) {
            val dir = file.parentFile
            if (dir != null) {
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        loge("创建目录失败")
                        return false
                    }
                }
            }
            FileOutputStream(file).closeAfter {
                it.write(data)
                it.flush()
            }
        }
        return false
    }

}