package dev.entao.app.http

import android.content.Context
import android.content.pm.ApplicationInfo
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.net.URLDecoder
import java.net.URLEncoder

private const val PROGRESS_DELAY = 50

fun logd(vararg values: Any?) {

}

fun loge(vararg values: Any?) {

}

val Context.debug: Boolean get() = 0 != (this.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)


val String.urlEncoded: String
    get() {
        return URLEncoder.encode(this, Charsets.UTF_8.name())
    }
val String.urlDecoded: String
    get() {
        return URLDecoder.decode(this, Charsets.UTF_8.name())
    }

fun <T : Closeable> T.closeSafe() {
    try {
        this.close()
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

inline fun <T : Closeable> T.closeAfter(block: (T) -> Unit) {
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


fun InputStream.copyToProgress(os: OutputStream, progress: Progress?, total: Int = this.available()) {
    try {
        progress?.also {
            ForeTask.post {
                it.onProgressStart(total)
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
                        p.onProgress(recv, total, if (total > 0) recv * 100 / total else 0)
                    }
                }
            }
            n = this.read(buf)
        }
        os.flush()
        progress?.also {
            ForeTask.post {
                it.onProgress(recv, total, if (total > 0) recv * 100 / total else 0)
            }
        }
    } finally {
        progress?.also { p ->
            ForeTask.post {
                p.onProgressFinish()
            }
        }
    }
}