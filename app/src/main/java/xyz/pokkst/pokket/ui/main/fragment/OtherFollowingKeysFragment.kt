package xyz.pokkst.pokket.ui.main.fragment

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_generated_seed.view.*
import kotlinx.android.synthetic.main.fragment_generated_seed.view.back_button
import kotlinx.android.synthetic.main.fragment_generated_seed.view.continue_button
import kotlinx.android.synthetic.main.fragment_other_following_keys.view.*
import org.bitcoinj.crypto.MnemonicCode
import org.bitcoinj.crypto.MnemonicException
import org.bitcoinj.wallet.DeterministicSeed
import xyz.pokkst.pokket.MainActivity
import xyz.pokkst.pokket.R
import java.security.SecureRandom


/**
 * A placeholder fragment containing a simple view.
 */
class OtherFollowingKeysFragment : Fragment() {
    val args: OtherFollowingKeysFragmentArgs by navArgs()
    var nPrevious = 0
    var nCurrent = 0
    var mCurrent = 0
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_other_following_keys, container, false)
        val seed = args.seed
        val restoring = args.restoring
        val cosignerInflater = activity?.layoutInflater

        val cosignerKeysList = root.cosigner_keys_layout

        root.n_edittext.doAfterTextChanged { text ->
            val n = if(text.isNullOrEmpty()) 0 else text.toString().toInt()
            if(n != nPrevious) {
                val difference = n - nPrevious
                if(difference > 0) {
                    val fixedDifference = difference - 1
                    for(x in 0..fixedDifference) {
                        val cosignerKeyInputLayout =
                            cosignerInflater?.inflate(R.layout.fragment_component_cosigner_entry, null) as ConstraintLayout
                        cosignerKeysList.addView(cosignerKeyInputLayout)
                    }
                } else {
                    val differenceAbsolute = difference * -1
                    for(x in 0..differenceAbsolute) {
                        val length = cosignerKeysList.childCount
                        val viewToRemove = cosignerKeysList.getChildAt(length - 1)
                        cosignerKeysList.removeView(viewToRemove)
                    }
                }
                nPrevious = n
            }

            nCurrent = n

            root.m_of_n_textview.text = resources.getString(R.string.you_are_creating_a_m_of_n_multisig_wallet, mCurrent, nCurrent + 1)
        }

        root.m_edittext.doAfterTextChanged { text ->
            val m = if(text.isNullOrEmpty()) 0 else text.toString().toInt()
            mCurrent = m

            root.m_of_n_textview.text = resources.getString(R.string.you_are_creating_a_m_of_n_multisig_wallet, mCurrent, nCurrent + 1)
        }

        root.m_of_n_textview.text = resources.getString(R.string.you_are_creating_a_m_of_n_multisig_wallet, mCurrent, nCurrent)

        root.continue_button.setOnClickListener {
            if(mCurrent != 0 && nCurrent != 0 && nCurrent < mCurrent) {
                val followingKeys = ArrayList<String>()
                val keysLength = cosignerKeysList.childCount - 1
                for (x in 0..keysLength) {
                    val keyEntryLayout = cosignerKeysList.getChildAt(x) as ConstraintLayout
                    val editText = keyEntryLayout.findViewById<EditText>(R.id.cosigner_edittext)
                    val key = editText.text.toString()
                    followingKeys.add(key)
                }

                val intent = Intent(requireActivity(), MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("seed", seed)
                intent.putExtra("new", !restoring)
                intent.putExtra("multisig", true)
                intent.putExtra("followingKeys", followingKeys)
                intent.putExtra("m", mCurrent)
                startActivity(intent)
            }
        }

        root.back_button.setOnClickListener {
            findNavController().popBackStack()
        }

        return root
    }
}