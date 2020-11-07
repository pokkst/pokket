package xyz.pokkst.pokket.ui.main.fragment

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.common.base.Splitter
import kotlinx.android.synthetic.main.fragment_intro_bg.view.*
import kotlinx.android.synthetic.main.fragment_new_wallet.view.*
import kotlinx.android.synthetic.main.fragment_restore_wallet.view.*
import kotlinx.android.synthetic.main.fragment_restore_wallet.view.multsig_checkbox
import xyz.pokkst.pokket.MainActivity
import xyz.pokkst.pokket.R

/**
 * A placeholder fragment containing a simple view.
 */
class RestoreWalletFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_restore_wallet, container, false)

        root.continue_button.setOnClickListener {
            val seedStr = root.recover_wallet_edit_text.text.toString().trim()
            val length = Splitter.on(' ').splitToList(seedStr).size
            if(length == 12) {
                val isMultisigChecked = root.multsig_checkbox.isChecked
                if(isMultisigChecked) {
                    val action = RestoreWalletFragmentDirections.navToMyFollowingKey(seedStr, true)
                    findNavController().navigate(action)
                    this.setStatusBarColor(requireActivity(), R.color.extra_light_grey)
                } else {
                    this.setStatusBarColor(requireActivity(), R.color.extra_light_grey)
                    val intent = Intent(requireActivity(), MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    println(seedStr)
                    intent.putExtra("seed", seedStr)
                    intent.putExtra("new", false)
                    startActivity(intent)
                }
            }
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