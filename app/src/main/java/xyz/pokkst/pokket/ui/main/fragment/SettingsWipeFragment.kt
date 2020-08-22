package xyz.pokkst.pokket.ui.main.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_settings_wipe.view.*
import xyz.pokkst.pokket.MainActivity
import xyz.pokkst.pokket.NewUserActivity
import xyz.pokkst.pokket.R
import xyz.pokkst.pokket.wallet.WalletManager
import java.io.File


/**
 * A placeholder fragment containing a simple view.
 */
class SettingsWipeFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_settings_wipe, container, false)
        root.continue_button.setOnClickListener {
            val seedEntered = root.editText_phrase.text.toString().trim()
            val walletSeed = WalletManager.walletKit?.wallet()?.keyChainSeed?.mnemonicString
            if(seedEntered == walletSeed) {
                val intent = Intent(requireContext(), NewUserActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                wipeAndRestart(intent)
            } else if(seedEntered.isNotEmpty()) {
                val intent = Intent(requireActivity(), MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("seed", seedEntered)
                intent.putExtra("new", false)
                wipeAndRestart(intent)
            }
        }
        return root
    }

    private fun wipeAndRestart(intent: Intent) {
        val walletFile = File(WalletManager.walletDir, "${WalletManager.walletFileName}.wallet")
        val spvChainFile = File(WalletManager.walletDir, "${WalletManager.walletFileName}.spvchain")
        walletFile.delete()
        spvChainFile.delete()
        startActivity(intent)
    }
}