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
import xyz.pokkst.pokket.cash.util.NFTConstants
import xyz.pokkst.pokket.cash.wallet.WalletManager

class NftAdapter(private val dataSet: List<SlpTokenBalance>) :
    RecyclerView.Adapter<NftAdapter.NftViewHolder>() {

    var listener: SlpAdapterListener? = null

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): NftViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.token_spinner_cell, viewGroup, false)
        return NftViewHolder(view, listener)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: NftViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.bind(dataSet[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    class NftViewHolder(itemView: View, val listener: SlpAdapterListener?) : RecyclerView.ViewHolder(itemView) {

        fun bind(nftBalance: SlpTokenBalance) {
            itemView.setOnClickListener {
                listener?.onClickToken(nftBalance)
            }
            val tokenId = nftBalance.tokenId
            val slpImage = itemView.findViewById<BlockiesIdenticon>(R.id.slpImage)
            val slpIcon = itemView.findViewById<ImageView>(R.id.slpWithIcon)
            val nft = WalletManager.walletKit?.getNft(tokenId)
            val nftBlockiesAddress = blockieAddressFromTokenId(
                    tokenId
                            ?: error("")
            )
            try {
                if(nft != null) {
                    val nftParentId = nft.nftParentId
                    if(nftParentId == NFTConstants.NFT_PARENT_ID_WAIFU) {
                        Picasso.get().load("https://icons.waifufaucet.com/64/${nft.tokenId}.png").into(slpIcon)
                        slpIcon.visibility = View.VISIBLE
                        slpImage.visibility = View.GONE
                    } else {
                        slpImage.setAddress(nftBlockiesAddress)
                        slpImage.setCornerRadius(128f)
                    }
                }
            } catch (e: Exception) {
                slpImage.setAddress(nftBlockiesAddress)
                slpImage.setCornerRadius(128f)
            }

            val text1 = itemView.findViewById<TextView>(R.id.text1)
            val text2 = itemView.findViewById<TextView>(R.id.text2)
            val text3 = itemView.findViewById<TextView>(R.id.text3)
            text1.text = nft?.name
            text3.text = nft?.ticker + " (NFT)"
            text2.text = nft?.tokenId
        }

        private fun blockieAddressFromTokenId(tokenId: String): String {
            return tokenId.slice(IntRange(12, tokenId.count() - 1))
        }
    }
}
