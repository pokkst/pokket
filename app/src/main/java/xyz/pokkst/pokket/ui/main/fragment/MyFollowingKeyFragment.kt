package xyz.pokkst.pokket.ui.main.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import xyz.pokkst.pokket.R
import xyz.pokkst.pokket.util.Toaster
import xyz.pokkst.pokket.wallet.WalletManager

/**
 * A placeholder fragment containing a simple view.
 */
class MyFollowingKeyFragment : Fragment() {
    val args: MyFollowingKeyFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_my_following_key, container, false)
        val seed = args.seed
        val restoring = args.restoring
        val tempWallet = Wallet.fromSeed(WalletManager.parameters, DeterministicSeed(seed, null, "", 0))
        val xpub = tempWallet.watchingKey.serializePubB58(WalletManager.parameters)

        root.the_phrase.text = xpub

        root.the_phrase.setOnClickListener {
            copyToClipboard(xpub)
        }

        root.continue_button.setOnClickListener {
            val action = MyFollowingKeyFragmentDirections.navToOtherFollowingKeys(seed, xpub, restoring)
            findNavController().navigate(action)
        }

        root.back_button.setOnClickListener {
            findNavController().popBackStack()
        }

        return root
    }

    private fun copyToClipboard(text: String) {
        val clipboard: ClipboardManager? = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText("Key", text)
        clipboard?.setPrimaryClip(clip)
        Toaster.showToastMessage(requireContext(), "copied")
    }
}