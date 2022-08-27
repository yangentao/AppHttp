package dev.entao.app.http

import java.io.IOException
import java.io.OutputStream
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation


@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class HeaderName(val value: String)


val KProperty<*>.headerName: String get() = this.findAnnotation<HeaderName>()?.value ?: this.name.headerKeyFormat

//accept => Accept
//userAgent => User-Agent
val String.headerKeyFormat: String
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

class SizeStream : OutputStream() {
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

fun allowDump(ct: String?): Boolean {
    val a = ct?.lowercase() ?: return false
    return "json" in a || "xml" in a || "html" in a || "text" in a
}
