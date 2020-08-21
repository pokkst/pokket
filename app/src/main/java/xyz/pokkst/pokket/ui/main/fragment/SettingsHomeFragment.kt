package xyz.pokkst.pokket.ui.main.fragment

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.SimpleAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_settings_home.view.*
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.slp.SlpOpReturn
import org.bitcoinj.core.slp.SlpTransaction
import org.bitcoinj.wallet.Wallet
import xyz.pokkst.pokket.R
import xyz.pokkst.pokket.util.Constants
import xyz.pokkst.pokket.util.DateFormatter
import xyz.pokkst.pokket.util.PriceHelper
import xyz.pokkst.pokket.wallet.WalletManager
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


/**
 * A placeholder fragment containing a simple view.
 */
class SettingsHomeFragment : Fragment() {
    private var sentColor = 0
    private var receivedColor = 0
    private var txList = ArrayList<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_settings_home, container, false)
        sentColor = Color.parseColor("#FF5454")
        receivedColor = Color.parseColor("#00BF00")
        this.setArrayAdapter(root, WalletManager.walletKit?.wallet())

        root.about.findViewById<RelativeLayout>(R.id.setting_layout).setOnClickListener {
            val intent = Intent(Constants.ACTION_SETTINGS_HIDE_BAR)
            LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(intent)
            findNavController().navigate(R.id.nav_to_about)
        }
        root.about.findViewById<TextView>(R.id.setting_label).text = resources.getString(R.string.about)

        root.recovery_phrase.findViewById<RelativeLayout>(R.id.setting_layout).setOnClickListener {
            val intent = Intent(Constants.ACTION_SETTINGS_HIDE_BAR)
            LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(intent)
            findNavController().navigate(R.id.nav_to_phrase)
        }
        root.recovery_phrase.findViewById<TextView>(R.id.setting_label).text = resources.getString(R.string.recovery_phrase_label)

        root.extended_public_key.findViewById<RelativeLayout>(R.id.setting_layout).setOnClickListener {
            val intent = Intent(Constants.ACTION_SETTINGS_HIDE_BAR)
            LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(intent)
            findNavController().navigate(R.id.nav_to_phrase)
        }
        root.extended_public_key.findViewById<TextView>(R.id.setting_label).text = resources.getString(R.string.epk_label)

        root.start_recovery_wallet.setOnClickListener {
            val intent = Intent(Constants.ACTION_SETTINGS_HIDE_BAR)
            LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(intent)
            findNavController().navigate(R.id.nav_to_wipe)
        }

        root.transactions_list.setOnItemClickListener { parent, view, position, id ->
            val intent = Intent(Constants.ACTION_SETTINGS_HIDE_BAR)
            LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(intent)
            val txid = txList[position]
            val tx = WalletManager.walletKit?.wallet()?.getTransaction(Sha256Hash.wrap(txid))
            val amount = tx?.getValue(WalletManager.walletKit?.wallet())
            val isSlp = SlpOpReturn.isSlpTx(tx)
            val slpAmount = if(isSlp) {
                val slpTx = SlpTransaction(tx)
                val slpToken = WalletManager.walletKit?.getSlpToken(slpTx.tokenId)
                if(slpToken != null) {
                    val slpAmount = slpTx.getRawValue(WalletManager.walletKit?.wallet())
                        .scaleByPowerOfTen(-slpToken.decimals).toDouble()
                    slpAmount
                } else {
                    0.0
                }
            } else {
                0.0
            }
            if(amount?.isPositive!! || slpAmount > 0) {
                findNavController().navigate(SettingsHomeFragmentDirections.navToTxReceived(txid, isSlp))
            } else if(amount.isNegative) {
                findNavController().navigate(SettingsHomeFragmentDirections.navToTxSent(txid, isSlp))
            }
        }

        root.more_transactions.setOnClickListener {
            val intent = Intent(Constants.ACTION_SETTINGS_HIDE_BAR)
            LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(intent)
            findNavController().navigate(R.id.nav_to_tx_list)
        }

        return root
    }

    private fun setArrayAdapter(root: View, wallet: Wallet?) {
        setListViewShit(root, wallet)
    }

    private fun setListViewShit(root: View, wallet: Wallet?) {
        object : Thread() {
            override fun run() {
                if (wallet != null) {
                    val txListFromWallet = wallet.getRecentTransactions(5, false)
                    val actualTxCount = wallet.getRecentTransactions(0, false).size
                    txList = ArrayList<String>()

                    if (txListFromWallet != null && txListFromWallet.size != 0) {
                        val txListFormatted = ArrayList<Map<String, String>>()

                        if (txListFromWallet.size > 0) {
                            requireActivity().runOnUiThread {
                                if(actualTxCount > 5) { root.more_transactions.visibility = View.VISIBLE }
                                root.no_transactions.visibility = View.GONE
                            }

                            for (x in 0 until txListFromWallet.size) {
                                val tx = txListFromWallet[x]
                                val isSlp = SlpOpReturn.isSlpTx(tx)
                                val confirmations = tx.confidence.depthInBlocks
                                val value = tx.getValue(wallet)
                                val timestamp = tx.updateTime.time.toString()
                                val datum = HashMap<String, String>()
                                var ticker = ""
                                val amountStr = if(isSlp) {
                                    val slpTx = SlpTransaction(tx)
                                    val slpToken = WalletManager.walletKit?.getSlpToken(slpTx.tokenId)
                                    if(slpToken != null) {
                                        ticker = slpToken.ticker
                                        val slpAmount = slpTx.getRawValue(WalletManager.walletKit?.wallet())
                                            .scaleByPowerOfTen(-slpToken.decimals).toDouble()
                                        formatBalance(slpAmount, "#.#########")
                                    } else {
                                        value.toPlainString()
                                    }
                                } else {
                                    value.toPlainString()
                                }

                                datum["action"] = if(value.isPositive || amountStr.toDouble() > 0) {
                                    "received"
                                } else {
                                    "sent"
                                }

                                datum["ticker"] = ticker
                                datum["slp"] = if(isSlp) "true" else "false"
                                datum["amount"] = amountStr
                                datum["fiatAmount"] = formatBalance((amountStr.toDouble() * PriceHelper.price), "0.00")
                                datum["timestamp"] = timestamp

                                when {
                                    confirmations == 0 -> datum["confirmations"] = "0/unconfirmed"
                                    confirmations < 6 -> datum["confirmations"] = "$confirmations/6 confirmations"
                                    else -> datum["confirmations"] = "6+ confirmations"
                                }

                                txList.add(tx.txId.toString())
                                txListFormatted.add(datum)
                            }

                            val itemsAdapter = object : SimpleAdapter(requireContext(), txListFormatted, R.layout.transaction_list_item, null, null) {
                                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                                    // Get the Item from ListView
                                    val view = LayoutInflater.from(requireContext()).inflate(R.layout.transaction_list_item, null)
                                    val sentReceivedTextView = view.findViewById<TextView>(R.id.transaction_sent_received_label)
                                    val dateTextView = view.findViewById<TextView>(R.id.transaction_date)
                                    val bitsMoved = view.findViewById<TextView>(R.id.transaction_amount_bits)
                                    val dollarsMoved = view.findViewById<TextView>(R.id.transaction_amount_dollars)

                                    val ticker = txListFormatted[position]["ticker"]
                                    val isSlp = txListFormatted[position]["slp"]
                                    val action = txListFormatted[position]["action"]
                                    val received = action == "received"
                                    val amount = txListFormatted[position]["amount"]
                                    val fiatAmount = txListFormatted[position]["fiatAmount"]
                                    val confirmations = txListFormatted[position]["confirmations"]
                                    val timestamp = txListFormatted[position]["timestamp"]?.let { java.lang.Long.parseLong(it) }
                                    sentReceivedTextView.setBackgroundResource(if (received) R.drawable.received_label else R.drawable.sent_label)
                                    sentReceivedTextView.setTextColor(if (received) receivedColor else sentColor)
                                    sentReceivedTextView.text = action
                                    bitsMoved.text = if(isSlp == "true" && ticker != "") "$amount $ticker" else resources.getString(R.string.tx_amount_moved, amount)
                                    dollarsMoved.text = if(isSlp == "true" && ticker != "") null else "($$fiatAmount)"
                                    dateTextView.text = if (timestamp != 0L) DateFormatter.getFormattedDateFromLong(
                                        requireActivity(),
                                        timestamp!!
                                    ) else DateFormatter.getFormattedDateFromLong(requireActivity(), System.currentTimeMillis())
                                    // Generate ListView Item using TextView
                                    return view
                                }
                            }
                            requireActivity().runOnUiThread { root.transactions_list.adapter = itemsAdapter }
                        } else {
                            requireActivity().runOnUiThread {
                                root.space.visibility = View.GONE
                                root.transactions_list.visibility = View.GONE
                                root.no_transactions.visibility = View.VISIBLE
                            }
                        }
                    } else {
                        requireActivity().runOnUiThread {
                            root.space.visibility = View.GONE
                            root.transactions_list.visibility = View.GONE
                            root.no_transactions.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }.start()
    }

    fun formatBalance(amount: Double, pattern: String): String {
        val formatter = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
        return formatter.format(amount)
    }
}