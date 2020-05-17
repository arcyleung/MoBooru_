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
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.Toolbar
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.net.URL
import java.util.*

class SubSelector : AppCompatActivity() {

    internal var origList: ArrayList<Sub> = ArrayList<Sub>()
    internal lateinit var subsList: ArrayList<Sub>
    private lateinit var subsMap: HashMap<Int, Sub>
    internal lateinit var customSubsMap: HashMap<Int, Sub>
    internal lateinit var selectedSubs: HashSet<Int>
    internal lateinit var selectedCustomSubs: HashSet<Int>
    internal var adp: CustomAdapter? = null
    internal var gson = Gson()
    private lateinit var mInfo: ImageButton
    private lateinit var mAddCustomSub: ImageButton
    private lateinit var mEditText: EditText
    private lateinit var mTabLayout: TabLayout
    internal var hashSetMap = object : TypeToken<HashSet<Int>>() {

    }.type
    internal lateinit var progressDialog: ProgressDialog
    private lateinit var addCustomSubTask: AddCustomSub
    private lateinit var prefs: SharedPreferences
    internal lateinit var prefsEditor: SharedPreferences.Editor
    internal var intSubMap = object : TypeToken<Map<Int, Sub>>() {

    }.type

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_subs)
        mEditText = findViewById<View>(R.id.search) as EditText
        mTabLayout = findViewById<View>(R.id.tabLayout) as TabLayout
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
                if (s.isNotEmpty()) {
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
                    .setNegativeButton("Back") { _, _ ->
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
            dialogBuilder.setPositiveButton("Done") { _, _ ->
                progressDialog = ProgressDialog.show(this@SubSelector, "Adding sub", "...", true)
                addCustomSubTask = AddCustomSub(edt.text.toString())
                addCustomSubTask.execute()
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
            }
            dialogBuilder.setNegativeButton("Cancel") { _, _ -> imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0) }
            val b = dialogBuilder.create()
            b.show()
            edt.requestFocus()

            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }

        // List of subs
        subsMap = intent.getSerializableExtra("subsMap") as HashMap<Int, Sub>
        customSubsMap = intent.getSerializableExtra("customSubsMap") as HashMap<Int, Sub>
        origList.addAll(subsMap.values)
        origList.addAll(customSubsMap.values)
        origList.sort()

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
        subsList.sort()
        displayList()
        updateTabs()
        buttonPressed()
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

    private fun updateTabs() {
        mTabLayout.getTabAt(0)!!.text = "ALL (${origList.size})"
        mTabLayout.getTabAt(1)!!.text = "SELECTED (${selectedCustomSubs.size + selectedSubs.size})"
    }

    private fun displayList() {
        adp = CustomAdapter(this, R.layout.activity_settings_subs_checkboxes, subsList)
        val lv = findViewById<View>(R.id.listView) as ListView
        lv.adapter = adp

        // Locate the EditText in activity_settings_subs.xml
        val editSearch = findViewById<View>(R.id.search) as EditText

        // Locate the TabLayout in activity_settings_subs.xml

        mTabLayout.addOnTabSelectedListener(object :
                TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                adp!!.filter("", mTabLayout.selectedTabPosition == 1)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }
        })

        // Capture Text in EditText
        editSearch.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(arg0: Editable) {
                // TODO Auto-generated method stub
                val text = editSearch.text.toString().toLowerCase(Locale.getDefault())
                adp!!.filter(text, mTabLayout.selectedTabPosition == 1)
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

        val subsToolbar = findViewById<View>(R.id.subs_toolbar) as Toolbar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            subsToolbar.title = "Subreddits"
        }
        setSupportActionBar(subsToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        //        subs_toolbar.setNavigationIcon(R.drawable.);
        subsToolbar.setNavigationOnClickListener {
            finish()
            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
            startActivity(Intent(this@SubSelector, Main::class.java))
            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
        }

        lv.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, _ ->
            val sub = parent.getItemAtPosition(position) as Sub
            val cb = view.findViewById<View>(R.id.checkBox) as CheckBox

            val isSelected = selectedSubs.contains(sub.subID) || selectedCustomSubs.contains(sub.subID)
            if (isSelected) {
                if (sub.isCustom) {
                    selectedCustomSubs.remove(sub.subID)
                } else {
                    selectedSubs.remove(sub.subID)
                }

                // If we are showing only selected subs, update the ListView adapter data
                // TODO: refactor with enum
                if (mTabLayout.selectedTabPosition == 1) {
                    subsList.remove(sub)
                    adp!!.notifyDataSetChanged()
                }
            } else {
                if (sub.isCustom) {
                    selectedCustomSubs.add(sub.subID)
                } else {
                    selectedSubs.add(sub.subID)
                }
            }
            updateTabs()
            cb.isChecked = !isSelected
            sub.selected = !isSelected
            adp!!.notifyDataSetChanged()
        }

        lv.onItemLongClickListener = AdapterView.OnItemLongClickListener { parent, _, position, _ ->
            val sub = parent.getItemAtPosition(position) as Sub
            val d1: AlertDialog
            if (sub.isCustom) {
                d1 = AlertDialog.Builder(ContextThemeWrapper(this@SubSelector, R.style.AppTheme))
                        .setTitle("About " + sub.subName + ":")
                        .setNegativeButton("Back") { _, _ ->
                            // Do nothing
                        }
                        .setNeutralButton("Delete") { _, _ ->
                            customSubsMap.remove(sub.subID)
                            val serial = gson.toJson(customSubsMap, intSubMap)
                            prefsEditor.putString("CUSTOM_SUBS", serial)

                            selectedCustomSubs.remove(sub.subID)
                            prefsEditor.putString("SELECTED_CUSTOM_SUBS", gson.toJson(selectedCustomSubs, hashSetMap))

                            prefsEditor.apply()
                            origList.remove(sub)
                            subsList.remove(sub)

                            adp!!.notifyDataSetChanged()
                            updateTabs()
                            Toast.makeText(this@SubSelector, "Removed " + sub.subName, Toast.LENGTH_SHORT).show()
                        }
                        .setMessage(if (sub.desc.isNotEmpty()) sub.desc else "No description")
                        .create()
            } else {
                d1 = AlertDialog.Builder(ContextThemeWrapper(this@SubSelector, R.style.AppTheme))
                        .setTitle("About " + sub.subName + ":")
                        .setNegativeButton("Back") { _, _ ->
                            // Do nothing
                        }
                        .setMessage(if (sub.desc.isNotEmpty()) sub.desc else "No description")
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

    inner class CustomAdapter(context: Context, textViewResourceId: Int, var subsList: ArrayList<Sub>) : ArrayAdapter<Sub>(context, textViewResourceId, subsList) {

        override fun getCount(): Int {
            return subsList.size
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView

            var holder: ViewHolder?

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
        fun filter(searchString: String, onlySelected: Boolean) {
            var lower = searchString.toLowerCase(Locale.getDefault())
            var tmp: ArrayList<Sub> = ArrayList<Sub>()
            subsList.clear()
            tmp.addAll(origList)

            // Only show selected subs
            if (onlySelected) {
                tmp = tmp.filter { s -> s.selected == onlySelected } as ArrayList<Sub>
            }

            // Has search term
            if (!lower.isEmpty()) {
                tmp = tmp.filter { s -> s.subName.contains(lower) } as ArrayList<Sub>
            }
            subsList.addAll(tmp)
            notifyDataSetChanged()
        }

        private inner class ViewHolder {
            internal var name: CheckBox? = null
            internal var isNSFW: TextView? = null
            internal var subscribers: TextView? = null
        }
    }

    // TODO: Implement as fallback method if redditbooru is down
    inner class AddCustomSub internal constructor(private var customSubName: String) : AsyncTask<Void, Int, Int>() {

        override fun doInBackground(vararg params: Void): Int? {
            // Parse subreddit
            // if begins with r/, pass directly
            // else prefix with r/
            if (customSubName.isEmpty())
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
                    subsList.sort()

                    runOnUiThread {
                        displayList()
                        updateTabs()
                    }
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
