package xyz.pokkst.pokket.util

import com.google.gson.Gson
import org.bitcoinj.utils.MultisigPayload
import org.bouncycastle.util.encoders.Base64

class PayloadHelper {
    companion object {
        fun isMultisigPayload(base64Payload: String): Boolean {
            return try {
                val json = decodeMultisigPayload(base64Payload)
                Gson().fromJson(json, MultisigPayload::class.java)
                json != null
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                false
            }
        }

        fun decodeMultisigPayload(base64Payload: String): String? {
            return try {
                CompressionHelper.decompress(Base64.decode(base64Payload))
            } catch (e: java.lang.Exception) {
                null
            }
        }

        fun encodeMultisigPayload(json: String): String? {
            return try {
                val compressedBytes = CompressionHelper.compress(json)
                return Base64.toBase64String(compressedBytes)
            } catch (e: java.lang.Exception) {
                null
            }
        }
    }
}