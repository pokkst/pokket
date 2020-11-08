package xyz.pokkst.pokket.util

import org.bitcoinj.core.Coin

class PaymentContent(val addressOrPayload: String?, val amount: Coin?, val paymentType: PaymentType)

enum class PaymentType {
    CASH_ACCOUNT,
    ADDRESS,
    PAYMENT_CODE,
    SLP_ADDRESS,
    MULTISIG_PAYLOAD,
    BIP70
}