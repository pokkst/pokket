package xyz.pokkst.pokket.cash.ui.main.fragment.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SimpleAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_settings_transactions.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.slp.SlpOpReturn
import org.bitcoinj.core.slp.SlpTransaction
import org.bitcoinj.wallet.Wallet
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.SettingsActivity
import xyz.pokkst.pokket.cash.ui.TransactionListEntryView
import xyz.pokkst.pokket.cash.util.BalanceFormatter
import xyz.pokkst.pokket.cash.util.PriceHelper
import xyz.pokkst.pokket.cash.wallet.WalletManager


/**
 * A placeholder fragment containing a simple view.
 */
class SettingsTransactionsFragment : Fragment() {
    private var txList = ArrayList<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings_transactions, container, false)
        this.setArrayAdapter(root, WalletManager.wallet)
        root.transactions_list.setOnItemClickListener { parent, view, position, id ->
            (activity as? SettingsActivity)?.adjustDeepMenu(1)
            val txid = txList[position]
            val tx = WalletManager.wallet?.getTransaction(Sha256Hash.wrap(txid))
            val amount = tx?.getValue(WalletManager.wallet)
            val isSlp = SlpOpReturn.isSlpTx(tx)
            val slpAmount = if (isSlp) {
                val slpTx = SlpTransaction(tx)
                val slpToken = WalletManager.walletKit?.getSlpToken(slpTx.tokenId)
                if (slpToken != null) {
                    val slpAmount = slpTx.getRawValue(WalletManager.wallet)
                        .scaleByPowerOfTen(-slpToken.decimals).toDouble()
                    slpAmount
                } else {
                    0.0
                }
            } else {
                0.0
            }
            if (amount?.isPositive == true || slpAmount > 0) {
                findNavController().navigate(
                    SettingsTransactionsFragmentDirections.navToTxReceived(
                        txid,
                        isSlp
                    )
                )
            } else if (amount?.isNegative == true) {
                findNavController().navigate(
                    SettingsTransactionsFragmentDirections.navToTxSent(
                        txid,
                        isSlp
                    )
                )
            }
        }
        return root
    }

    private fun setArrayAdapter(root: View, wallet: Wallet?) {
        setListViewShit(root, wallet)
    }

    private fun setListViewShit(root: View, wallet: Wallet?) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (wallet != null) {
                val txListFromWallet = wallet.getRecentTransactions(0, false)
                txList = ArrayList<String>()

                if (txListFromWallet != null && txListFromWallet.size != 0) {
                    val txListFormatted = ArrayList<Map<String, String>>()

                    if (txListFromWallet.size > 0) {
                        for (x in 0 until txListFromWallet.size) {
                            val tx = txListFromWallet[x]
                            val isSlp = SlpOpReturn.isSlpTx(tx)
                            val value = tx.getValue(wallet)
                            val timestamp = tx.updateTime.time.toString()
                            val datum = HashMap<String, String>()
                            var ticker = ""
                            val amountStr = if (isSlp) {
                                val slpTx = SlpTransaction(tx)
                                val slpToken =
                                    WalletManager.walletKit?.getSlpToken(slpTx.tokenId)
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

                            datum["action"] =
                                if (value.isPositive || amountStr.toDouble() > 0) {
                                    "received"
                                } else {
                                    "sent"
                                }

                            datum["ticker"] = ticker
                            datum["slp"] = if (isSlp) "true" else "false"
                            datum["amount"] = amountStr
                            datum["fiatAmount"] = BalanceFormatter.formatBalance(
                                (amountStr.toDouble() * PriceHelper.price),
                                "0.00"
                            )
                            datum["timestamp"] = timestamp

                            txList.add(tx.txId.toString())
                            txListFormatted.add(datum)
                        }

                        val itemsAdapter = object : SimpleAdapter(
                            requireContext(),
                            txListFormatted,
                            R.layout.transaction_list_item,
                            null,
                            null
                        ) {
                            override fun getView(
                                position: Int,
                                convertView: View?,
                                parent: ViewGroup
                            ): View {
                                return TransactionListEntryView.instanceOf(activity, position, txListFormatted)
                            }
                        }
                        requireActivity().runOnUiThread {
                            root.transactions_list.adapter = itemsAdapter
                        }
                    }
                }
            }
        }
    }
}