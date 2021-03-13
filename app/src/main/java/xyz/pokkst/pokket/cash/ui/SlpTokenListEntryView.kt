package xyz.pokkst.pokket.cash.ui

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.luminiasoft.ethereum.blockiesandroid.BlockiesIdenticon
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.wallet.WalletManager
import java.util.*

class SlpTokenListEntryView {
    companion object {
        fun instanceOf(activity: Activity?, position: Int, layoutResId: Int): View {
            val view = LayoutInflater.from(activity)
                .inflate(layoutResId, null)
            var isNft = false
            val tokenBal = try {
                WalletManager.walletKit?.slpBalances?.get(position)
            } catch(e: Exception) {
                val nft = WalletManager.walletKit?.nftBalances?.get(position)
                if(nft != null) {
                    isNft = true
                }

                nft
            }
            val tokenId = tokenBal?.tokenId
            val slpImage = view.findViewById<BlockiesIdenticon>(R.id.slpImage)
            val slpIcon = view.findViewById<ImageView>(R.id.slpWithIcon)
            val slpBlockiesAddress = blockieAddressFromTokenId(
                tokenId
                    ?: error("")
            )
            val slpToken = WalletManager.walletKit?.getSlpToken(tokenId) ?: WalletManager.walletKit?.getNft(tokenId)
            try {
                if (slpToken != null) {
                    val exists =
                        activity?.resources?.getIdentifier(
                            "slp$tokenId",
                            "drawable",
                            activity.packageName
                        ) != 0
                    if (exists) {
                        val drawable =
                            activity?.resources?.getDrawable(
                                activity.resources.getIdentifier(
                                    "slp$tokenId",
                                    "drawable",
                                    activity.packageName
                                )
                            )
                        activity?.runOnUiThread {
                            slpIcon.setImageDrawable(drawable)
                            slpImage.visibility = View.GONE
                            slpIcon.visibility = View.VISIBLE
                        }
                    } else {
                        slpImage.setAddress(slpBlockiesAddress)
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
            val balance =
                tokenBal.balance
            text1.text = String.format(
                Locale.ENGLISH, "%.${slpToken?.decimals
                    ?: 0}f", balance
            )
            text3.text = if(isNft) {
                slpToken?.ticker + " (NFT)"
            } else {
                slpToken?.ticker
            }
            text2.text = slpToken?.tokenId

            return view
        }

        private fun blockieAddressFromTokenId(tokenId: String): String {
            return tokenId.slice(IntRange(12, tokenId.count() - 1))
        }
    }
}