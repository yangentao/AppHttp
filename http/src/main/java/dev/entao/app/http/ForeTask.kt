package dev.entao.app.http

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

object ForeTask {
    val handler: Handler by lazy { Handler(Looper.getMainLooper()) }

    fun post(block: () -> Unit) {
        handler.post(block)
    }
}

object BackTask {
    val thread: HandlerThread = HandlerThread("backtask", Thread.NORM_PRIORITY - 1).apply {
        start()
    }
    val handler: Handler by lazy { Handler(thread.looper) }

    fun post(block: () -> Unit) {
        handler.post(block)
    }
}