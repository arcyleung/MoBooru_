package com.absoluteapps.arthurl.mobooru

import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.google.android.material.snackbar.Snackbar

class Help : AppCompatActivity() {

    private var mViewPager: ViewPager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFormat(PixelFormat.TRANSLUCENT)
        setContentView(R.layout.help_page)

        mViewPager = findViewById<View>(R.id.viewpager) as ViewPager

        // Set an Adapter on the ViewPager
        mViewPager!!.adapter = HelpAdapter(supportFragmentManager)

        // Set a PageTransformer
        mViewPager!!.setPageTransformer(false, HelpPageTransformer())

        val snackbar = Snackbar
                .make(mViewPager!!, "", Snackbar.LENGTH_INDEFINITE)
                .setAction("DONE") { finish() }
        snackbar.view.setBackgroundColor(Color.parseColor("#cc434343"))
        snackbar.show()
    }

}
