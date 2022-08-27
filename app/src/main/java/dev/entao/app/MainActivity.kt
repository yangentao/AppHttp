package dev.entao.app

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import dev.entao.app.http.HttpConfig
import dev.entao.app.http.httpGet
import dev.entao.app.http.httpMultipart
import java.io.File

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        HttpConfig.allowDumpByDebugFlag(this)
        BackTask.post {
            test()
        }
    }
    fun test2() {
        val resp = httpMultipart(this, "https://entao.dev/") {
            "user" arg "tom"
            file("fileA", File("..."))
        }
        val s = resp.valueText
        Log.d("http", s ?: "null")
    }
    fun test() {
        val url = "https://entao.dev/"
        val r = httpGet(url) {
            "user" arg "tom"
        }
        val s = r.valueText
        Log.d("http", s ?: "null")
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