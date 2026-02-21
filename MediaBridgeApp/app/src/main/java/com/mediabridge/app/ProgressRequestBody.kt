package com.mediabridge.app

import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.BufferedSink
import java.io.File

class ProgressRequestBody(
    private val file: File,
    private val contentType: String,
    private val listener: (Int) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? =
        contentType.toMediaTypeOrNull()

    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: BufferedSink) {

        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        file.inputStream().use { inputStream ->

            var uploaded = 0L
            val total = contentLength()

            while (true) {

                val read = inputStream.read(buffer)
                if (read == -1) break

                uploaded += read
                sink.write(buffer, 0, read)

                val progress = (100 * uploaded / total).toInt()
                listener(progress)
            }
        }
    }
}
