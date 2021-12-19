package xyz.pokkst.pokket.cash.ui.main.fragment.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.transaction_item_expanded_sent.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.slp.SlpTransaction
import org.bitcoinj.script.ScriptPattern
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.interactors.WalletInteractor
import xyz.pokkst.pokket.cash.util.BalanceFormatter
import xyz.pokkst.pokket.cash.util.ClipboardHelper
import xyz.pokkst.pokket.cash.util.PriceHelper
import xyz.pokkst.pokket.cash.wallet.WalletManager
import java.util.*


/**
 * A placeholder fragment containing a simple view.
 */
class TransactionSentFragment : Fragment() {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val walletInteractor = WalletInteractor.getInstance()
        val root = inflater.inflate(R.layout.transaction_item_expanded_sent, container, false)
        val txid = arguments?.getString("txid", "")
        val tx = walletInteractor.getBitcoinWallet()?.getTransaction(Sha256Hash.wrap(txid)) ?: return null
        root.tx_id.setOnClickListener {
            ClipboardHelper.copyToClipboard(activity, txid)
        }
        root.tx_hash_text.text = txid

        root.tx_status_text.text = if (tx.confidence.depthInBlocks > 0) {
            "confirmed in block #${tx.confidence.appearedAtChainHeight}"
        } else {
            "verified, waiting for confirmation"
        }

        val fromAddresses = ArrayList<String>()
        for (x in tx.inputs.indices) {
            fromAddresses.add(tx.inputs[x].outpoint.toString())
        }
        setSentFromAddresses(root.general_tx_from_layout, fromAddresses)

        val toAddresses = ArrayList<String?>()
        val toAmounts = ArrayList<Long>()
        for (x in tx.outputs.indices) {
            if (ScriptPattern.isOpReturn(tx.outputs[x].scriptPubKey)) {
                toAddresses.add("OP_RETURN")
            } else {
                val address = tx.outputs[x].scriptPubKey.getToAddress(WalletManager.parameters).toCash().toString()
                toAddresses.add(address)
            }

            toAmounts.add(tx.outputs[x].value.value)
        }

        val bchSent = -tx.getValueSentFromMe(walletInteractor.getBitcoinWallet()).toPlainString().toDouble()

        val bchFee = if (tx.fee != null) {
            tx.fee.toPlainString().toDouble()
        } else {
            0.0
        }
        root.tx_to_fee_amount_text.text = resources.getString(
                R.string.tx_amount_moved,
                "-${BalanceFormatter.formatBalance(bchFee, "#.########")}"
        )
        root.tx_amount_text.text = resources.getString(
            R.string.tx_amount_moved,
            BalanceFormatter.formatBalance(bchSent, "#.########")
        )

        lifecycleScope.launch(Dispatchers.IO) {
            val fiatValue = bchSent.times(PriceHelper.price)
            val feeFiatValue = bchFee * PriceHelper.price
            activity?.runOnUiThread {
                root.tx_to_fee_exchange_text.text =
                    "($-${BalanceFormatter.formatBalance(feeFiatValue, "0.00")})"
                root.tx_exchange_text.text =
                    "($${BalanceFormatter.formatBalance(fiatValue, "0.00")})"
            }
        }

        setSentToAddresses(root.general_tx_to_layout, toAddresses, toAmounts)

        return root
    }

    private fun setSentFromAddresses(
            view: LinearLayout,
            addresses: ArrayList<String>
    ) {
        val inflater = requireActivity().layoutInflater
        for (address in addresses) {
            val addressBlock =
                    inflater.inflate(R.layout.transaction_sent_from_addresses, null) as RelativeLayout
            val txFrom =
                    addressBlock.findViewById<View>(R.id.tx_from_text) as TextView
            val txFromDescription =
                    addressBlock.findViewById<View>(R.id.tx_from_description) as TextView
            //BRAnimator.showCopyBubble(activity, addressBlock, txFrom)
            if (address.isNotEmpty()) {
                txFrom.text = address
                txFromDescription.text = getString(R.string.utxo)
                view.addView(addressBlock)
            }
        }
    }

    private fun setSentToAddresses(
            view: LinearLayout,
            addresses: ArrayList<String?>,
            amounts: ArrayList<Long>
    ) {
        val inflater = requireActivity().layoutInflater
        val walletInteractor = WalletInteractor.getInstance()
        val wallet = walletInteractor.getBitcoinWallet()
        val txid = arguments?.getString("txid", "")
        val tx = wallet?.getTransaction(Sha256Hash.wrap(txid))
        for (i in addresses.indices) {
            val utxoIsMine = if (tx != null) {
                tx.outputs[i].isMine(wallet)
            } else {
                false
            }

            val addressBlock =
                    inflater.inflate(R.layout.transaction_sent_to_addresses, null) as RelativeLayout
            val txTo =
                    addressBlock.findViewById<View>(R.id.tx_to_text) as TextView
            val txToDescription =
                    addressBlock.findViewById<View>(R.id.tx_to_description) as TextView
            val txToAmount =
                    addressBlock.findViewById<View>(R.id.tx_to_amount_text) as TextView
            val txToExchange =
                    addressBlock.findViewById<View>(R.id.tx_to_exchange_text) as TextView
            if (addresses[i] != null && addresses[i]!!.isNotEmpty()) {
                txTo.text = addresses[i]
                if (addresses[i] == "OP_RETURN") {
                    txToDescription.text = getString(R.string.op_return_address)
                } else {
                    txToDescription.text = getString(R.string.payment_address)
                }
                val amountInBch = amounts[i] / 100000000.0

                txToAmount.text = if (utxoIsMine) {
                    resources.getString(
                        R.string.tx_amount_moved,
                        "${BalanceFormatter.formatBalance(amountInBch, "#.########")}"
                    )
                } else {
                    resources.getString(
                        R.string.tx_amount_moved,
                        "-${BalanceFormatter.formatBalance(amountInBch, "#.########")}"
                    )
                }

                val amountInFiat = amountInBch * PriceHelper.price
                txToExchange.text = if (utxoIsMine) {
                    "($${BalanceFormatter.formatBalance(amountInFiat, "0.00")})"
                } else {
                    "($-${BalanceFormatter.formatBalance(amountInFiat, "0.00")})"
                }

                if (utxoIsMine) {
                    txToAmount.setTextColor(resources.getColor(R.color.black))
                    txToExchange.setTextColor(resources.getColor(R.color.black))
                }

                view.addView(addressBlock)
            }
        }
    }
}