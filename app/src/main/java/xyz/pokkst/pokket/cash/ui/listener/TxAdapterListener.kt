package xyz.pokkst.pokket.cash.ui.listener

import org.bitcoinj.core.Transaction

interface TxAdapterListener {
    fun onClickTransaction(tx: Transaction)
}