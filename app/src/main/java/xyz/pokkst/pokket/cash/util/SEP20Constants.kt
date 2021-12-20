package xyz.pokkst.pokket.cash.util

import org.web3j.contracts.eip20.generated.ERC20
import org.web3j.tx.gas.DefaultGasProvider
import xyz.pokkst.pokket.cash.interactors.WalletInteractor

class SEP20Constants {
    companion object {
        private val walletInteractor = WalletInteractor.getInstance()
        const val FLEXUSD_ADDRESS = "0x7b2B3C5308ab5b2a1d9a94d20D35CCDf61e05b72"
        const val EBEN_ADDRESS = "0x77CB87b57F54667978Eb1B199b28a0db8C8E1c0B"
        const val MIST_ADDRESS = "0x5fA664f69c2A4A3ec94FaC3cBf7049BD9CA73129"
        const val SPICE_ADDRESS = "0xe11829a7d5d8806bb36e118461a1012588fafd89"

        fun getToken(contractAddress: String): ERC20 {
            return ERC20.load(contractAddress, walletInteractor.getSmartWallet(), walletInteractor.getCredentials(), DefaultGasProvider())
        }
    }
}