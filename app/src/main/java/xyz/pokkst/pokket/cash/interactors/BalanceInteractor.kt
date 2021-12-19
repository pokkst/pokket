package xyz.pokkst.pokket.cash.interactors

import org.bitcoinj.wallet.Wallet
import org.web3j.protocol.core.DefaultBlockParameterName
import xyz.pokkst.pokket.cash.util.BalanceFormatter
import xyz.pokkst.pokket.cash.wallet.WalletManager
import java.math.BigDecimal

class BalanceInteractor {
    val walletInteractor = WalletInteractor.getInstance()
    private var lastUpdateTimeMs: Long = 0
    private var cachedSmartBalance: BigDecimal = BigDecimal.ZERO

    fun getBitcoinBalance(): BigDecimal {
        val balance = walletInteractor.getBitcoinWallet()?.getBalance(Wallet.BalanceType.ESTIMATED)
        return if(balance == null || balance.isZero)
            BigDecimal.ZERO
        else
            balance.toBtc()
    }

    fun getSmartBalance(): BigDecimal {
        val currentTimeMs = System.currentTimeMillis()
        val updateBalance = updateSmartBalance(currentTimeMs)
        if(updateBalance) {
            val sbchAddress = walletInteractor.getSmartAddress()
            val sbchWeiBalance =
                WalletManager.web3?.ethGetBalance(sbchAddress, DefaultBlockParameterName.LATEST)
                    ?.send()?.balance
            val sbchBalance = sbchWeiBalance?.toLong()?.let { BalanceFormatter.toEtherBalance(it) }
            cachedSmartBalance = sbchBalance ?: BigDecimal.ZERO
        }

        return cachedSmartBalance
    }

    private fun updateSmartBalance(currentTimeMs: Long): Boolean {
        val difference = currentTimeMs - lastUpdateTimeMs
        return difference >= 10 * 1000 // 10 seconds
    }

    companion object {
        private var instance: BalanceInteractor? = null
        fun getInstance(): BalanceInteractor {
            if(instance == null) {
                instance = BalanceInteractor()
            }
            return instance as BalanceInteractor
        }
    }
}