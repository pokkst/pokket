package xyz.pokkst.pokket.cash.ui.main.fragment.setting

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_settings_wipe.view.*
import xyz.pokkst.pokket.cash.NewUserActivity
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.wallet.WalletManager
import java.io.File


/**
 * A placeholder fragment containing a simple view.
 */
class SettingsWipeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings_wipe, container, false)
        root.continue_button.setOnClickListener {
            val seedEntered = root.editText_phrase.text.toString().trim()
            val walletSeed = WalletManager.wallet?.keyChainSeed?.mnemonicString
            if (seedEntered == walletSeed) {
                WalletManager.stopWallets()
                val intent = Intent(requireContext(), NewUserActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                wipeAndRestart(intent)
            }
        }
        return root
    }

    private fun wipeAndRestart(intent: Intent) {
        val walletFile = File(WalletManager.walletDir, "${WalletManager.walletFileName}.wallet")
        val multisigWalletFile =
            File(WalletManager.walletDir, "${WalletManager.multisigWalletFileName}.wallet")
        val spvChainFile = File(WalletManager.walletDir, "${WalletManager.walletFileName}.spvchain")
        val multisigSpvChainFile =
            File(WalletManager.walletDir, "${WalletManager.multisigWalletFileName}.spvchain")
        walletFile.delete()
        multisigWalletFile.delete()
        spvChainFile.delete()
        multisigSpvChainFile.delete()
        startActivity(intent)
    }
}