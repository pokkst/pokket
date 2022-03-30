package xyz.pokkst.pokket.cash.ui.main.fragment.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_settings_node.view.*
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.util.Constants
import xyz.pokkst.pokket.cash.util.PrefsHelper


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
        val ip = PrefsHelper.instance(activity)?.getString(Constants.PREF_NODE_IP, null)
        root.the_ip.setText(ip)
        root.the_ip.doAfterTextChanged {
            val newIp = root.the_ip.text.toString()
            PrefsHelper.instance(activity)?.edit()?.putString(Constants.PREF_NODE_IP, newIp)?.apply()
        }
        return root
    }
}