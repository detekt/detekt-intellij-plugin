package io.gitlab.arturbosch.detekt.idea.util

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.BufferedSink
import okio.buffer
import okio.sink
import java.io.File


interface DownloadFile {

    fun download(url: String): File?
}

class DownloadFileImpl : DownloadFile {

    override fun download(url: String): File? {
        val fileName = getUrlFileName(url) ?: ("unknownfile_" + System.currentTimeMillis())
        val file = File(getConfigPath(), fileName)
        if (file.exists()) {
            return file
        }

        val okHttpClient = OkHttpClient.Builder().build()
        val req = Request.Builder().url(url).build()
        return runCatching {
            val response = okHttpClient.newCall(req).execute()
            println("WARNING download file($fileName) successful!")
            return file.apply {
                createNewFile()

                val sink: BufferedSink = this.sink().buffer()
                sink.writeAll(response.body!!.source())
                sink.close()
            }
        }.onFailure {
            println("download $url failed. message: $it")
        }.getOrNull()
    }

    private fun getConfigPath(): File {
        val path = PluginManagerCore.getPlugin(PluginId.getId("detekt"))!!.pluginPath
        return File("$path/config").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private fun getUrlFileName(url: String): String? {
        var filename: String? = null
        val strings = url.split("/").toTypedArray()
        for (string in strings) {
            if (string.contains("?")) {
                val endIndex = string.indexOf("?")
                if (endIndex != -1) {
                    filename = string.substring(0, endIndex)
                    return filename
                }
            }
        }
        if (strings.isNotEmpty()) {
            filename = strings[strings.size - 1]
        }
        return filename
    }

}