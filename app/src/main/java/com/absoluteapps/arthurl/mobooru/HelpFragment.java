package com.absoluteapps.arthurl.mobooru;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class HelpFragment extends Fragment{
    private static final String PAGE = "page";

    private int mPage;

    public static HelpFragment newInstance(int page) {
        HelpFragment frag = new HelpFragment();
        Bundle b = new Bundle();
        b.putInt(PAGE, page);
        frag.setArguments(b);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getArguments().containsKey(PAGE))
            throw new RuntimeException("Fragment must contain a \"" + PAGE + "\" argument!");
        mPage = getArguments().getInt(PAGE);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Select a layout based on the current page
        int layoutResId;
        int file;
        switch (mPage) {
            case 0:
                layoutResId = R.layout.help_1;
                file = R.drawable.help1;
                break;
            case 1:
                layoutResId = R.layout.help_2;
                file = R.drawable.help2;
                break;
            default:
                layoutResId = R.layout.help_3;
                file = R.drawable.help3;
                break;
        }

        // Inflate the layout resource file
        View view = getActivity().getLayoutInflater().inflate(layoutResId, container, false);
        View bg = view.findViewById(R.id.intro_background);
        // Set the current page index as the View's tag (useful in the PageTransformer)
        view.setTag(mPage);
        bg.setBackgroundColor(Color.parseColor("#5c5c5c"));

        ImageView iv;
        iv = view.findViewById(R.id.imageView);
        iv.setImageResource(file);
        return view;
    }
}
