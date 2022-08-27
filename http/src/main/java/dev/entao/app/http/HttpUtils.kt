@file:Suppress("RedundantExplicitType")

package dev.entao.app.http

import android.os.Handler
import android.os.Looper
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URLEncoder

internal object ForeTask {
    private val handler: Handler by lazy { Handler(Looper.getMainLooper()) }

    fun post(block: () -> Unit) {
        handler.post(block)
    }
}


//accept => Accept
//userAgent => User-Agent
internal val String.headerKeyFormat: String
    get() {
        val sb = StringBuilder()
        for (ch: Char in this) {
            if (sb.isEmpty()) {
                sb.append(ch.uppercaseChar())
            } else if (ch.isUpperCase()) {
                sb.append('-').append(ch)
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

internal class SizeStream : OutputStream() {
    var size = 0
        private set

    @Throws(IOException::class)
    override fun write(oneByte: Int) {
        ++size
    }

    fun incSize(size: Int) {
        this.size += size
    }

}

internal fun allowDump(ct: String?): Boolean {
    val a = ct?.lowercase() ?: return false
    return "json" in a || "xml" in a || "html" in a || "text" in a
}

private const val PROGRESS_DELAY = 50

internal fun logd(vararg values: Any?) {
    val s = values.joinToString(" ") { it?.toString() ?: "null" }
    HttpConfig.debugPrinter(s)
}

internal fun loge(vararg values: Any?) {
    val s = values.joinToString(" ") { it?.toString() ?: "null" }
    HttpConfig.errorPrinter(s)
}


internal val String.urlEncoded: String
    get() {
        return URLEncoder.encode(this, Charsets.UTF_8.name())
    }


internal fun <T : Closeable> T.closeSafe() {
    try {
        this.close()
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

internal inline fun <T : Closeable> T.closeAfter(block: (T) -> Unit) {
    try {
        block(this)
    } catch (e: Exception) {
        loge(e)
        e.printStackTrace()
    } finally {
        try {
            this.close()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}


internal fun InputStream.copyToProgress(os: OutputStream, progress: HttpProgress?, total: Int = this.available()) {
    try {
        progress?.also {
            ForeTask.post {
                it.onHttpStart(total)
            }
        }

        val buf = ByteArray(8192)
        var pre: Long = 0L
        var recv = 0

        var n = this.read(buf)
        while (n >= 0) {
            os.write(buf, 0, n)
            recv += n
            progress?.also { p ->
                val curr = System.currentTimeMillis()
                if (curr - pre > PROGRESS_DELAY) {
                    pre = curr
                    ForeTask.post {
                        p.onHttpProgress(recv, total, if (total > 0) recv * 100 / total else 0)
                    }
                }
            }
            n = this.read(buf)
        }
        os.flush()
        progress?.also {
            ForeTask.post {
                it.onHttpProgress(recv, total, if (total > 0) recv * 100 / total else 0)
            }
        }
    } finally {
        progress?.also { p ->
            ForeTask.post {
                p.onHttpFinish()
            }
        }
    }
}