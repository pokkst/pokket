package xyz.pokkst.pokket.cash.interactors

import org.bitcoinj.core.Address
import org.bitcoinj.kits.BIP47AppKit
import org.bitcoinj.kits.MultisigAppKit
import org.bitcoinj.wallet.Wallet
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import xyz.pokkst.pokket.cash.service.WalletService

class WalletInteractor {
    fun getWalletKit(): BIP47AppKit? {
        return WalletService.walletKit
    }

    fun getMultisigKit(): MultisigAppKit? {
        return WalletService.multisigWalletKit
    }

    fun getBitcoinWallet(): Wallet? {
        return WalletService.wallet
    }

    fun getSmartWallet(): Web3j? {
        return WalletService.web3
    }

    fun getCredentials(): Credentials? {
        return WalletService.credentials
    }

    fun getBitcoinAddress(): Address? {
        return getBitcoinWallet()?.currentReceiveAddress()?.toCash()
    }

    fun getFreshBitcoinAddress(): Address? {
        return getBitcoinWallet()?.freshReceiveAddress()?.toCash()
    }

    fun getSmartAddress(): String {
        return WalletService.getInstance().getSmartBchAddress().toString()
    }

    companion object {
        private var instance: WalletInteractor? = null
        fun getInstance(): WalletInteractor {
            if (instance == null) {
                instance = WalletInteractor()
            }
            return instance as WalletInteractor
        }
    }
}