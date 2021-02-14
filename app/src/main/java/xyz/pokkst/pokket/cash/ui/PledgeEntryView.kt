package xyz.pokkst.pokket.cash.ui

import android.app.Activity
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import org.bitcoinj.core.TransactionOutput
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.util.DateFormatter

class PledgeEntryView {
    companion object {
        fun instanceOf(activity: Activity?, position: Int, plegdesFormatted: ArrayList<Map<String, TransactionOutput>>): View {
            val view = LayoutInflater.from(activity)
                .inflate(R.layout.pledge_item_cell, null)
            val text1 =
                view.findViewById<TextView>(R.id.text1)
            val utxo = plegdesFormatted[position]["utxo"]
            text1.text = utxo?.parentTransactionHash.toString()
            return view
        }
    }
}