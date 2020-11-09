package xyz.pokkst.pokket.ui.main.fragment.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_settings_phrase.view.*
import xyz.pokkst.pokket.R
import xyz.pokkst.pokket.wallet.WalletManager


/**
 * A placeholder fragment containing a simple view.
 */
class SettingsPhraseFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings_phrase, container, false)
        root.the_phrase.text = WalletManager.wallet?.keyChainSeed?.mnemonicString
        return root
    }
}