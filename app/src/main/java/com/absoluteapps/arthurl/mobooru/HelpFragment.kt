package com.absoluteapps.arthurl.mobooru

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

class HelpFragment : Fragment() {

    private var mPage: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!arguments!!.containsKey(PAGE))
            throw RuntimeException("Fragment must contain a \"$PAGE\" argument!")
        mPage = arguments!!.getInt(PAGE)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        // Select a layout based on the current page
        val layoutResId: Int
        val file: Int
        when (mPage) {
            0 -> {
                layoutResId = R.layout.help_1
                file = R.drawable.help1
            }
            1 -> {
                layoutResId = R.layout.help_2
                file = R.drawable.help2
            }
            else -> {
                layoutResId = R.layout.help_3
                file = R.drawable.help3
            }
        }

        // Inflate the layout resource file
        val view = activity!!.layoutInflater.inflate(layoutResId, container, false)
        val bg = view.findViewById<View>(R.id.intro_background)
        // Set the current page index as the View's tag (useful in the PageTransformer)
        view.tag = mPage
        bg.setBackgroundColor(Color.parseColor("#5c5c5c"))

        val iv: ImageView
        iv = view.findViewById(R.id.imageView)
        iv.setImageResource(file)
        return view
    }

    companion object {
        private val PAGE = "page"

        fun newInstance(page: Int): HelpFragment {
            val frag = HelpFragment()
            val b = Bundle()
            b.putInt(PAGE, page)
            frag.arguments = b
            return frag
        }
    }
}
