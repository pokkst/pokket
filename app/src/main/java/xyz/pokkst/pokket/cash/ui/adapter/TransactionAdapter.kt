package xyz.pokkst.pokket.cash.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.bitcoinj.core.Transaction
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.ui.listener.TxAdapterListener
import xyz.pokkst.pokket.cash.util.BalanceFormatter
import xyz.pokkst.pokket.cash.util.DateFormatter
import xyz.pokkst.pokket.cash.util.PriceHelper
import xyz.pokkst.pokket.cash.wallet.WalletService

class TransactionAdapter(private val dataSet: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    var listener: TxAdapterListener? = null

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): TransactionViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.transaction_list_item, viewGroup, false)
        return TransactionViewHolder(view, listener)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: TransactionViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.bind(dataSet[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    class TransactionViewHolder(itemView: View, private val listener: TxAdapterListener?) :
        RecyclerView.ViewHolder(itemView) {

        fun bind(tx: Transaction) {
            itemView.setOnClickListener {
                listener?.onClickTransaction(tx)
            }
            val sentReceivedTextView =
                itemView.findViewById<TextView>(R.id.transaction_sent_received_label)
            val dateTextView = itemView.findViewById<TextView>(R.id.transaction_date)
            val bitsMoved = itemView.findViewById<TextView>(R.id.transaction_amount_bits)
            val dollarsMoved = itemView.findViewById<TextView>(R.id.transaction_amount_dollars)

            val value = tx.getValue(WalletService.wallet)
            val amountStr = value.toPlainString()
            val action = if (value.isPositive) "received" else "sent"
            val received = action == "received"
            val fiatAmount = BalanceFormatter.formatBalance(
                (amountStr.toDouble() * PriceHelper.price),
                "0.00"
            )
            val timestamp = tx.updateTime.time
            sentReceivedTextView.setBackgroundResource(if (received) R.drawable.received_label else R.drawable.sent_label)
            sentReceivedTextView.setTextColor(
                if (received) Color.parseColor("#00BF00") else Color.parseColor(
                    "#FF5454"
                )
            )
            sentReceivedTextView.text = action
            bitsMoved.text = itemView.resources?.getString(
                R.string.tx_amount_moved,
                amountStr
            )
            dollarsMoved.text = "($$fiatAmount)"
            dateTextView.text = if (timestamp != 0L) {
                timestamp.let {
                    DateFormatter.getFormattedDateFromLong(
                        itemView.context,
                        it
                    )
                }
            } else DateFormatter.getFormattedDateFromLong(
                itemView.context,
                System.currentTimeMillis()
            )
        }
    }
}
