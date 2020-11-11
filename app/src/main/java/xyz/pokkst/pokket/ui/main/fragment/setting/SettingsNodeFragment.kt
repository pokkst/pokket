package xyz.pokkst.pokket.ui.main.fragment.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_settings_epk.view.*
import kotlinx.android.synthetic.main.fragment_settings_node.view.*
import kotlinx.android.synthetic.main.fragment_settings_phrase.view.*
import xyz.pokkst.pokket.R
import xyz.pokkst.pokket.util.ClipboardHelper
import xyz.pokkst.pokket.util.PrefsHelper
import xyz.pokkst.pokket.wallet.WalletManager


/**
 * A placeholder fragment containing a simple view.
 */
class SettingsNodeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings_node, container, false)
        val ip = PrefsHelper.instance(activity)?.getString("node_ip", null)
        root.the_ip.setText(ip)
        root.the_ip.doAfterTextChanged {
            val newIp = root.the_ip.text.toString()
            PrefsHelper.instance(activity)?.edit()?.putString("node_ip", newIp)?.apply()
        }
        return root
    }
}