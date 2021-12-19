package xyz.pokkst.pokket.cash.util

import com.google.common.util.concurrent.ListenableFuture
import org.bitcoinj.protocols.payments.PaymentSession
import org.bitcoinj.protocols.payments.slp.SlpPaymentSession

class BIP70Helper {
    companion object {
        fun getBchPaymentSession(url: String?): PaymentSession {
            val future: ListenableFuture<PaymentSession> = PaymentSession.createFromUrl(url)
            return future.get()
        }
    }
}