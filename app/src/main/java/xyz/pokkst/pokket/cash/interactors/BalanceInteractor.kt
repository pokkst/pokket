package xyz.pokkst.pokket.cash.interactors

import org.bitcoinj.wallet.Wallet
import org.web3j.protocol.core.DefaultBlockParameterName
import xyz.pokkst.pokket.cash.util.BalanceFormatter
import java.math.BigDecimal
import java.math.BigInteger

class BalanceInteractor {
    private val walletInteractor = WalletInteractor.getInstance()
    private var lastUpdateTimeMs: Long = 0
    private var cachedSmartBalance: BigInteger = BigInteger.ZERO

    fun getBitcoinBalance(): BigDecimal {
        val balance = walletInteractor.getBitcoinWallet()?.getBalance(Wallet.BalanceType.ESTIMATED)
        return if (balance == null || balance.isZero)
            BigDecimal.ZERO
        else
            balance.toBtc()
    }

    fun getSmartBalanceRaw(): BigInteger {
        val currentTimeMs = System.currentTimeMillis()
        val updateBalance = updateSmartBalance(currentTimeMs)
        if (updateBalance) {
            if(walletInteractor.getSmartWallet() == null || walletInteractor.getCredentials() == null) return BigInteger.ZERO
            val sbchAddress = walletInteractor.getSmartAddress()
            val sbchWeiBalance =
                walletInteractor.getSmartWallet()
                    ?.ethGetBalance(sbchAddress, DefaultBlockParameterName.LATEST)
                    ?.send()?.balance
            cachedSmartBalance = sbchWeiBalance ?: BigInteger.ZERO
        }

        return cachedSmartBalance
    }

    fun getSmartBalance(): BigDecimal {
        val rawBalance = getSmartBalanceRaw()
        val sbchBalance = rawBalance.toLong().let { BalanceFormatter.toEtherBalance(it) }
        return sbchBalance ?: BigDecimal.ZERO
    }

    private fun updateSmartBalance(currentTimeMs: Long): Boolean {
        val difference = currentTimeMs - lastUpdateTimeMs
        return difference >= 10 * 1000 // 10 seconds
    }

    companion object {
        private var instance: BalanceInteractor? = null
        fun getInstance(): BalanceInteractor {
            if (instance == null) {
                instance = BalanceInteractor()
            }
            return instance as BalanceInteractor
        }
    }
}