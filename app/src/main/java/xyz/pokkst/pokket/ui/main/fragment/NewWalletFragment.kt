package xyz.pokkst.pokket.ui.main.fragment

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_intro_bg.view.*
import kotlinx.android.synthetic.main.fragment_new_wallet.view.*
import kotlinx.android.synthetic.main.intro_fragment_warning.view.*
import xyz.pokkst.pokket.R

/**
 * A placeholder fragment containing a simple view.
 */
class NewWalletFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_new_wallet, container, false)
        root.intro_new_wallet_generate.setOnClickListener {
            //val username = root.new_cash_acct_name.text.toString()
            //if(username.isNotEmpty()) {
                if (root.seed_warning_screen.visibility != View.VISIBLE) {
                    this.setStatusBarColor(requireActivity(), R.color.purple_dark)
                    root.seed_warning_screen.visibility = View.VISIBLE
                }
            //}
        }

        root.intro_warning_show_button.setOnClickListener {
            findNavController().navigate(R.id.nav_to_generated_seed)
            this.setStatusBarColor(requireActivity(), R.color.extra_light_grey)
        }

        root.intro_left_button.setOnClickListener {
            findNavController().popBackStack()
        }

        return root
    }

    fun setStatusBarColor(activity: Activity, color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = activity.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = activity.resources.getColor(color)
        }
    }
}