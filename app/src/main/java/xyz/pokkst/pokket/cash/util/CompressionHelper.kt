package xyz.pokkst.pokket.cash.util

import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


object CompressionHelper {
    @Throws(IOException::class)
    fun compress(str: String?): ByteArray? {
        if (str == null || str.isEmpty()) {
            return null
        }
        val obj = ByteArrayOutputStream()
        val gzip = GZIPOutputStream(obj)
        gzip.write(str.toByteArray(charset("UTF-8")))
        gzip.flush()
        gzip.close()
        return obj.toByteArray()
    }

    @Throws(IOException::class)
    fun decompress(compressed: ByteArray?): String {
        val outStr = StringBuilder()
        if (compressed == null || compressed.isEmpty()) {
            return ""
        }
        if (isCompressed(compressed)) {
            val gis =
                GZIPInputStream(ByteArrayInputStream(compressed))
            val bufferedReader =
                BufferedReader(InputStreamReader(gis, "UTF-8"))
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                outStr.append(line)
            }
        } else {
            outStr.append(compressed)
        }
        return outStr.toString()
    }

    fun isCompressed(compressed: ByteArray): Boolean {
        return compressed[0] == GZIPInputStream.GZIP_MAGIC.toByte() && compressed[1] == (GZIPInputStream.GZIP_MAGIC shr 8).toByte()
    }
}