package xyz.pokkst.pokket.cash.util

import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import xyz.pokkst.pokket.cash.service.WalletService
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.*

class UriHelper {
    companion object {
        private const val SMARTBCH_PREFIX = "0x"
        private var addressOrPayload: String? = null
        private var amount: Coin? = null

        private fun getQueryParams(url: String): Map<String, List<String>> {
            try {
                val params = HashMap<String, List<String>>()
                val urlParts =
                    url.split("\\?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (urlParts.size > 1) {
                    val query = urlParts[1]
                    for (param in query.split("&".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()) {
                        val pair =
                            param.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val key = URLDecoder.decode(pair[0], "UTF-8")
                        var value = ""
                        if (pair.size > 1) {
                            value = URLDecoder.decode(pair[1], "UTF-8")
                        }

                        var values: MutableList<String>? = params[key] as MutableList<String>?
                        if (values == null) {
                            values = ArrayList()
                            params[key] = values
                        }
                        values.add(value)
                    }
                }

                return params
            } catch (ex: UnsupportedEncodingException) {
                throw AssertionError(ex)
            }
        }

        private fun getQueryBaseAddress(url: String): String {
            val urlParts = url.split("\\?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return if (urlParts.size > 1) {
                urlParts[0]
            } else {
                url
            }
        }

        fun parse(uri: String): PaymentContent? {
            val params = WalletService.parameters
            val mappedVariables = getQueryParams(uri)
            val destinationWithoutPrefix =
                uri.replace("${params.cashAddrPrefix}:", "")
                    .replace("${params.simpleledgerPrefix}:", "")
            if (destinationWithoutPrefix.contains("http")) {
                addressOrPayload = if (mappedVariables["r"] != null) {
                    (mappedVariables["r"] ?: error(""))[0]
                } else {
                    uri
                }
                return PaymentContent(addressOrPayload, null, PaymentType.BIP70)
            } else {
                if (PayloadHelper.isMultisigPayload(destinationWithoutPrefix)) {
                    return PaymentContent(
                        destinationWithoutPrefix,
                        null,
                        PaymentType.MULTISIG_PAYLOAD
                    )
                } else if (PayloadHelper.isFlipstarterPayload(destinationWithoutPrefix)) {
                    return PaymentContent(
                        destinationWithoutPrefix,
                        null,
                        PaymentType.FLIPSTARTER_PAYLOAD
                    )
                } else {
                    amount = if (mappedVariables["amount"] != null) {
                        val amountVariable = (mappedVariables["amount"] ?: error(""))[0]
                        Coin.parseCoin(amountVariable)
                    } else {
                        null
                    }

                    //MetaMask adds an "ethereum:" prefix regardless of chain. They should really make it configurable.
                    addressOrPayload = uri.replace("ethereum:", "")

                    addressOrPayload = when {
                        uri.startsWith(SMARTBCH_PREFIX) -> getQueryBaseAddress(uri).toLowerCase()
                        uri.startsWith(params.cashAddrPrefix) -> getQueryBaseAddress(
                            uri
                        ).replace("${params.cashAddrPrefix}:", "")
                        uri.startsWith("cashacct") -> getQueryBaseAddress(uri).replace(
                            "cashacct:",
                            ""
                        )
                        else -> getQueryBaseAddress(uri)
                    }

                    if (addressOrPayload?.contains("#") == true) {
                        return PaymentContent(addressOrPayload, amount, PaymentType.CASH_ACCOUNT)
                    } else if (Address.isValidCashAddr(
                            WalletService.parameters,
                            addressOrPayload
                        ) || Address.isValidLegacyAddress(
                            WalletService.parameters,
                            addressOrPayload
                        )
                    ) {
                        val address = addressOrPayload
                        val addressModified =
                            address?.replace(WalletService.parameters.cashAddrPrefix + ":", "")
                        return if (addressModified == Constants.HOPCASH_BCH_INCOMING) {
                            PaymentContent(addressOrPayload, amount, PaymentType.HOP_TO_SBCH)
                        } else {
                            PaymentContent(addressOrPayload, amount, PaymentType.ADDRESS)
                        }
                    } else if (Address.isValidPaymentCode(addressOrPayload)) {
                        return PaymentContent(addressOrPayload, amount, PaymentType.PAYMENT_CODE)
                    } else if (addressOrPayload?.startsWith(SMARTBCH_PREFIX) == true) {
                        val address = addressOrPayload
                        return if (address.equals(
                                Constants.HOPCASH_SBCH_INCOMING,
                                ignoreCase = true
                            )
                        ) {
                            PaymentContent(addressOrPayload, amount, PaymentType.HOP_TO_BCH)
                        } else {
                            PaymentContent(addressOrPayload, amount, PaymentType.SMARTBCH_ADDRESS)
                        }
                    }
                }

            }

            return null
        }
    }

}