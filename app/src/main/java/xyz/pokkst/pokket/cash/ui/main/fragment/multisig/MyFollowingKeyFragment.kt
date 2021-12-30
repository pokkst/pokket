package xyz.pokkst.pokket.cash.ui.main.fragment.multisig

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_my_following_key.view.*
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.Wallet
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.util.ClipboardHelper
import xyz.pokkst.pokket.cash.wallet.WalletService

/**
 * A placeholder fragment containing a simple view.
 */
class MyFollowingKeyFragment : Fragment() {
    val args: MyFollowingKeyFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_my_following_key, container, false)
        val seed = args.seed
        val restoring = args.restoring
        val tempWallet =
            Wallet.fromSeed(WalletService.parameters, DeterministicSeed(seed, null, "", 0))
        val xpub = tempWallet.watchingKey.serializePubB58(WalletService.parameters)

        root.the_phrase.text = xpub

        root.the_phrase.setOnClickListener {
            ClipboardHelper.copyToClipboard(activity, xpub)
        }

        root.continue_button.setOnClickListener {
            val action =
                MyFollowingKeyFragmentDirections.navToOtherFollowingKeys(seed, xpub, restoring)
            findNavController().navigate(action)
        }

        root.back_button.setOnClickListener {
            findNavController().popBackStack()
        }

        return root
    }
}