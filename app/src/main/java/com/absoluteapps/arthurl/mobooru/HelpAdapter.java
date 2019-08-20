package com.absoluteapps.arthurl.mobooru;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class HelpAdapter extends FragmentPagerAdapter {
    public HelpAdapter (FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return HelpFragment.newInstance(position);
            case 1:
                return HelpFragment.newInstance(position);
            default:
                return HelpFragment.newInstance(position);
        }
    }

    @Override
    public int getCount() {
        return 3;
    }

}
