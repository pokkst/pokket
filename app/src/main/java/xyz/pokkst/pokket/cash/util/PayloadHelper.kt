package xyz.pokkst.pokket.cash.util

import android.os.Build
import com.google.gson.Gson
import org.bitcoinj.core.flipstarter.FlipstarterInvoicePayload
import org.bitcoinj.utils.MultisigPayload
import org.bouncycastle.util.encoders.Base64
import java.nio.charset.StandardCharsets

class PayloadHelper {
    companion object {
        fun isFlipstarterPayload(base64Payload: String): Boolean {
            return try {
                val payload = decodeFlipstarterPayload(base64Payload)
                payload != null
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        fun decodeFlipstarterPayload(base64Payload: String): FlipstarterInvoicePayload? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                return try {
                    val payloadBytes: ByteArray =
                        Base64.decode(base64Payload)
                    val invoiceJson =
                        String(payloadBytes, StandardCharsets.UTF_16LE)
                    Gson().fromJson(
                        invoiceJson,
                        FlipstarterInvoicePayload::class.java
                    )
                } catch (e: java.lang.Exception) {
                    null
                }
            } else {
                return null
            }
        }

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
                Gson().fromJson(
                    CompressionHelper.decompress(Base64.decode(base64Payload)),
                    MultisigPayload::class.java
                )
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