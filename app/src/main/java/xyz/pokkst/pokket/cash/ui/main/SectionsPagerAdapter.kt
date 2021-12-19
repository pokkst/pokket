package xyz.pokkst.pokket.cash.ui.main

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import xyz.pokkst.pokket.cash.ui.main.fragment.receive.ReceiveFragment
import xyz.pokkst.pokket.cash.ui.main.fragment.send.SendHomeFragment

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(private val context: Context, fm: FragmentManager) :
    FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        return if (position == 0) {
            SendHomeFragment()
        } else {
            ReceiveFragment()
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return ""
    }

    override fun getCount(): Int {
        // Show 2 total pages.
        return 2
    }
}