package xyz.pokkst.pokket.cash.ui.main.fragment.multisig

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_generated_seed.view.back_button
import kotlinx.android.synthetic.main.fragment_generated_seed.view.continue_button
import kotlinx.android.synthetic.main.fragment_other_following_keys.view.*
import xyz.pokkst.pokket.cash.MainActivity
import xyz.pokkst.pokket.cash.R


/**
 * A placeholder fragment containing a simple view.
 */
class OtherFollowingKeysFragment : Fragment() {
    val args: OtherFollowingKeysFragmentArgs by navArgs()
    var nPrevious = 0
    var nCurrent = 0
    var mCurrent = 0
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_other_following_keys, container, false)
        val seed = args.seed
        val restoring = args.restoring
        val cosignerInflater = activity?.layoutInflater

        val cosignerKeysList = root.cosigner_keys_layout

        root.n_edittext.doAfterTextChanged { text ->
            var n = if (text.isNullOrEmpty()) 0 else text.toString().toInt()
            if (n > 9) {
                n = 9
                root.n_edittext.setText("9")
            }

            if (n != nPrevious) {
                val difference = n - nPrevious
                if (difference > 0) {
                    val fixedDifference = difference - 2
                    for (x in 0..fixedDifference) {
                        val cosignerKeyInputLayout =
                                cosignerInflater?.inflate(
                                        R.layout.fragment_component_cosigner_entry,
                                        null
                                ) as ConstraintLayout
                        cosignerKeysList.addView(cosignerKeyInputLayout)
                    }
                } else {
                    val differenceAbsolute = difference * -1
                    for (x in 0..differenceAbsolute) {
                        val length = cosignerKeysList.childCount
                        val viewToRemove = cosignerKeysList.getChildAt(length - 1)
                        cosignerKeysList.removeView(viewToRemove)
                    }
                }
                nPrevious = n
            }

            nCurrent = n

            root.m_of_n_textview.text = resources.getString(
                    R.string.you_are_creating_a_m_of_n_multisig_wallet,
                    mCurrent,
                    nCurrent
            )
        }

        root.m_edittext.doAfterTextChanged { text ->
            var m = if (text.isNullOrEmpty()) 0 else text.toString().toInt()
            if (m > 8) {
                m = 8
                root.m_edittext.setText("8")
            }

            mCurrent = m

            root.m_of_n_textview.text = resources.getString(
                    R.string.you_are_creating_a_m_of_n_multisig_wallet,
                    mCurrent,
                    nCurrent
            )
        }

        root.m_of_n_textview.text = resources.getString(
                R.string.you_are_creating_a_m_of_n_multisig_wallet,
                mCurrent,
                nCurrent
        )

        root.continue_button.setOnClickListener {
            if (mCurrent != 0 && nCurrent != 0 && mCurrent <= nCurrent) {
                val followingKeys = ArrayList<String>()
                val keysLength = cosignerKeysList.childCount - 1
                for (x in 0..keysLength) {
                    val keyEntryLayout = cosignerKeysList.getChildAt(x) as ConstraintLayout
                    val editText = keyEntryLayout.findViewById<EditText>(R.id.cosigner_edittext)
                    val key = editText.text.toString()
                    followingKeys.add(key)
                }

                val keysEntered = followingKeys.none { it.isEmpty() }
                if (keysEntered) {
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
        }

        root.back_button.setOnClickListener {
            findNavController().popBackStack()
        }

        return root
    }
}