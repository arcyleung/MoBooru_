package com.absoluteapps.arthurl.mobooru;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class SubSelector extends AppCompatActivity {

    ArrayList<Sub> origList;
    ArrayList<Sub> subsList;
    HashMap<Integer, Sub> subsMap;
    HashMap<Integer, Sub> customSubsMap;
    HashSet<Integer> selectedSubs;
    HashSet<Integer> selectedCustomSubs;
    CustomAdapter adp = null;
    Gson gson = new Gson();
    Button mClearText;
    ImageButton mInfo, mAddCustomSub;
    EditText mEditText;
    Type hashSetMap = new TypeToken<HashSet<Integer>>() {
    }.getType();
    ProgressDialog progressDialog;
    AddCustomSub addCustomSubTask;
    SharedPreferences prefs;
    SharedPreferences.Editor prefsEditor;
    Type intSubMap = new TypeToken<Map<Integer, Sub>>() {
    }.getType();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_subs);
        mEditText = (EditText) findViewById(R.id.search);
        mAddCustomSub = (ImageButton) findViewById(R.id.addCustomSub);
//        mClearText = (Button) findViewById(R.id.clearText);
        mInfo = (ImageButton) findViewById(R.id.info);

        //initially clear button is invisible
//        mClearText.setVisibility(View.INVISIBLE);

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
                if (s.length() != 0) {
//                    mClearText.setVisibility(View.VISIBLE);
                } else {
//                    mClearText.setVisibility(View.INVISIBLE);
                }
            }
        });

        // Info button
        mInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog d1 = new AlertDialog.Builder(new ContextThemeWrapper(SubSelector.this, R.style.AppTheme))
                        .setTitle("Tips")
                        .setNegativeButton("Back", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(R.drawable.baseline_help_outline_black_36)
                        .setMessage("Check the subreddits you'd like to see pictures from; press and hold each subreddit to see its description!")
                        .create();

                d1.show();
            }
        });

        // Add Custom sub button
        mAddCustomSub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final InputMethodManager imm = (InputMethodManager) SubSelector.this.getSystemService(SubSelector.INPUT_METHOD_SERVICE);
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(SubSelector.this, R.style.AppTheme));
                LayoutInflater inflater = LayoutInflater.from(SubSelector.this);
                final View dialogView = inflater.inflate(R.layout.add_custom_sub, null);
                dialogBuilder.setView(dialogView);
                final EditText edt = (EditText) dialogView.findViewById(R.id.subEntry);

                dialogBuilder.setTitle("Add custom subreddit");
                dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        progressDialog = ProgressDialog.show(SubSelector.this, "Adding sub", "...", true);
                        addCustomSubTask = new AddCustomSub(edt.getText().toString());
                        addCustomSubTask.execute();
                        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                    }
                });
                dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                    }
                });
                AlertDialog b = dialogBuilder.create();
                b.show();
                edt.requestFocus();

                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        });

        // List of subs
        subsMap = (HashMap<Integer, Sub>) getIntent().getSerializableExtra("subsMap");
        customSubsMap = (HashMap<Integer, Sub>) getIntent().getSerializableExtra("customSubsMap");
        origList = new ArrayList(subsMap.values());

        // Checked favorite subs <sub_id, checked>
        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        prefsEditor = prefs.edit();
        try {
            selectedSubs = gson.fromJson(prefs.getString("SELECTED_SUBS", "[" + R.string.defaultsub + "]"), hashSetMap);
            selectedCustomSubs = gson.fromJson(prefs.getString("SELECTED_CUSTOM_SUBS", "[]"), hashSetMap);
            for (int id : selectedSubs) {
                subsMap.get(id).selected = true;
            }
            for (int id : selectedCustomSubs) {
                customSubsMap.get(id).selected = true;
            }
        } catch (Exception ex) {
            // Set selectedString sub to awwnime only
            selectedSubs = new HashSet<Integer>();
            selectedSubs.add(1);
        }
        subsList = new ArrayList<>(subsMap.values());
        subsList.addAll(customSubsMap.values());
        displayList();
        buttonPressed();
    }

    public void clear(View view) {
        mEditText.setText("");
//        mClearText.setVisibility(View.INVISIBLE);
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
//        overridePendingTransition(R.transition.fade_in, R.transition.fade_out);
        startActivity(new Intent(this, Main.class));
//        overridePendingTransition(R.transition.fade_in, R.transition.fade_out);
        finish();
    }

    private void displayList() {
        Collections.sort(subsList);
        adp = new CustomAdapter(this, R.layout.activity_settings_subs_checkboxes, subsList);
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
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Sub sub = (Sub) parent.getItemAtPosition(position);
                CheckBox cb = (CheckBox) view.findViewById(R.id.checkBox);

                boolean isSelected = selectedSubs.contains(sub.subID) || selectedCustomSubs.contains(sub.subID);
                if (isSelected) {
                    if (sub.isCustom) {
                        selectedCustomSubs.remove(sub.subID);
                    } else {
                        selectedSubs.remove(sub.subID);
                    }
                } else {
                    if (sub.isCustom) {
                        selectedCustomSubs.add(sub.subID);
                    } else {
                        selectedSubs.add(sub.subID);
                    }
                }
                cb.setChecked(!isSelected);
                sub.selected = (!isSelected);
                Collections.sort(subsList);
                adp.notifyDataSetChanged();
            }
        });

        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Sub sub = (Sub) parent.getItemAtPosition(position);
                    final AlertDialog d1;
                    if (sub.isCustom) {
                        d1 = new AlertDialog.Builder(new ContextThemeWrapper(SubSelector.this, R.style.AppTheme))
                                .setTitle("About " + sub.subName + ":")
                                .setNegativeButton("Back", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Do nothing
                                    }
                                })
                                .setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        customSubsMap.remove(sub.subID);
                                        String serial = gson.toJson(customSubsMap, intSubMap);
                                        prefsEditor.putString("CUSTOM_SUBS", serial);

                                        selectedCustomSubs.remove(sub.subID);
                                        prefsEditor.putString("SELECTED_CUSTOM_SUBS", gson.toJson(selectedCustomSubs, hashSetMap));

                                        prefsEditor.apply();
                                        origList.remove(sub);
                                        subsList.remove(sub);

                                        adp.notifyDataSetChanged();

                                        Toast.makeText(SubSelector.this, "Removed " + sub.subName, Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setMessage(sub.desc.length() > 0 ? sub.desc : "No description")
                                .create();
                    } else {
                        d1 = new AlertDialog.Builder(new ContextThemeWrapper(SubSelector.this, R.style.AppTheme))
                                .setTitle("About " + sub.subName + ":")
                                .setNegativeButton("Back", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Do nothing
                                    }
                                })
                                .setMessage(sub.desc.length() > 0 ? sub.desc : "No description")
                                .create();
                    }
                    d1.show();
                return true;
            }
        });
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
        String serial = gson.toJson(selectedSubs, hashSetMap);
        prefsEditor.putString("SELECTED_SUBS", serial);
        serial = gson.toJson(selectedCustomSubs, hashSetMap);
        prefsEditor.putString("SELECTED_CUSTOM_SUBS", serial);
        prefsEditor.apply();
    }

    private class CustomAdapter extends ArrayAdapter<Sub> {

        private ArrayList<Sub> subsList;

        public CustomAdapter(Context context, int textViewResourceId, ArrayList<Sub> subsList) {
            super(context, textViewResourceId, subsList);
            this.subsList = subsList;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            ViewHolder holder = null;

            if (convertView == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = vi.inflate(R.layout.activity_settings_subs_checkboxes, parent, false);

                holder = new ViewHolder();
                holder.name = (CheckBox) convertView.findViewById(R.id.checkBox);
                holder.isNSFW = (TextView) convertView.findViewById(R.id.isNSFW);
                holder.subscribers = (TextView) convertView.findViewById(R.id.subscribers);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Sub sb = subsList.get(position);

            holder.name.setText(sb.subName);
            holder.name.setChecked((!sb.isCustom && selectedSubs.contains(sb.subID)) || (sb.isCustom && selectedCustomSubs.contains((sb.subID))));
            holder.name.setTag(sb);
            holder.isNSFW.setText(sb.isNSFW ? "NSFW" : "");
            holder.subscribers.setText(Formatter.shortHandFormatter(sb.subscriberCount));
            return convertView;
        }

        // Filter Class
        public void filter(String charText) {
            charText = charText.toLowerCase(Locale.getDefault());
            subsList.clear();
            if (charText.length() == 0) {
                subsList.addAll(origList);
            } else {
                for (Sub s : origList) {
                    if (s.subName.contains(charText)) {
                        subsList.add(s);
                    }
                }
            }
            notifyDataSetChanged();
        }

        private class ViewHolder {
            CheckBox name;
            TextView isNSFW;
            TextView subscribers;
        }
    }

    // TODO: Implement as fallback method if redditbooru is down
    private class AddCustomSub extends AsyncTask<Void, Integer, Integer> {
        String customSubName;

        AddCustomSub(String subName) {
            this.customSubName = subName;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            // Parse subreddit
            // if begins with r/, pass directly
            // else prefix with r/
            if (customSubName.length() == 0)
                return -1;
            if (!customSubName.startsWith("r/"))
                customSubName = "r/" + customSubName;

            customSubName.trim();

            for (Sub s : subsList) {
                if (customSubName.equals(s.subName))
                    return -2;
            }

            for (Sub s : customSubsMap.values()) {
                if (customSubName.equals(s.subName))
                    return -2;
            }

            String info;
            JSONObject obj;
            try {
                String aboutURL = "https://www.reddit.com/" + customSubName + "/about.json";
                URL url = new URL(aboutURL);
                Scanner scan = new Scanner(url.openStream());
                info = "";
                while (scan.hasNext())
                    info += scan.nextLine();
                scan.close();
                obj = new JSONObject(info).getJSONObject("data");
                if (obj.has("dist")) {
                    // Invalid or empty subreddit
                    return -3;
                } else {
                    int customSubID = customSubsMap.size() + 10000;
                    Sub newSub = new Sub(
                            customSubName,
                            customSubID,
                            obj.getInt("subscribers"),
                            true,
                            obj.getBoolean("over18"),
                            true,
                            obj.getString("public_description")
                    );
                    customSubsMap.put(customSubsMap.size() + 10000, newSub);
                    String serial = gson.toJson(customSubsMap, intSubMap);
                    prefsEditor.putString("CUSTOM_SUBS", serial);

                    selectedCustomSubs.add(customSubID);
                    prefsEditor.putString("SELECTED_CUSTOM_SUBS", gson.toJson(selectedCustomSubs, hashSetMap));

                    prefsEditor.apply();
                    origList.add(newSub);
                    subsList.add(newSub);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            displayList();
                        }
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return -4;
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            String msg = null;
            switch (result) {
                case 0:
                    msg = "Added " + customSubName + " sub...";
                    break;
                case -1:
                    msg = "Subreddit cannot be blank!";
                    break;
                case -2:
                    msg = "Subreddit " + customSubName + " has already been added!";
                    break;
                case -3:
                    msg = "Subreddit " + customSubName + " does not exist or is empty";
                    break;
                case -4:
                    msg = "Error " + customSubName + " sub...";
                    break;
            }
            Toast.makeText(getApplicationContext(),
                    msg,
                    Toast.LENGTH_LONG).show();
            progressDialog.dismiss();
        }

        protected void onPreExecute() {

        }

        protected void onProgressUpdate(Integer... values) {

        }
    }
}
