package dev.entao.app.http

import android.os.Handler
import android.os.Looper

object ForeTask {
    val handler: Handler by lazy { Handler(Looper.getMainLooper()) }

    fun post(block: () -> Unit) {
        handler.post(block)
    }
}