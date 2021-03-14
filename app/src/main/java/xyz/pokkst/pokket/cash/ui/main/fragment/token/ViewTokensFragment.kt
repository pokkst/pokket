package xyz.pokkst.pokket.cash.ui.main.fragment.token

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.bitcoinj.core.slp.SlpTokenBalance
import xyz.pokkst.pokket.cash.MainActivity
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.ui.NftListEntryView
import xyz.pokkst.pokket.cash.ui.NonScrollListView
import xyz.pokkst.pokket.cash.ui.SlpTokenListEntryView
import xyz.pokkst.pokket.cash.util.Constants
import xyz.pokkst.pokket.cash.wallet.WalletManager


/**
 * A placeholder fragment containing a simple view.
 */
class ViewTokensFragment : Fragment() {
    var root: View? = null
    private var srlSLP: SwipeRefreshLayout? = null
    private var slpList: NonScrollListView? = null
    private var nftList: NonScrollListView? = null
    private var slpCalculationJob: Job? = null
    private var nftCalculationJob: Job? = null

    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Constants.ACTION_MAIN_ENABLE_PAGER == intent.action) {
                this@ViewTokensFragment.findNavController()
                    .popBackStack(R.id.sendHomeFragment, false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                (activity as? MainActivity)?.toggleSendScreen(false)
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        root = inflater.inflate(R.layout.fragment_view_tokens, container, false)
        (activity as? MainActivity)?.enableTokensScreen()
        val filter = IntentFilter()
        filter.addAction(Constants.ACTION_MAIN_ENABLE_PAGER)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, filter)
        srlSLP = root?.findViewById(R.id.srlSLP)
        slpList = root?.findViewById(R.id.slpList)
        nftList = root?.findViewById(R.id.nftList)
        this.srlSLP?.setOnRefreshListener { this.refresh() }
        this.slpList?.setOnItemClickListener { parent, view, position, id ->
            val tokenBalance = WalletManager.walletKit?.slpBalances?.get(position)
            val tokenId = tokenBalance?.tokenId
            findNavController().navigate(
                ViewTokensFragmentDirections.navToSendFromViewTokens(
                    null,
                    tokenId
                )
            )
        }

        this.nftList?.setOnItemClickListener { parent, view, position, id ->
            val tokenBalance = WalletManager.walletKit?.nftBalances?.get(position)
            val tokenId = tokenBalance?.tokenId
            findNavController().navigate(
                    ViewTokensFragmentDirections.navToSendFromViewTokens(
                            null,
                            tokenId
                    )
            )
        }

        refresh()
        return root
    }

    override fun onDestroy() {
        super.onDestroy()
        slpCalculationJob?.cancel()
        nftCalculationJob?.cancel()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
    }

    private fun refresh() {
        slpCalculationJob = lifecycleScope.launch(Dispatchers.IO) {
            WalletManager.walletKit?.recalculateSlpUtxos()
            activity?.runOnUiThread {
                setSLPList()
            }
        }

        nftCalculationJob = lifecycleScope.launch(Dispatchers.IO) {
            WalletManager.walletKit?.recalculateNftUtxos()
            activity?.runOnUiThread {
                setNFTList()
            }
        }

        val srlSlp = srlSLP
        if (srlSlp != null && srlSlp.isRefreshing) srlSLP?.isRefreshing = false
    }

    private fun setSLPList() {
        val slpItems = WalletManager.walletKit?.slpBalances?.toList() ?: listOf()
        val slpAdapter = object : ArrayAdapter<SlpTokenBalance>(
            requireContext(),
            R.layout.token_list_cell,
                slpItems
        ) {
            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                return SlpTokenListEntryView.instanceOf(
                    activity,
                    position,
                    R.layout.token_list_cell
                )
            }
        }

        slpList?.adapter = slpAdapter
        slpList?.refreshDrawableState()

        if (slpItems.isEmpty()) {
            slpList?.visibility = View.GONE
            root?.findViewById<TextView>(R.id.slp_list_label)?.visibility = View.GONE
            root?.findViewById<TextView>(R.id.no_tokens_view)?.visibility = View.VISIBLE
        } else {
            slpList?.visibility = View.VISIBLE
            root?.findViewById<TextView>(R.id.slp_list_label)?.visibility = View.VISIBLE
            root?.findViewById<TextView>(R.id.no_tokens_view)?.visibility = View.GONE
        }

        root?.findViewById<TextView>(R.id.loading_slp_tokens_view)?.visibility = View.GONE
    }

    private fun setNFTList() {
        val nftItems = WalletManager.walletKit?.nftBalances?.toList() ?: listOf()
        val nftAdapter = object : ArrayAdapter<SlpTokenBalance>(
            requireContext(),
            R.layout.token_list_cell,
            nftItems
        ) {
            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                return NftListEntryView.instanceOf(
                    activity,
                    position,
                    R.layout.token_list_cell
                )
            }
        }

        nftList?.adapter = nftAdapter
        nftList?.refreshDrawableState()

        if (nftItems.isEmpty()) {
            nftList?.visibility = View.GONE
            root?.findViewById<TextView>(R.id.nft_list_label)?.visibility = View.GONE
            root?.findViewById<TextView>(R.id.no_nfts_view)?.visibility = View.VISIBLE
        } else {
            nftList?.visibility = View.VISIBLE
            root?.findViewById<TextView>(R.id.nft_list_label)?.visibility = View.VISIBLE
            root?.findViewById<TextView>(R.id.no_nfts_view)?.visibility = View.GONE
        }

        root?.findViewById<TextView>(R.id.loading_nfts_view)?.visibility = View.GONE

    }
}