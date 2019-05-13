package com.absoluteapps.arthurl.mobooru;

import android.graphics.Color;
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
                return HelpFragment.newInstance(Color.parseColor("#03A9F4"), position); // blue
            default:
                return HelpFragment.newInstance(Color.parseColor("#4CAF50"), position); // green
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

}
