package xyz.pokkst.pokket.cash.ui.main.fragment.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_new_user.view.*
import xyz.pokkst.pokket.cash.R

/**
 * A placeholder fragment containing a simple view.
 */
class NewUserFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_new_user, container, false)
        root.intro_new_wallet.setOnClickListener {
            findNavController().navigate(R.id.nav_to_new_wallet)
        }
        root.intro_recover_wallet.setOnClickListener {
            findNavController().navigate(R.id.nav_to_restore_wallet)
        }
        return root
    }
}