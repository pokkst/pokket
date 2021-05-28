package xyz.pokkst.pokket.cash.ui.main.fragment.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_settings_chain_privacy.view.*
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.util.PrefsHelper


/**
 * A placeholder fragment containing a simple view.
 */
class SettingsChainPrivacyFragment : Fragment() {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings_chain_privacy, container, false)
        val privateMode = PrefsHelper.instance(activity)?.getBoolean("private_mode", false) ?: false
        root.chain_privacy_switch.isChecked = privateMode
        root.chain_privacy_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            PrefsHelper.instance(activity)?.edit()?.putBoolean("private_mode", isChecked)?.apply()
        }
        return root
    }
}