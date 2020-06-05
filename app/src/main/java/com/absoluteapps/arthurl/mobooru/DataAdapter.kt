package com.absoluteapps.arthurl.mobooru

import android.app.Activity
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions

import com.etsy.android.grid.util.DynamicHeightImageView
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

class DataAdapter(internal var activity: Activity, internal var resource: Int, internal var datas: List<Data>, internal var showNsfw: Boolean = false, internal var showTitles: Boolean = true) : ArrayAdapter<Data>(activity, resource, datas) {
    private val regOpts = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .transform(MultiTransformation(CenterCrop(), RoundedCornersTransformation(20,10)))

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var row = convertView
        val holder: DealHolder

        if (row == null) {
            val inflater = activity.layoutInflater
            row = inflater.inflate(resource, parent, false)

            holder = DealHolder()
            holder.image = row!!.findViewById<View>(R.id.image) as DynamicHeightImageView
            holder.title = row.findViewById<View>(R.id.title) as TextView
            holder.title!!.visibility = if (showTitles) View.VISIBLE else View.GONE
            holder.description = row.findViewById<View>(R.id.description) as TextView
            holder.description!!.visibility = if (showTitles) View.VISIBLE else View.GONE
            holder.score = row.findViewById<View>(R.id.score) as TextView
            holder.score!!.visibility = if (showTitles) View.VISIBLE else View.GONE

            row.tag = holder
        } else {
            holder = row.tag as DealHolder
        }

        val data = datas[position]

        holder.title!!.text = if (data.title.length > 35) data.title.substring(0, 30) + "..." else data.title
        holder.score!!.text = data.score
        holder.score!!.setTypeface(null, Typeface.BOLD_ITALIC)
        holder.description!!.text = if (data.series.length > 30) data.series.substring(0, 25) + "..." else data.series

        if (data.thumbImgUrl !== "") {
            val r = if (data.rat < 1) 1 / data.rat else data.rat

            if (data.nsfw && !showNsfw) {
                holder.image!!.heightRatio = r
                Glide.with(activity)
                        .load(data.thumbImgUrl)
                        .override(540, Math.floor(540 * r).toInt())   // set height programmatically
                        .apply(RequestOptions().transform(MultiTransformation(BlurTransformation(25, 3), CenterCrop(), RoundedCornersTransformation(20,10))))
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(holder.image!!)
            } else {
                holder.image!!.heightRatio = r
                Glide.with(activity)
                        .load(data.thumbImgUrl)
                        .override(540, Math.floor(540 * r).toInt())   // set height programmatically
                        .apply(regOpts)
                        .into(holder.image!!)
            }
        }
        return row
    }

    internal class DealHolder {
        var image: DynamicHeightImageView? = null
        var title: TextView? = null
        var score: TextView? = null
        var description: TextView? = null
    }
}
