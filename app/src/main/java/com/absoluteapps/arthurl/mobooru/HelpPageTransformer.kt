package com.absoluteapps.arthurl.mobooru

import android.os.Build
import android.view.View
import androidx.viewpager.widget.ViewPager

class HelpPageTransformer : ViewPager.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        // Here you can do all kinds of stuff, like get the
        // width of the page and perform calculations based
        // on how far the user has swiped the page.
        val pageWidth = page.width
        val pageWidthTimesPosition = pageWidth * position
        val absPosition = Math.abs(position)

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
            val description = page.findViewById<View>(R.id.description)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                description.translationZ = pageWidthTimesPosition / 2f + 1f
            } else {
                description.translationX = pageWidthTimesPosition / 2f
            }
            description.alpha = 1.0f - absPosition
        }
    }
}
