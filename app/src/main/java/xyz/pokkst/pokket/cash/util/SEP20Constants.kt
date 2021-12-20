package xyz.pokkst.pokket.cash.util

import org.web3j.contracts.eip20.generated.ERC20
import org.web3j.tx.gas.DefaultGasProvider
import xyz.pokkst.pokket.cash.interactors.WalletInteractor
import java.math.BigInteger

data class SEP20(val contractAddress: String, val symbol: String, val name: String)
data class SEP20Balance(val sep20: SEP20, val balance: BigInteger, val lastUpdatedTimeMs: Long)

class SEP20Constants {
    companion object {
        private val walletInteractor = WalletInteractor.getInstance()
        val FLEXUSD = SEP20("0x7b2B3C5308ab5b2a1d9a94d20D35CCDf61e05b72", "FLEXUSD", "FlexUSD")
        val EBEN = SEP20("0x77CB87b57F54667978Eb1B199b28a0db8C8E1c0B", "EBEN", "Green Ben")
        val MIST = SEP20("0x5fA664f69c2A4A3ec94FaC3cBf7049BD9CA73129", "MIST", "MIST")
        val SPICE = SEP20("0xe11829a7d5d8806bb36e118461a1012588fafd89", "SPICE", "SPICE")
        val supportedTokens = listOf(FLEXUSD, EBEN, MIST, SPICE)

        fun getToken(sep20: SEP20): ERC20 {
            return ERC20.load(sep20.contractAddress, walletInteractor.getSmartWallet(), walletInteractor.getCredentials(), DefaultGasProvider())
        }

        fun getTokenBalance(erc20: ERC20): BigInteger {
            return erc20.balanceOf(walletInteractor.getSmartAddress()).send()
        }
    }
}