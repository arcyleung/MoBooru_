package com.absoluteapps.arthurl.mobooru

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class HelpAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        when (position) {
            0 -> return HelpFragment.newInstance(position)
            1 -> return HelpFragment.newInstance(position)
            else -> return HelpFragment.newInstance(position)
        }
    }

    override fun getCount(): Int {
        return 3
    }

}
