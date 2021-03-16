package xyz.pokkst.pokket.cash.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.slp.SlpOpReturn
import org.bitcoinj.core.slp.SlpTransaction
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.ui.listener.TxAdapterListener
import xyz.pokkst.pokket.cash.util.BalanceFormatter
import xyz.pokkst.pokket.cash.util.DateFormatter
import xyz.pokkst.pokket.cash.util.PriceHelper
import xyz.pokkst.pokket.cash.wallet.WalletManager

class TransactionAdapter(private val dataSet: List<Transaction>) :
        RecyclerView.Adapter<TransactionAdapter.NftAdapter>() {

    var listener: TxAdapterListener? = null

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): NftAdapter {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.transaction_list_item, viewGroup, false)
        return NftAdapter(view, listener)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: NftAdapter, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.bind(dataSet[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    class NftAdapter(itemView: View, private val listener: TxAdapterListener?) : RecyclerView.ViewHolder(itemView) {

        fun bind(tx: Transaction) {
            itemView.setOnClickListener {
                listener?.onClickTransaction(tx)
            }
            val sentReceivedTextView = itemView.findViewById<TextView>(R.id.transaction_sent_received_label)
            val dateTextView = itemView.findViewById<TextView>(R.id.transaction_date)
            val bitsMoved = itemView.findViewById<TextView>(R.id.transaction_amount_bits)
            val dollarsMoved = itemView.findViewById<TextView>(R.id.transaction_amount_dollars)

            val isSlp = SlpOpReturn.isSlpTx(tx) || SlpOpReturn.isNftChildTx(tx)
            val value = tx.getValue(WalletManager.wallet)
            var ticker = ""
            val amountStr = if (isSlp) {
                val slpTx = SlpTransaction(tx)
                val slpToken =
                        WalletManager.walletKit?.getSlpToken(slpTx.tokenId)
                                ?: WalletManager.walletKit?.getNft(slpTx.tokenId)
                if (slpToken != null) {
                    ticker = slpToken.ticker
                    val slpAmount = slpTx.getRawValue(WalletManager.wallet)
                            .scaleByPowerOfTen(-slpToken.decimals).toDouble()
                    BalanceFormatter.formatBalance(slpAmount, "#.#########")
                } else {
                    value.toPlainString()
                }
            } else {
                value.toPlainString()
            }
            val action = if (value.isPositive) "received" else "sent"
            val received = action == "received"
            val fiatAmount = BalanceFormatter.formatBalance(
                    (amountStr.toDouble() * PriceHelper.price),
                    "0.00"
            )
            val timestamp = tx.updateTime.time
            sentReceivedTextView.setBackgroundResource(if (received) R.drawable.received_label else R.drawable.sent_label)
            sentReceivedTextView.setTextColor(if (received) Color.parseColor("#00BF00") else Color.parseColor("#FF5454"))
            sentReceivedTextView.text = action
            bitsMoved.text =
                    if (isSlp && ticker != "") "$amountStr $ticker" else itemView.resources?.getString(
                            R.string.tx_amount_moved,
                            amountStr
                    )
            dollarsMoved.text =
                    if (isSlp && ticker != "") null else "($$fiatAmount)"
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
