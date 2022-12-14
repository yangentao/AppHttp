@file:Suppress("MemberVisibilityCanBePrivate", "unused", "PrivatePropertyName")

package dev.entao.app.http


import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log

/**
 * Created by entaoyang@163.com on 2015-11-20.
 */

object HttpConfig {
    var allowDump: Boolean = true
    var debugPrinter: (String) -> Unit = { Log.d("http", it) }
    var errorPrinter: (String) -> Unit = { Log.e("http", it) }

    fun allowDumpByDebugFlag(context: Context) {
        allowDump = 0 != (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)
    }
}

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


