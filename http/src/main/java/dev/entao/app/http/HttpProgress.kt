package dev.entao.app.http

interface HttpProgress {
    fun onHttpStart(total: Int)

    fun onHttpProgress(current: Int, total: Int, percent: Int)

    fun onHttpFinish()
}
