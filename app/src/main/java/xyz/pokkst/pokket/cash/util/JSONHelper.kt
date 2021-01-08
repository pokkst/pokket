package xyz.pokkst.pokket.cash.util

import org.json.JSONObject
import java.io.*
import java.net.URL
import java.nio.charset.Charset

class JSONHelper {

    fun getJsonObject(url: String): JSONObject? {
        var `is`: InputStream? = null
        try {
            `is` = URL(url).openConnection().getInputStream()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        return try {
            val rd = BufferedReader(InputStreamReader(`is`, Charset.forName("UTF-8")))
            val jsonText = readJSONFile(rd)
            JSONObject(jsonText)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                `is`?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    companion object {

        @Throws(IOException::class)
        fun readJSONFile(rd: Reader): String {
            val sb = StringBuilder()
            while (true) {
                val cp = rd.read()

                if (cp != -1)
                    sb.append(cp.toChar())
                else
                    break
            }
            return sb.toString()
        }
    }
}
