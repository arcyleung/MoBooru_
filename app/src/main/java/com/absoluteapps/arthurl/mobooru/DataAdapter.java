package com.absoluteapps.arthurl.mobooru;

import android.app.Activity;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.etsy.android.grid.util.DynamicHeightImageView;
import com.squareup.picasso.Picasso;

import java.util.List;

public class DataAdapter extends ArrayAdapter<Data> {

    Activity activity;
    int resource;
    List<Data> datas;
    Boolean showNsfw;
    Uri nsfwLogo = Uri.parse("drawable/nsfwlogo.png");

    public DataAdapter(Activity activity, int resource, List<Data> objects, Boolean shwNsfw) {
        super(activity, resource, objects);

        this.activity = activity;
        this.resource = resource;
        this.datas = objects;
        this.showNsfw = shwNsfw;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        final DealHolder holder;

        if (row == null) {
            LayoutInflater inflater = activity.getLayoutInflater();
            row = inflater.inflate(resource, parent, false);

            holder = new DealHolder();
            holder.image = (DynamicHeightImageView) row.findViewById(R.id.image);
            holder.title = (TextView) row.findViewById(R.id.title);
            holder.description = (TextView) row.findViewById(R.id.description);
            holder.score = (TextView) row.findViewById(R.id.score);

            row.setTag(holder);
        } else {
            holder = (DealHolder) row.getTag();
        }

        final Data data = datas.get(position);

        holder.title.setText(data.title.length() > 35 ? data.title.substring(0, 30) + "..." : data.title);
        holder.score.setText(data.score + "🔺");
        if (data.ogSrc == null) {
            holder.description.setText("");
        } else {
            holder.description.setText(data.series.length() > 30 ? data.series.substring(0, 25) + "..." : data.series);
        }
        if (data.thumbImgUrl != ""){
            if (data.nsfw && !showNsfw) {
                holder.image.setHeightRatio(1);
                Picasso.get()
                        .load(R.drawable.nsfwlogo)
                        .resize(500,580)
                        .into(holder.image);
            } else {
                double r = data.rat < 1 ? 1/data.rat : data.rat;
                holder.image.setHeightRatio(r);
                Picasso.get()
                        .load(data.thumbImgUrl)
                        .resize(540, (int) Math.floor(540*r))   // set height programmatically
                        .centerCrop()
                        .transform(new RoundedTransformation(20, 10))
                        .into(holder.image);

            }
        } else {
//            Picasso.with(this.getContext())
//                    .load(new File("drawable/404_notfound.jpg"))
//                    .transform(new RoundedTransformation(20, 10))
//                    .into(holder.image);
        }
        return row;
    }

    static class DealHolder {
        DynamicHeightImageView image;
        TextView title;
        TextView score;
        TextView description;
    }
}
