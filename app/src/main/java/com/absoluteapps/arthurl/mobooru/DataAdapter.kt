package com.absoluteapps.arthurl.mobooru

import android.app.Activity
import android.graphics.Typeface
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

import com.etsy.android.grid.util.DynamicHeightImageView
import com.squareup.picasso.Picasso

class DataAdapter(internal var activity: Activity, internal var resource: Int, internal var datas: List<Data>, internal var showNsfw: Boolean = false, internal var showTitles: Boolean = true) : ArrayAdapter<Data>(activity, resource, datas) {
    internal var nsfwLogo = Uri.parse("drawable/nsfwlogo.png")

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
        if (data.ogSrc == null) {
            holder.description!!.text = ""
        } else {
            holder.description!!.text = if (data.series.length > 30) data.series.substring(0, 25) + "..." else data.series
        }
        if (data.thumbImgUrl !== "") {
            if (data.nsfw && (!showNsfw)!!) {
                holder.image!!.heightRatio = 1.0
                Picasso.get()
                        .load(R.drawable.nsfwlogo)
                        .resize(500, 580)
                        .into(holder.image)
            } else {
                val r = if (data.rat < 1) 1 / data.rat else data.rat
                holder.image!!.heightRatio = r
                Picasso.get()
                        .load(data.thumbImgUrl)
                        .resize(540, Math.floor(540 * r).toInt())   // set height programmatically
                        .centerCrop()
                        .transform(RoundedTransformation(20, 10))
                        .into(holder.image)

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
