package com.absoluteapps.arthurl.mobooru;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class SubSelector extends AppCompatActivity {

    List<Sub> origList;
    List<Sub> subsList;
    HashMap<Integer, Sub> subsMap;
    HashSet<Integer> selectedSubs;
    CustomAdapter adp = null;
    Gson gson = new Gson();
    Button mClearText;
    EditText mEditText;
    Type hashSetMap = new TypeToken<HashSet<Integer>>(){}.getType();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_subs);
        mEditText = (EditText) findViewById(R.id.search);
        mClearText = (Button) findViewById(R.id.clearText);

        //initially clear button is invisible
        mClearText.setVisibility(View.INVISIBLE);

        //clear button visibility on text change
        mEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                //do nothing
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() != 0) {
                    mClearText.setVisibility(View.VISIBLE);
                } else {
                    mClearText.setVisibility(View.GONE);
                }
            }
        });

        // List of subs
        subsMap = (HashMap<Integer, Sub>) getIntent().getSerializableExtra("subsList");
        origList = new ArrayList(subsMap.values());

        // Checked favorite subs <sub_id, checked>
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

//        if (prefs.getString("SELECTED_SUBS", "["+R.string.defaultsub+"]").equals("["+R.string.defaultsub+"]")){
//
//            // Set selectedString sub to awwnime only
////            selectedSubs = new HashMap<Integer, Boolean>();
////            for (int i = 0; i < subsList.size(); i++){
////                favoriteSubs.put(subsList.get(i).subID, false);
////            }
//            selectedSubs.add(1);
//        } else {
            try {
                selectedSubs = gson.fromJson(prefs.getString("SELECTED_SUBS", "["+R.string.defaultsub+"]"), hashSetMap);
                for (int id: selectedSubs){
                        subsMap.get(id).selected = true;
                }
            }
            catch (Exception ex){
                System.out.println("[DBG] failed deserialize " +ex);
                System.out.println(gson.toJson(subsList));
                // Set selectedString sub to awwnime only
                selectedSubs = new HashSet<Integer>();
//                for (int i = 0; i < subsList.size(); i++){
//                    favoriteSubs.put(subsList.get(i).subID, false);
//                }
                selectedSubs.add(1);
            }
            subsList = new ArrayList<>(subsMap.values());
//        }
        displayList();
        buttonPressed();
    }

    public void clear(View view) {
        mEditText.setText("");
        mClearText.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onBackPressed() {
        finish();
        overridePendingTransition(R.transition.fade_in, R.transition.fade_out);
        startActivity(new Intent(this, Main.class));
        overridePendingTransition(R.transition.fade_in, R.transition.fade_out);
    }

    private void displayList() {
        Collections.sort(subsList);
        adp = new CustomAdapter(this, R.layout.activity_settings_subs_checkboxes, new ArrayList<Sub>(subsList));
        ListView lv = (ListView) findViewById(R.id.listView);
        lv.setAdapter(adp);

        // Locate the EditText in listview_main.xml
        final EditText editsearch = (EditText) findViewById(R.id.search);

        // Capture Text in EditText
        editsearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {
                // TODO Auto-generated method stub
                String text = editsearch.getText().toString().toLowerCase(Locale.getDefault());
                adp.filter(text);
                Collections.sort(subsList);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2,
                                      int arg3) {
                // TODO Auto-generated method stub
            }
        });

        Toolbar subs_toolbar = (Toolbar) findViewById(R.id.subs_toolbar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            subs_toolbar.setTitle("Subreddits");
        }
        setSupportActionBar(subs_toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

//        subs_toolbar.setNavigationIcon(R.drawable.);
        subs_toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(R.transition.fade_in, R.transition.fade_out);
                startActivity(new Intent(SubSelector.this, Main.class));
                overridePendingTransition(R.transition.fade_in, R.transition.fade_out);
            }
        });

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int p, long id) {
                Sub sub = (Sub) parent.getItemAtPosition(p);
                ((Sub) parent.getItemAtPosition(p)).selected = !((Sub) parent.getItemAtPosition(p)).selected;
                CheckBox cb = (CheckBox) view.findViewById(R.id.checkBox);
                boolean isSelected = selectedSubs.contains(sub.subID);
                if (isSelected) {
                    System.out.println("[DBG] removing " +sub.subID);
                    selectedSubs.remove(sub.subID);
                } else {
                    System.out.println("[DBG] adding " +sub.subID);
                    selectedSubs.add(sub.subID);
                }
                cb.setChecked(!isSelected);
                sub.selected = (!isSelected);
                Collections.sort(subsList);
                adp.notifyDataSetChanged();
            }
        });
    }

    private class CustomAdapter extends ArrayAdapter<Sub> {

        private ArrayList<Sub> subsList;

        public CustomAdapter(Context context, int textViewResourceId, ArrayList<Sub> subsList) {
            super(context, textViewResourceId, subsList);
            this.subsList = subsList;
        }

        private class ViewHolder {
            CheckBox name;
            TextView isNSFW;
            TextView subscribers;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            ViewHolder holder = null;

            if (convertView == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = vi.inflate(R.layout.activity_settings_subs_checkboxes, parent, false);

                holder = new ViewHolder();
                holder.name = (CheckBox) convertView.findViewById(R.id.checkBox);
                holder.isNSFW = (TextView) convertView.findViewById(R.id.isNSFW);
                holder.subscribers = (TextView) convertView.findViewById(R.id.subscribers);
                convertView.setTag(holder);
            }
            else {
                holder = (ViewHolder) convertView.getTag();
            }

            Sub sb = subsList.get(position);

            holder.name.setText(sb.subName);
            holder.name.setChecked(selectedSubs.contains(sb.subID));
            holder.name.setTag(sb);
            holder.isNSFW.setText(sb.isNSFW ? "NSFW" : "");
            holder.subscribers.setText(thousandsFormatter(sb.subscriberCount));
            return convertView;
        }

        // Filter Class
        public void filter(String charText) {
            charText = charText.toLowerCase(Locale.getDefault());
            subsList.clear();
            if (charText.length() == 0) {
                subsList.addAll(origList);
            }
            else
            {
                for (Sub s : origList)
                {
                    if (s.subName.contains(charText))
                    {
                        subsList.add(s);
                    }
                }
            }
            notifyDataSetChanged();
        }
    }

    public String thousandsFormatter(int value){
        if (value < 1000){
            return value+" ";
        }
        int scale = (int) Math.pow(10, 1);
        return Math.round(value/100 ) / (1.0*10) +"k ";
    }

    private void buttonPressed() {
        Button back = (Button) findViewById(R.id.updateSelection);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuffer response = new StringBuffer();

                response.append(" Saved: \n");
                ArrayList<Sub> s = adp.subsList;
                for (Sub d : s) {
                    if (d.selected) {
                        response.append("\n " + d.subName);
                    }
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
//        String serial = "";
//        for (Sub s : subsList){
//            if (s.selectedString){
//                serial = serial + s.subID+",";
//            }
//        }
        String serial = gson.toJson(selectedSubs, hashSetMap);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString("SELECTED_SUBS", serial);
        System.out.println("[DBG] saving " +serial);
        prefsEditor.apply();
    }
}
