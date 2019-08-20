package com.absoluteapps.arthurl.mobooru;

import android.support.v4.view.ViewPager;
import android.view.View;

public class HelpPageTransformer implements ViewPager.PageTransformer {
    @Override
    public void transformPage(View page, float position) {
        // Here you can do all kinds of stuff, like get the
        // width of the page and perform calculations based
        // on how far the user has swiped the page.
        int pageWidth = page.getWidth();
        float pageWidthTimesPosition = pageWidth * position;
        float absPosition = Math.abs(position);

        // Now it's time for the effects
        if (position <= -1.0f || position >= 1.0f) {

            // The page is not visible. This is a good place to stop
            // any potential work / animations you may have running.

        } else if (position == 0.0f) {

            // The page is selected. This is a good time to reset Views
            // after animations as you can't always count on the PageTransformer
            // callbacks to match up perfectly.

        } else {

            // Now the description. We also want this one to
            // fade, but the animation should also slowly move
            // down and out of the screen
            View description = page.findViewById(R.id.description);
            description.setTranslationY(-pageWidthTimesPosition / 2f);
            description.setAlpha(1.0f - absPosition);
        }
    }
}
