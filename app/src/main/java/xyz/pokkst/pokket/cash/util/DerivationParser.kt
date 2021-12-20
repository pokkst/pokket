package xyz.pokkst.pokket.cash.util

import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.HDPath
import org.bitcoinj.wallet.KeyChainGroupStructure

class DerivationParser {
    companion object {
        fun parse(derivationPath: String): KeyChainGroupStructure? {
            if(derivationPath.startsWith("m/")) {
                val mRemoved = derivationPath.replace("m/", "")
                val split = mRemoved.split("/")
                val nonPrimeExists = split.any { !it.contains("'") }
                if(nonPrimeExists) {
                    return null
                } else {
                    return try {
                        var hdPath: HDPath? = null
                        split.forEachIndexed { index, s ->
                            val childNumber = s.replace("'", "").toInt()
                            hdPath = if (index == 0) {
                                HDPath.M(ChildNumber(childNumber, true))
                            } else {
                                hdPath?.extend(ChildNumber(childNumber, true))
                            }
                        }
                        KeyChainGroupStructure { hdPath }
                    } catch (e: Exception) {
                        null
                    }
                }
            } else {
                return null
            }
        }
    }
}