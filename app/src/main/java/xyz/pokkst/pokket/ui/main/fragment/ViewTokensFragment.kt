package xyz.pokkst.pokket.ui.main.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.luminiasoft.ethereum.blockiesandroid.BlockiesIdenticon
import kotlinx.android.synthetic.main.component_input_numpad.view.*
import kotlinx.android.synthetic.main.fragment_send_amount.view.*
import org.bitcoinj.core.Coin
import org.bitcoinj.core.InsufficientMoneyException
import org.bitcoinj.core.Transaction
import org.bitcoinj.protocols.payments.PaymentProtocol
import org.bitcoinj.protocols.payments.PaymentProtocolException
import org.bitcoinj.protocols.payments.PaymentSession
import org.bitcoinj.utils.MonetaryFormat
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import xyz.pokkst.pokket.MainActivity
import xyz.pokkst.pokket.R
import xyz.pokkst.pokket.ui.NonScrollListView
import xyz.pokkst.pokket.util.Constants
import xyz.pokkst.pokket.util.PriceHelper
import xyz.pokkst.pokket.util.Toaster
import xyz.pokkst.pokket.wallet.WalletManager
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.concurrent.ExecutionException


/**
 * A placeholder fragment containing a simple view.
 */
class ViewTokensFragment : Fragment() {
    var root: View? = null
    private var refreshingTokens: Boolean = false
    var tokenList = ArrayList<Map<String, String>>()
    private var srlSLP: SwipeRefreshLayout? = null
    private var slpList: NonScrollListView? = null

    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Constants.ACTION_MAIN_ENABLE_PAGER == intent.action) {
                this@ViewTokensFragment.findNavController().popBackStack(R.id.sendHomeFragment, false)
            }
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        root = inflater.inflate(R.layout.fragment_view_tokens, container, false)
        (requireActivity() as MainActivity).enableTokensScreen()
        val filter = IntentFilter()
        filter.addAction(Constants.ACTION_MAIN_ENABLE_PAGER)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, filter)
        srlSLP = root?.findViewById(R.id.srlSLP)
        slpList = root?.findViewById(R.id.slpList)
        this.srlSLP?.setOnRefreshListener { this.refresh() }
        refresh()
        return root
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
    }

    private fun refresh() {
        object : Thread() {
            override fun run() {
                try {
                    WalletManager.walletKit?.recalculateSlpUtxos()

                    this@ViewTokensFragment.requireActivity().runOnUiThread {
                        setSLPList()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()

        val srlSlp = srlSLP
        if (srlSlp != null && srlSlp.isRefreshing) srlSLP?.isRefreshing = false
    }

    private fun setSLPList() {
        tokenList.clear()

        val walletKit = WalletManager.walletKit
        if(walletKit != null) {
            for (tokenBalance in walletKit.slpBalances) {
                val slpToken = walletKit.getSlpToken(tokenBalance.tokenId)

                val datum = HashMap<String, String>()
                val tokenTicker = slpToken.ticker
                val tokenHash = slpToken.tokenId
                val balance = tokenBalance.balance

                datum["tokenHash"] = tokenHash
                datum["tokenTicker"] = tokenTicker!!
                datum["balance"] = balance.toString()

                tokenList.add(datum)
            }

            if(tokenList.isNotEmpty()) {
                root?.findViewById<TextView>(R.id.loading_tokens_view)?.visibility = View.GONE
                root?.findViewById<TextView>(R.id.no_tokens_view)?.visibility = View.GONE
                slpList?.visibility = View.VISIBLE
                val itemsAdapter = object : SimpleAdapter(
                    requireContext(),
                    tokenList,
                    R.layout.token_list_cell,
                    null,
                    null
                ) {
                    override fun getView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        // Get the Item from ListView
                        val view = LayoutInflater.from(requireContext())
                            .inflate(R.layout.token_list_cell, null)
                        val slpBlockiesAddress = blockieAddressFromTokenId(
                            tokenList[position]["tokenHash"]
                                ?: error("")
                        )

                        val slpImage = view.findViewById<BlockiesIdenticon>(R.id.slpImage)
                        val slpIcon = view.findViewById<ImageView>(R.id.slpWithIcon)
                        val tokenHash = tokenList[position]["tokenHash"]
                        val slpToken = WalletManager.walletKit?.getSlpToken(tokenHash)

                        object : Thread() {
                            override fun run() {
                                try {
                                    if (slpToken != null) {
                                        val exists =
                                            this@ViewTokensFragment.resources.getIdentifier(
                                                "slp$tokenHash",
                                                "drawable",
                                                this@ViewTokensFragment.requireActivity().packageName
                                            ) != 0
                                        if (exists) {
                                            val drawable =
                                                this@ViewTokensFragment.resources.getDrawable(
                                                    this@ViewTokensFragment.resources.getIdentifier(
                                                        "slp$tokenHash",
                                                        "drawable",
                                                        this@ViewTokensFragment.requireActivity().packageName
                                                    )
                                                )
                                            this@ViewTokensFragment.requireActivity()
                                                .runOnUiThread {
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
                                            this@ViewTokensFragment.resources.getDrawable(
                                                this@ViewTokensFragment.resources.getIdentifier(
                                                    "logo_bch",
                                                    "drawable",
                                                    this@ViewTokensFragment.requireActivity().packageName
                                                )
                                            )
                                        this@ViewTokensFragment.requireActivity().runOnUiThread {
                                            slpIcon.setImageDrawable(drawable)
                                            slpImage.visibility = View.GONE
                                            slpIcon.visibility = View.VISIBLE
                                        }
                                    }
                                } catch (e: Exception) {
                                    slpImage.setAddress(slpBlockiesAddress)
                                    slpImage.setCornerRadius(128f)
                                }
                            }
                        }.start()

                        // Initialize a TextView for ListView each Item
                        val text1 = view.findViewById<TextView>(R.id.text1)
                        val text2 = view.findViewById<TextView>(R.id.text2)
                        val text3 = view.findViewById<TextView>(R.id.text3)
                        val tokenBalString = tokenList[position]["balance"].toString()
                        text1.text = String.format(
                            Locale.ENGLISH, "%.${slpToken?.decimals
                                ?: 0}f", java.lang.Double.parseDouble(tokenBalString)
                        )
                        text3.text = tokenList[position]["tokenTicker"].toString()
                        text2.text = slpToken?.tokenId
                        // Set the text color of TextView (ListView Item)
                        /*if (UIManager.nightModeEnabled) {
                        text1.setTextColor(Color.WHITE)
                        text2.setTextColor(Color.GRAY)
                        text3.setTextColor(Color.WHITE)
                    } else {*/
                        text1.setTextColor(Color.BLACK)
                        text3.setTextColor(Color.BLACK)
                        //}

                        text2.ellipsize = TextUtils.TruncateAt.END
                        text2.maxLines = 1
                        text2.isSingleLine = true
                        // Generate ListView Item using TextView
                        return view
                    }
                }
                this.requireActivity().runOnUiThread {
                    slpList?.adapter = itemsAdapter
                    slpList?.refreshDrawableState()
                }
            } else {
                root?.findViewById<TextView>(R.id.loading_tokens_view)?.visibility = View.GONE
                root?.findViewById<TextView>(R.id.no_tokens_view)?.visibility = View.VISIBLE
                slpList?.visibility = View.GONE
            }
        }
    }

    private fun blockieAddressFromTokenId(tokenId: String): String {
        return tokenId.slice(IntRange(12, tokenId.count() - 1))
    }
}