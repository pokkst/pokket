package xyz.pokkst.pokket.ui

import android.app.Activity
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import xyz.pokkst.pokket.R
import xyz.pokkst.pokket.util.DateFormatter

class TransactionListEntryView {
    companion object {
        fun instanceOf(activity: Activity?, position: Int, txListFormatted: ArrayList<Map<String, String>>): View {
            val view = LayoutInflater.from(activity)
                .inflate(R.layout.transaction_list_item, null)
            val sentReceivedTextView =
                view.findViewById<TextView>(R.id.transaction_sent_received_label)
            val dateTextView =
                view.findViewById<TextView>(R.id.transaction_date)
            val bitsMoved =
                view.findViewById<TextView>(R.id.transaction_amount_bits)
            val dollarsMoved =
                view.findViewById<TextView>(R.id.transaction_amount_dollars)

            val ticker = txListFormatted[position]["ticker"]
            val isSlp = txListFormatted[position]["slp"]
            val action = txListFormatted[position]["action"]
            val received = action == "received"
            val amount = txListFormatted[position]["amount"]
            val fiatAmount = txListFormatted[position]["fiatAmount"]
            val timestamp = txListFormatted[position]["timestamp"]?.let {
                java.lang.Long.parseLong(it)
            }
            sentReceivedTextView.setBackgroundResource(if (received) R.drawable.received_label else R.drawable.sent_label)
            sentReceivedTextView.setTextColor(if (received) Color.parseColor("#00BF00") else Color.parseColor("#FF5454"))
            sentReceivedTextView.text = action
            bitsMoved.text =
                if (isSlp == "true" && ticker != "") "$amount $ticker" else activity?.resources?.getString(
                    R.string.tx_amount_moved,
                    amount
                )
            dollarsMoved.text =
                if (isSlp == "true" && ticker != "") null else "($$fiatAmount)"
            dateTextView.text = if (timestamp != 0L) {
                timestamp?.let {
                    DateFormatter.getFormattedDateFromLong(
                        activity,
                        it
                    )
                }
            } else DateFormatter.getFormattedDateFromLong(
                activity,
                System.currentTimeMillis()
            )

            return view
        }
    }
}