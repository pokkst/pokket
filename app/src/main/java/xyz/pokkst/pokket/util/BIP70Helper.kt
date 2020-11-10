package xyz.pokkst.pokket.util

import com.google.common.util.concurrent.ListenableFuture
import org.bitcoinj.protocols.payments.PaymentSession
import org.bitcoinj.protocols.payments.slp.SlpPaymentSession

class BIP70Helper {
    companion object {
        fun getBchPaymentSession(url: String?): PaymentSession {
            val future: ListenableFuture<PaymentSession> = PaymentSession.createFromUrl(url)
            return future.get()
        }

        fun getSlpPaymentSession(url: String?): SlpPaymentSession {
            val future: ListenableFuture<SlpPaymentSession> = SlpPaymentSession.createFromUrl(url)
            return future.get()
        }

        fun getPaymentSessionType(url: String?): BIP70Type? {
            return try {
                getBchPaymentSession(url)
                BIP70Type.BCH
            } catch (e: Exception) {
                try {
                    getSlpPaymentSession(url)
                    BIP70Type.SLP
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}

enum class BIP70Type {
    BCH,
    SLP
}