package xyz.pokkst.pokket.cash.ui.main.fragment.setting

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.SimpleAdapter
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.android.synthetic.main.fragment_settings_flipstarters.view.*
import kotlinx.android.synthetic.main.fragment_settings_transactions.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionOutput
import org.bitcoinj.wallet.SendRequest
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.ui.PledgeEntryView
import xyz.pokkst.pokket.cash.wallet.WalletService


/**
 * A placeholder fragment containing a simple view.
 */
class SettingsFlipstartersFragment : Fragment() {
    private var pledges = ArrayList<TransactionOutput>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings_flipstarters, container, false)
        val frozenUtxos = WalletService.wallet?.unspents?.filter { it.isFrozen }
        root.pledge_list.setOnItemClickListener { parent, view, position, id ->
            val utxo = pledges[position]
            showCancelDialog(utxo)
        }
        val flipstarterPledges = ArrayList<TransactionOutput>()
        if (frozenUtxos != null) {
            for (utxo in frozenUtxos) {
                val parentTx = utxo.parentTransaction
                if (parentTx != null) {
                    if (parentTx.memo == "flipstarter_pledge") {
                        flipstarterPledges.add(utxo)
                    }
                }
            }
        }
        setListViewShit(root, flipstarterPledges)
        return root
    }

    private fun setListViewShit(root: View, pledges: ArrayList<TransactionOutput>) {
        lifecycleScope.launch(Dispatchers.IO) {
            val pledgesFormatted = ArrayList<Map<String, TransactionOutput>>()

            for (x in 0 until pledges.size) {
                val datum = HashMap<String, TransactionOutput>()
                datum["utxo"] = pledges[x]
                this@SettingsFlipstartersFragment.pledges.add(pledges[x])
                pledgesFormatted.add(datum)
            }

            val itemsAdapter = object : SimpleAdapter(
                requireContext(),
                pledgesFormatted,
                R.layout.pledge_item_cell,
                null,
                null
            ) {
                override fun getView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup
                ): View {
                    return PledgeEntryView.instanceOf(activity, position, pledgesFormatted)
                }
            }
            activity?.runOnUiThread {
                root.pledge_list.adapter = itemsAdapter
            }
        }
    }

    private fun showCancelDialog(utxo: TransactionOutput) {
        val dialog = activity?.let { Dialog(it) }
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog?.setContentView(R.layout.dialog_cancel_pledge)
        val cancelPledgeButton = dialog?.findViewById<Button>(R.id.cancel_pledge_button)
        val closeButton = dialog?.findViewById<TextView>(R.id.pledge_close)
        cancelPledgeButton?.setOnClickListener {
            val cancelReq = SendRequest.cancelFlipstarterPledge(WalletService.wallet, utxo)
            val sendResult = WalletService.wallet?.sendCoins(cancelReq)
            Futures.addCallback(
                sendResult?.broadcastComplete,
                object : FutureCallback<Transaction?> {
                    override fun onSuccess(@Nullable result: Transaction?) {
                        dialog.dismiss()
                    }

                    override fun onFailure(t: Throwable) { // We died trying to empty the wallet.

                    }
                },
                MoreExecutors.directExecutor()
            )
        }
        closeButton?.setOnClickListener {
            dialog.dismiss()
        }
        dialog?.show()
    }
}