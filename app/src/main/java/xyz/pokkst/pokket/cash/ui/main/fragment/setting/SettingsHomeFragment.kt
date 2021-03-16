package xyz.pokkst.pokket.cash.ui.main.fragment.setting

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_settings_home.view.*
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.slp.SlpOpReturn
import org.bitcoinj.wallet.Wallet
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.SettingsActivity
import xyz.pokkst.pokket.cash.ui.adapter.TransactionAdapter
import xyz.pokkst.pokket.cash.ui.listener.TxAdapterListener
import xyz.pokkst.pokket.cash.wallet.WalletManager


/**
 * A placeholder fragment containing a simple view.
 */
class SettingsHomeFragment : Fragment(), TxAdapterListener {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings_home, container, false)

        root.about.findViewById<RelativeLayout>(R.id.setting_layout).setOnClickListener {
            navigate(R.id.nav_to_about)
        }
        root.about.findViewById<TextView>(R.id.setting_label).text =
            resources.getString(R.string.about)

        root.recovery_phrase.findViewById<RelativeLayout>(R.id.setting_layout).setOnClickListener {
            navigate(R.id.nav_to_phrase)
        }
        root.recovery_phrase.findViewById<TextView>(R.id.setting_label).text =
            resources.getString(R.string.recovery_phrase_label)

        root.custom_node.findViewById<RelativeLayout>(R.id.setting_layout).setOnClickListener {
            navigate(R.id.nav_to_node)
        }
        root.custom_node.findViewById<TextView>(R.id.setting_label).text =
            resources.getString(R.string.node_label)

        root.extended_public_key.findViewById<RelativeLayout>(R.id.setting_layout)
            .setOnClickListener {
                navigate(R.id.nav_to_epk)
            }
        root.extended_public_key.findViewById<TextView>(R.id.setting_label).text =
            resources.getString(R.string.epk_label)

        root.shift_service.findViewById<RelativeLayout>(R.id.setting_layout).setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shift.pokket.cash/a/mmG1iwJRO"))
            startActivity(browserIntent)
        }
        root.shift_service.findViewById<TextView>(R.id.setting_label).text =
            resources.getString(R.string.shift_service_label)

        root.flipstarters.findViewById<RelativeLayout>(R.id.setting_layout).setOnClickListener {
            navigate(R.id.nav_to_flipstarters)
        }
        root.flipstarters.findViewById<TextView>(R.id.setting_label).text =
            resources.getString(R.string.flipstarter_label)

        root.start_recovery_wallet.setOnClickListener {
            navigate(R.id.nav_to_wipe)
        }

        setSyncStatus(root)

        setupTransactions(root, WalletManager.wallet)
        root.more_transactions.setOnClickListener {
            navigate(R.id.nav_to_tx_list)
        }

        return root
    }

    private fun setSyncStatus(root: View?) {
        val lastSeenBlockHeight = WalletManager.wallet?.lastBlockSeenHeight
        val bestBlockHeight = WalletManager.kit?.peerGroup()?.mostCommonChainHeight
        when {
            bestBlockHeight == 0 ->
                root?.sync_status?.text = resources.getString(R.string.not_syncing)
            bestBlockHeight != lastSeenBlockHeight ->
                root?.sync_status?.text = resources.getString(R.string.syncing)
            bestBlockHeight == lastSeenBlockHeight ->
                root?.sync_status?.text = resources.getString(R.string.synced)
        }
    }

    private fun navigate(navResId: Int) {
        (activity as? SettingsActivity)?.adjustDeepMenu(1)
        findNavController().navigate(navResId)
    }

    private fun setupTransactions(root: View, wallet: Wallet?) {
        var transactions = wallet?.getRecentTransactions(0, false)

        if (transactions?.isNotEmpty() == true) {
            if(transactions.size > 5) {
                root.more_transactions.visibility = View.VISIBLE
                transactions = transactions.subList(0, 5)
            }
            val itemsAdapter = TransactionAdapter(transactions)
            itemsAdapter.listener = this
            root.transactions_list.adapter = itemsAdapter
            root.transactions_list.layoutManager = LinearLayoutManager(context)
            root.transactions_list.isNestedScrollingEnabled = false
            root.no_transactions.visibility = View.GONE
        } else {
            root.space.visibility = View.GONE
            root.transactions_list.visibility = View.GONE
            root.no_transactions.visibility = View.VISIBLE
        }
    }

    override fun onClickTransaction(tx: Transaction) {
        (activity as? SettingsActivity)?.adjustDeepMenu(1)
        val txid = tx.txId.toString()
        val amount = tx.getValue(WalletManager.wallet)
        val isSlp = SlpOpReturn.isSlpTx(tx) || SlpOpReturn.isNftChildTx(tx)
        if (amount?.isPositive == true) {
            findNavController().navigate(
                    SettingsHomeFragmentDirections.navToTxReceived(
                            txid,
                            isSlp
                    )
            )
        } else if (amount?.isNegative == true) {
            findNavController().navigate(
                    SettingsHomeFragmentDirections.navToTxSent(
                            txid,
                            isSlp
                    )
            )
        }
    }
}