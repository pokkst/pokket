package xyz.pokkst.pokket.cash.ui.listener

import org.bitcoinj.core.slp.SlpTokenBalance

interface SlpAdapterListener {
    fun onClickToken(slpTokenBalance: SlpTokenBalance)
}