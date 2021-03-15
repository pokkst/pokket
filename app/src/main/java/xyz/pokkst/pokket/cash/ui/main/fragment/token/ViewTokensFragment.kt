package xyz.pokkst.pokket.cash.ui.main.fragment.token

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.bitcoinj.core.slp.SlpTokenBalance
import xyz.pokkst.pokket.cash.MainActivity
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.ui.adapter.NftAdapter
import xyz.pokkst.pokket.cash.ui.adapter.SlpAdapter
import xyz.pokkst.pokket.cash.ui.adapter.TokenHeaderAdapter
import xyz.pokkst.pokket.cash.ui.listener.SlpAdapterListener
import xyz.pokkst.pokket.cash.util.Constants
import xyz.pokkst.pokket.cash.util.Toaster
import xyz.pokkst.pokket.cash.wallet.WalletManager


/**
 * A placeholder fragment containing a simple view.
 */
class ViewTokensFragment : Fragment(), SlpAdapterListener {
    var root: View? = null
    var sendingAddress: String? = null
    private var tokenList: RecyclerView? = null
    private var calculationJob: Job? = null

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
        tokenList = root?.findViewById(R.id.tokenList)
        refresh()
        sendingAddress = arguments?.getString("address")
        return root
    }

    override fun onDestroy() {
        super.onDestroy()
        calculationJob?.cancel()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
    }

    private fun refresh() {
        calculationJob = lifecycleScope.launch(Dispatchers.IO) {
            WalletManager.walletKit?.recalculateSlpUtxos()
            WalletManager.walletKit?.recalculateNftUtxos()
            activity?.runOnUiThread {
                setList()
            }
        }
    }

    private fun setList() {
        val slpHeader = TokenHeaderAdapter("slp tokens")
        val slpItems = WalletManager.walletKit?.slpBalances?.toList() ?: listOf()
        val slpAdapter = SlpAdapter(slpItems)
        slpAdapter.listener = this
        slpHeader.isVisible = slpItems.isNotEmpty()

        val nftHeader = TokenHeaderAdapter("NFTs")
        val nftItems = WalletManager.walletKit?.nftBalances?.toList() ?: listOf()
        val nftAdapter = NftAdapter(nftItems)
        nftAdapter.listener = this
        nftHeader.isVisible = nftItems.isNotEmpty()

        val concatAdapter = ConcatAdapter(slpHeader, slpAdapter, nftHeader, nftAdapter)
        tokenList?.adapter = concatAdapter
        tokenList?.layoutManager = LinearLayoutManager(context)

        root?.findViewById<TextView>(R.id.loading_slp_tokens_view)?.visibility = View.GONE
    }

    override fun onClickToken(slpTokenBalance: SlpTokenBalance) {
        findNavController().navigate(
                ViewTokensFragmentDirections.navToSendFromViewTokens(
                        sendingAddress,
                        slpTokenBalance.tokenId
                )
        )
    }
}