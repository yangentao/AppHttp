package dev.entao.app.http

import java.io.Closeable

fun logd(vararg values: Any?) {

}
fun loge(vararg values: Any?) {

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