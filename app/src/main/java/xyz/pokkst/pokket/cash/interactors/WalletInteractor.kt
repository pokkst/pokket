package xyz.pokkst.pokket.cash.interactors

import org.bitcoinj.core.Address
import org.bitcoinj.kits.BIP47AppKit
import org.bitcoinj.kits.MultisigAppKit
import org.bitcoinj.kits.WalletKitCore
import org.bitcoinj.wallet.Wallet
import org.web3j.protocol.Web3j
import xyz.pokkst.pokket.cash.wallet.WalletManager

class WalletInteractor {
    fun getWalletKit(): BIP47AppKit? {
        return WalletManager.walletKit
    }

    fun getMultisigKit(): MultisigAppKit? {
        return WalletManager.multisigWalletKit
    }

    fun getBitcoinWallet(): Wallet? {
        return WalletManager.wallet
    }

    fun getSmartWallet(): Web3j? {
        return WalletManager.web3
    }

    fun getBitcoinAddress(): Address? {
        return getBitcoinWallet()?.currentReceiveAddress()
    }

    fun getSmartAddress(): String {
        return WalletManager.getSmartBchAddress().toString()
    }

    companion object {
        private var instance: WalletInteractor? = null
        fun getInstance(): WalletInteractor {
            if(instance == null) {
                instance = WalletInteractor()
            }
            return instance as WalletInteractor
        }
    }
}