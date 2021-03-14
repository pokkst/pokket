package xyz.pokkst.pokket.cash.ui

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.luminiasoft.ethereum.blockiesandroid.BlockiesIdenticon
import com.squareup.picasso.Picasso
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.util.NFTConstants
import xyz.pokkst.pokket.cash.wallet.WalletManager
import java.util.*

class NftListEntryView {
    companion object {
        fun instanceOf(activity: Activity?, position: Int, layoutResId: Int): View {
            val view = LayoutInflater.from(activity)
                    .inflate(layoutResId, null)
            val tokenBal = WalletManager.walletKit?.nftBalances?.get(position)
            val tokenId = tokenBal?.tokenId
            val slpImage = view.findViewById<BlockiesIdenticon>(R.id.slpImage)
            val slpIcon = view.findViewById<ImageView>(R.id.slpWithIcon)
            val slpBlockiesAddress = blockieAddressFromTokenId(
                    tokenId
                            ?: error("")
            )

            val nft = WalletManager.walletKit?.getNft(tokenId)

            try {
                if(nft != null) {
                    val nftParentId = nft.nftParentId
                    if(nftParentId == NFTConstants.NFT_PARENT_ID_WAIFU) {
                        Picasso.get().load("https://icons.waifufaucet.com/64/${nft.tokenId}.png").into(slpIcon)
                        slpIcon.visibility = View.VISIBLE
                        slpImage.visibility = View.GONE
                    } else {
                        slpImage.setAddress(nft.tokenId)
                        slpImage.setCornerRadius(128f)
                    }
                } else {
                    val drawable =
                        activity?.resources?.getDrawable(
                            activity.resources.getIdentifier(
                                "logo_bch",
                                "drawable",
                                activity.packageName
                            )
                        )
                    activity?.runOnUiThread {
                        slpIcon.setImageDrawable(drawable)
                        slpImage.visibility = View.GONE
                        slpIcon.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                slpImage.setAddress(slpBlockiesAddress)
                slpImage.setCornerRadius(128f)
            }

            val text1 = view.findViewById<TextView>(R.id.text1)
            val text2 = view.findViewById<TextView>(R.id.text2)
            val text3 = view.findViewById<TextView>(R.id.text3)
            text1.text = nft?.name
            text3.text = nft?.ticker + " (NFT)"
            text2.text = nft?.tokenId

            return view
        }

        private fun blockieAddressFromTokenId(tokenId: String): String {
            return tokenId.slice(IntRange(12, tokenId.count() - 1))
        }
    }
}