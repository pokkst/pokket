package xyz.pokkst.pokket.util

import com.google.gson.Gson
import org.bitcoinj.utils.MultisigPayload
import org.bouncycastle.util.encoders.Base64

class PayloadHelper {
    companion object {
        fun isMultisigPayload(base64Payload: String): Boolean {
            return try {
                val payload = decodeMultisigPayload(base64Payload)
                payload != null
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                false
            }
        }

        fun decodeMultisigPayload(base64Payload: String): MultisigPayload? {
            return try {
                Gson().fromJson(CompressionHelper.decompress(Base64.decode(base64Payload)), MultisigPayload::class.java)
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