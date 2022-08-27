package dev.entao.app.http

interface Progress {
    fun onProgressStart(total: Int)

    fun onProgress(current: Int, total: Int, percent: Int)

    fun onProgressFinish()
}
