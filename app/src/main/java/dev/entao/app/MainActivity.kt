package dev.entao.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.entao.app.http.BackTask
import dev.entao.app.http.HttpGet
import dev.entao.app.http.logd
import dev.entao.app.http.headerKeyFormat

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BackTask.post {
//            test()
        }
        logd("agent".headerKeyFormat)
        logd("userAgent".headerKeyFormat)

        val a = System.currentTimeMillis()
        logd(a.toString(16) )
    }

    fun test() {
        val url = "https://entao.dev/"
        val r = HttpGet(url).request()
        val s = r.responseText()
        logd(s)
    }
}
