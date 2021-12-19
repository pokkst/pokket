package xyz.pokkst.pokket.cash.ui

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import org.bitcoinj.core.TransactionOutput
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.util.PriceHelper

class PledgeEntryView {
    companion object {
        fun instanceOf(
            activity: Activity?,
            position: Int,
            plegdesFormatted: ArrayList<Map<String, TransactionOutput>>
        ): View {
            val view = LayoutInflater.from(activity)
                .inflate(R.layout.pledge_item_cell, null)
            val txHashText = view.findViewById<TextView>(R.id.text1)
            val bchAmountText = view.findViewById<TextView>(R.id.text2)
            val fiatAmountText = view.findViewById<TextView>(R.id.text3)

            val utxo = plegdesFormatted[position]["utxo"]
            val value = utxo?.value?.toPlainString()
            val fiatValue = value?.toDouble()?.times(PriceHelper.price)
            txHashText.text = utxo?.parentTransactionHash.toString()
            bchAmountText.text = "$value BCH"
            fiatAmountText.text = "($$fiatValue)"

            return view
        }
    }
}