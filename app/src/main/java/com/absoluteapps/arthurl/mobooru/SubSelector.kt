package com.absoluteapps.arthurl.mobooru

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ContextThemeWrapper
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import org.json.JSONObject

import java.net.URL
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.Locale
import java.util.Scanner

class SubSelector : AppCompatActivity() {

    internal lateinit var origList: ArrayList<Sub>
    internal lateinit var subsList: ArrayList<Sub>
    internal lateinit var subsMap: HashMap<Int, Sub>
    internal lateinit var customSubsMap: HashMap<Int, Sub>
    internal lateinit var selectedSubs: HashSet<Int>
    internal lateinit var selectedCustomSubs: HashSet<Int>
    internal var adp: CustomAdapter? = null
    internal var gson = Gson()
    internal lateinit var mInfo: ImageButton
    internal lateinit var mAddCustomSub: ImageButton
    internal lateinit var mEditText: EditText
    internal var hashSetMap = object : TypeToken<HashSet<Int>>() {

    }.type
    internal lateinit var progressDialog: ProgressDialog
    internal lateinit var addCustomSubTask: AddCustomSub
    internal lateinit var prefs: SharedPreferences
    internal lateinit var prefsEditor: SharedPreferences.Editor
    internal var intSubMap = object : TypeToken<Map<Int, Sub>>() {

    }.type

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_subs)
        mEditText = findViewById<View>(R.id.search) as EditText
        mAddCustomSub = findViewById<View>(R.id.addCustomSub) as ImageButton
        //        mClearText = (Button) findViewById(R.id.clearText);
        mInfo = findViewById<View>(R.id.info) as ImageButton

        //initially clear button is invisible
        //        mClearText.setVisibility(View.INVISIBLE);

        //clear button visibility on text change
        mEditText.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
                //do nothing
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                //do nothing
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.length != 0) {
                    //                    mClearText.setVisibility(View.VISIBLE);
                } else {
                    //                    mClearText.setVisibility(View.INVISIBLE);
                }
            }
        })

        // Info button
        mInfo.setOnClickListener {
            val d1 = AlertDialog.Builder(ContextThemeWrapper(this@SubSelector, R.style.AppTheme))
                    .setTitle("Tips")
                    .setNegativeButton("Back") { dialog, which ->
                        // do nothing
                    }
                    .setIcon(R.drawable.baseline_help_outline_black_36)
                    .setMessage("Check the subreddits you'd like to see pictures from; press and hold each subreddit to see its description!")
                    .create()

            d1.show()
        }

        // Add Custom sub button
        mAddCustomSub.setOnClickListener {
            val imm = this@SubSelector.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            val dialogBuilder = AlertDialog.Builder(ContextThemeWrapper(this@SubSelector, R.style.AppTheme))
            val inflater = LayoutInflater.from(this@SubSelector)
            val dialogView = inflater.inflate(R.layout.add_custom_sub, null)
            dialogBuilder.setView(dialogView)
            val edt = dialogView.findViewById<View>(R.id.subEntry) as EditText

            dialogBuilder.setTitle("Add subreddit")
            dialogBuilder.setPositiveButton("Done") { dialog, whichButton ->
                progressDialog = ProgressDialog.show(this@SubSelector, "Adding sub", "...", true)
                addCustomSubTask = AddCustomSub(edt.text.toString())
                addCustomSubTask.execute()
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
            }
            dialogBuilder.setNegativeButton("Cancel") { dialog, whichButton -> imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0) }
            val b = dialogBuilder.create()
            b.show()
            edt.requestFocus()

            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }

        // List of subs
        subsMap = intent.getSerializableExtra("subsMap") as HashMap<Int, Sub>
        customSubsMap = intent.getSerializableExtra("customSubsMap") as HashMap<Int, Sub>
        origList = ArrayList(subsMap.values)

        // Checked favorite subs <sub_id, checked>
        prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        prefsEditor = prefs.edit()
        try {
            selectedSubs = gson.fromJson(prefs.getString("SELECTED_SUBS", "[" + R.string.defaultsub + "]"), hashSetMap)
            selectedCustomSubs = gson.fromJson(prefs.getString("SELECTED_CUSTOM_SUBS", "[]"), hashSetMap)
            for (id in selectedSubs) {
                subsMap[id]!!.selected = true
            }
            for (id in selectedCustomSubs) {
                customSubsMap[id]!!.selected = true
            }
        } catch (ex: Exception) {
            // Set selectedString sub to awwnime only
            selectedSubs = HashSet()
            selectedSubs.add(1)
        }

        subsList = ArrayList(subsMap.values)
        subsList.addAll(customSubsMap.values)
        displayList()
        buttonPressed()
    }

    fun clear(view: View) {
        mEditText.setText("")
        //        mClearText.setVisibility(View.INVISIBLE);
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_settings, menu)
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)

    }

    override fun onBackPressed() {
        overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
        startActivity(Intent(this, Main::class.java))
        overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
        finish()
    }

    private fun displayList() {
        Collections.sort(subsList)
        adp = CustomAdapter(this, R.layout.activity_settings_subs_checkboxes, subsList)
        val lv = findViewById<View>(R.id.listView) as ListView
        lv.adapter = adp

        // Locate the EditText in listview_main.xml
        val editsearch = findViewById<View>(R.id.search) as EditText

        // Capture Text in EditText
        editsearch.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(arg0: Editable) {
                // TODO Auto-generated method stub
                val text = editsearch.text.toString().toLowerCase(Locale.getDefault())
                adp!!.filter(text)
                Collections.sort(subsList)
            }

            override fun beforeTextChanged(arg0: CharSequence, arg1: Int,
                                           arg2: Int, arg3: Int) {
                // TODO Auto-generated method stub
            }

            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int,
                                       arg3: Int) {
                // TODO Auto-generated method stub
            }
        })

        val subs_toolbar = findViewById<View>(R.id.subs_toolbar) as Toolbar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            subs_toolbar.title = "Subreddits"
        }
        setSupportActionBar(subs_toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        //        subs_toolbar.setNavigationIcon(R.drawable.);
        subs_toolbar.setNavigationOnClickListener {
            finish()
            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
            startActivity(Intent(this@SubSelector, Main::class.java))
            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
        }

        lv.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val sub = parent.getItemAtPosition(position) as Sub
            val cb = view.findViewById<View>(R.id.checkBox) as CheckBox

            val isSelected = selectedSubs.contains(sub.subID) || selectedCustomSubs.contains(sub.subID)
            if (isSelected) {
                if (sub.isCustom) {
                    selectedCustomSubs.remove(sub.subID)
                } else {
                    selectedSubs.remove(sub.subID)
                }
            } else {
                if (sub.isCustom) {
                    selectedCustomSubs.add(sub.subID)
                } else {
                    selectedSubs.add(sub.subID)
                }
            }
            cb.isChecked = !isSelected
            sub.selected = !isSelected
            Collections.sort(subsList)
            adp!!.notifyDataSetChanged()
        }

        lv.onItemLongClickListener = AdapterView.OnItemLongClickListener { parent, view, position, id ->
            val sub = parent.getItemAtPosition(position) as Sub
            val d1: AlertDialog
            if (sub.isCustom) {
                d1 = AlertDialog.Builder(ContextThemeWrapper(this@SubSelector, R.style.AppTheme))
                        .setTitle("About " + sub.subName + ":")
                        .setNegativeButton("Back") { dialog, which ->
                            // Do nothing
                        }
                        .setNeutralButton("Delete") { dialogInterface, i ->
                            customSubsMap.remove(sub.subID)
                            val serial = gson.toJson(customSubsMap, intSubMap)
                            prefsEditor.putString("CUSTOM_SUBS", serial)

                            selectedCustomSubs.remove(sub.subID)
                            prefsEditor.putString("SELECTED_CUSTOM_SUBS", gson.toJson(selectedCustomSubs, hashSetMap))

                            prefsEditor.apply()
                            origList.remove(sub)
                            subsList.remove(sub)

                            adp!!.notifyDataSetChanged()

                            Toast.makeText(this@SubSelector, "Removed " + sub.subName, Toast.LENGTH_SHORT).show()
                        }
                        .setMessage(if (sub.desc.length > 0) sub.desc else "No description")
                        .create()
            } else {
                d1 = AlertDialog.Builder(ContextThemeWrapper(this@SubSelector, R.style.AppTheme))
                        .setTitle("About " + sub.subName + ":")
                        .setNegativeButton("Back") { dialog, which ->
                            // Do nothing
                        }
                        .setMessage(if (sub.desc.length > 0) sub.desc else "No description")
                        .create()
            }
            d1.show()
            true
        }
    }

    private fun buttonPressed() {
        val back = findViewById<View>(R.id.updateSelection) as Button
        back.setOnClickListener {
            val response = StringBuffer()
            response.append(" Saved: \n")
            val s = adp!!.subsList
            for (d in s) {
                if (d.selected) {
                    response.append("\n " + d.subName)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        var serial = gson.toJson(selectedSubs, hashSetMap)
        prefsEditor.putString("SELECTED_SUBS", serial)
        serial = gson.toJson(selectedCustomSubs, hashSetMap)
        prefsEditor.putString("SELECTED_CUSTOM_SUBS", serial)
        prefsEditor.apply()
    }

    inner class CustomAdapter(context: Context, textViewResourceId: Int, val subsList: ArrayList<Sub>) : ArrayAdapter<Sub>(context, textViewResourceId, subsList) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView

            var holder: ViewHolder? = null

            if (convertView == null) {
                val vi = getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                convertView = vi.inflate(R.layout.activity_settings_subs_checkboxes, parent, false)

                holder = ViewHolder()
                holder.name = convertView!!.findViewById<View>(R.id.checkBox) as CheckBox
                holder.isNSFW = convertView.findViewById<View>(R.id.isNSFW) as TextView
                holder.subscribers = convertView.findViewById<View>(R.id.subscribers) as TextView
                convertView.tag = holder
            } else {
                holder = convertView.tag as ViewHolder
            }

            val sb = subsList[position]

            holder.name!!.text = sb.subName
            holder.name!!.isChecked = !sb.isCustom && selectedSubs.contains(sb.subID) || sb.isCustom && selectedCustomSubs.contains(sb.subID)
            holder.name!!.tag = sb
            holder.isNSFW!!.text = if (sb.isNSFW) "NSFW" else ""
            holder.subscribers!!.text = Formatter.shortHandFormatter(sb.subscriberCount)
            return convertView
        }

        // Filter Class
        fun filter(charText: String) {
            var charText = charText
            charText = charText.toLowerCase(Locale.getDefault())
            subsList.clear()
            if (charText.length == 0) {
                subsList.addAll(origList)
            } else {
                for (s in origList) {
                    if (s.subName.contains(charText)) {
                        subsList.add(s)
                    }
                }
            }
            notifyDataSetChanged()
        }

        private inner class ViewHolder {
            internal var name: CheckBox? = null
            internal var isNSFW: TextView? = null
            internal var subscribers: TextView? = null
        }
    }

    // TODO: Implement as fallback method if redditbooru is down
    inner class AddCustomSub internal constructor(internal var customSubName: String) : AsyncTask<Void, Int, Int>() {

        override fun doInBackground(vararg params: Void): Int? {
            // Parse subreddit
            // if begins with r/, pass directly
            // else prefix with r/
            if (customSubName.length == 0)
                return -1
            if (!customSubName.startsWith("r/"))
                customSubName = "r/$customSubName"

            customSubName.trim { it <= ' ' }

            for (s in subsList) {
                if (customSubName == s.subName)
                    return -2
            }

            for (s in customSubsMap.values) {
                if (customSubName == s.subName)
                    return -2
            }

            var info: String
            val obj: JSONObject
            try {
                val aboutURL = "https://www.reddit.com/$customSubName/about.json"
                val url = URL(aboutURL)
                val scan = Scanner(url.openStream())
                info = ""
                while (scan.hasNext())
                    info += scan.nextLine()
                scan.close()
                obj = JSONObject(info).getJSONObject("data")
                if (obj.has("dist")) {
                    // Invalid or empty subreddit
                    return -3
                } else {
                    val customSubID = customSubsMap.size + 10000
                    val newSub = Sub(
                            customSubName,
                            customSubID,
                            obj.getInt("subscribers"),
                            true,
                            obj.getBoolean("over18"),
                            true,
                            obj.getString("public_description")
                    )
                    customSubsMap[customSubsMap.size + 10000] = newSub
                    val serial = gson.toJson(customSubsMap, intSubMap)
                    prefsEditor.putString("CUSTOM_SUBS", serial)

                    selectedCustomSubs.add(customSubID)
                    prefsEditor.putString("SELECTED_CUSTOM_SUBS", gson.toJson(selectedCustomSubs, hashSetMap))

                    prefsEditor.apply()
                    origList.add(newSub)
                    subsList.add(newSub)

                    runOnUiThread { displayList() }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                return -4
            }

            return 0
        }

        override fun onPostExecute(result: Int) {
            var msg: String? = null
            when (result) {
                0 -> msg = "Added $customSubName sub..."
                -1 -> msg = "Subreddit cannot be blank!"
                -2 -> msg = "Subreddit $customSubName has already been added!"
                -3 -> msg = "Subreddit $customSubName does not exist or is empty"
                -4 -> msg = "Error $customSubName sub..."
            }
            Toast.makeText(applicationContext,
                    msg,
                    Toast.LENGTH_LONG).show()
            progressDialog.dismiss()
        }

        override fun onPreExecute() {

        }

        override fun onProgressUpdate(vararg values: Int?) {

        }
    }
}
