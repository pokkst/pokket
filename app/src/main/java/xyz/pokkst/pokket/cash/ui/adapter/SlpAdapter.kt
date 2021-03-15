package xyz.pokkst.pokket.cash.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.luminiasoft.ethereum.blockiesandroid.BlockiesIdenticon
import com.squareup.picasso.Picasso
import org.bitcoinj.core.slp.SlpTokenBalance
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.ui.listener.SlpAdapterListener
import xyz.pokkst.pokket.cash.wallet.WalletManager
import java.util.*

class SlpAdapter(private val dataSet: List<SlpTokenBalance>) :
        RecyclerView.Adapter<SlpAdapter.SlpViewHolder>() {

    var listener: SlpAdapterListener? = null

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): SlpViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.token_spinner_cell, viewGroup, false)
        return SlpViewHolder(view, listener)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: SlpViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.bind(dataSet[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    class SlpViewHolder(itemView: View, val listener: SlpAdapterListener?) : RecyclerView.ViewHolder(itemView) {

        fun bind(tokenBalance: SlpTokenBalance) {
            itemView.setOnClickListener {
                listener?.onClickToken(tokenBalance)
            }
            val tokenId = tokenBalance.tokenId
            val slpImage = itemView.findViewById<BlockiesIdenticon>(R.id.slpImage)
            val slpIcon = itemView.findViewById<ImageView>(R.id.slpWithIcon)
            val slpBlockiesAddress = blockieAddressFromTokenId(
                    tokenId
                            ?: error("")
            )

            val slpToken = WalletManager.walletKit?.getSlpToken(tokenId)

            try {
                if (slpToken != null) {
                    val picasso = Picasso.Builder(itemView.context)
                            .listener { picasso, uri, exception ->
                                slpImage.setAddress(slpBlockiesAddress)
                                slpImage.setCornerRadius(128f)
                                slpImage.visibility = View.VISIBLE
                                slpIcon.visibility = View.GONE
                            }
                            .build()
                    picasso.load("https://tokens.bch.sx/64/${slpToken.tokenId}.png").into(slpIcon)
                    slpImage.visibility = View.GONE
                    slpIcon.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                slpImage.setAddress(slpBlockiesAddress)
                slpImage.setCornerRadius(128f)
            }

            val text1 = itemView.findViewById<TextView>(R.id.text1)
            val text2 = itemView.findViewById<TextView>(R.id.text2)
            val text3 = itemView.findViewById<TextView>(R.id.text3)
            val balance =
                    tokenBalance.balance
            text1.text = String.format(
                    Locale.ENGLISH, "%.${slpToken?.decimals
                    ?: 0}f", balance
            )
            text3.text = slpToken?.ticker
            text2.text = slpToken?.tokenId

        }

        private fun blockieAddressFromTokenId(tokenId: String): String {
            return tokenId.slice(IntRange(12, tokenId.count() - 1))
        }
    }
}
