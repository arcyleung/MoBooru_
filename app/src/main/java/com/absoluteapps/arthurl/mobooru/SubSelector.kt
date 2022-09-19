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
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class SubSelector : AppCompatActivity() {

    internal var origList: ArrayList<Sub> = ArrayList<Sub>()
    internal lateinit var subsList: ArrayList<Sub>
    internal var allShownSubsCount: Int = 0
    internal var selectedShownSubsCount: Int = 0
    private lateinit var subsMap: HashMap<String, Sub>
    internal lateinit var customSubsMap: HashMap<String, Sub>
    internal lateinit var selectedSubs: HashSet<String>
    internal lateinit var selectedCustomSubs: HashSet<String>
    internal var showNsfw: Boolean = false
    internal var darkmode: Boolean = false
    internal var adp: CustomAdapter? = null
    internal var gson = Gson()
    private lateinit var mInfo: ImageButton
    private lateinit var mAddCustomSub: ImageButton
    private lateinit var mEditText: EditText
    private lateinit var addSuccess: ArrayList<String>
    private lateinit var addExisting: ArrayList<String>
    private lateinit var addFailed: ArrayList<String>
    private var addCount: Int = 0
    private lateinit var mTabLayout: TabLayout
    internal var hashSetMap = object : TypeToken<HashSet<String>>() {

    }.type
    internal lateinit var progressDialog: ProgressDialog
    private lateinit var addCustomSubTask: AddCustomSub
    private lateinit var prefs: SharedPreferences
    internal lateinit var prefsEditor: SharedPreferences.Editor
    internal var intSubMap = object : TypeToken<Map<String, Sub>>() {
    }.type

    override fun onCreate(savedInstanceState: Bundle?) {
        // Checked favorite subs <sub_id, checked>
        prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        prefsEditor = prefs.edit()

        // Dark mode
        darkmode = prefs.getBoolean("DARK_MODE", false)
        setTheme(if (darkmode) R.style.AppThemeDark else R.style.AppThemeLight)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_subs)

        mEditText = findViewById<View>(R.id.search) as EditText
        mTabLayout = findViewById<View>(R.id.tabLayout) as TabLayout
        mAddCustomSub = findViewById<View>(R.id.addCustomSub) as ImageButton
        //        mClearText = (Button) findViewById(R.id.clearText);
        mInfo = findViewById<View>(R.id.info) as ImageButton

        //initially clear button is invisible
        // mClearText.setVisibility(View.INVISIBLE);

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
                    //mClearText.setVisibility(View.VISIBLE);
                } else {
                    //mClearText.setVisibility(View.INVISIBLE);
                }
            }
        })

        // Info button
        mInfo.setOnClickListener {
            val d1 = AlertDialog.Builder(ContextThemeWrapper(this@SubSelector, if (darkmode) R.style.AppThemeDark else R.style.AppThemeLight))
                    .setTitle(R.string.tips)
                    .setNeutralButton(R.string.reset) { _, _ ->
                        // Reset selection to default
                        selectedSubs = HashSet()
                        selectedSubs.add("awwnime")
                        selectedCustomSubs = HashSet()
                        prefsEditor.putString("SELECTED_SUBS", "['awwnime']")
                        prefsEditor.putString("SELECTED_CUSTOM_SUBS", "[]")
                        prefsEditor.apply()
                        finish()
                        overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
                        startActivity(Intent(this@SubSelector, Main::class.java))
                        overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
                    }
                    .setNegativeButton(R.string.back) { _, _ ->
                        // do nothing
                    }
                    .setIcon(R.drawable.baseline_help_outline_white_36)
                    .setMessage(R.string.help_button_text)
                    .create()
            d1.show()
        }

        // Add Custom sub button
        mAddCustomSub.setOnClickListener {
            val imm = this@SubSelector.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            val dialogBuilder = AlertDialog.Builder(ContextThemeWrapper(this@SubSelector, if (darkmode) R.style.AppThemeDark else R.style.AppThemeLight))
            val inflater = LayoutInflater.from(this@SubSelector)
            val dialogView = inflater.inflate(R.layout.add_custom_sub, null)
            dialogBuilder.setView(dialogView)
            val edt = dialogView.findViewById<View>(R.id.subEntry) as EditText

            dialogBuilder.setTitle(R.string.add_subreddit)
            dialogBuilder.setMessage(R.string.add_subreddit_desc)
            dialogBuilder.setPositiveButton(R.string.done) { _, _ ->
                progressDialog = ProgressDialog.show(this@SubSelector, getString(R.string.add_subreddit), "…", true)
                val inputs = edt.text.toString().split("\n")

                // Reset counters
                addSuccess = ArrayList<String>()
                addExisting = ArrayList<String>()
                addFailed = ArrayList<String>()
                addCount = inputs.size

                inputs.forEach {
                    addCustomSubTask = AddCustomSub(it)
                    addCustomSubTask.execute()
                }

                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
            }
            dialogBuilder.setNegativeButton(R.string.cancel) { _, _ -> imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0) }
            val b = dialogBuilder.create()
            b.show()
            edt.requestFocus()

            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }

        // List of subs
        subsMap = intent.getSerializableExtra("subsMap") as HashMap<String, Sub>
        customSubsMap = intent.getSerializableExtra("customSubsMap") as HashMap<String, Sub>

        // Delete NSFW default subs
        origList.addAll(subsMap.values.filter { s -> !s.isNSFW })
        origList.addAll(customSubsMap.values)
        origList.sort()

        try {
            selectedSubs = gson.fromJson(prefs.getString("SELECTED_SUBS", "['awwnime']"), hashSetMap)
            selectedCustomSubs = gson.fromJson(prefs.getString("SELECTED_CUSTOM_SUBS", "[]"), hashSetMap)
            showNsfw = prefs.getBoolean("SHOW_NSFW", false)
            for (id in selectedSubs) {
                if (subsMap.keys.contains(id)) {
                    subsMap[id]?.selected = true
                } else {
                    selectedSubs.remove(id)
                }
            }
            for (id in selectedCustomSubs) {
                if (customSubsMap.keys.contains(id)) {
                    customSubsMap[id]?.selected = true
                } else {
                    selectedCustomSubs.remove(id)
                }
            }
        } catch (ex: Exception) {
            println(ex.printStackTrace())
            // Set selectedString sub to awwnime only
            selectedSubs = HashSet()
            selectedCustomSubs = HashSet()
            selectedSubs.add("awwnime")
        }

        subsList = ArrayList(subsMap.values.filter{ s -> !s.isNSFW})
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
        prefsEditor.putString("SELECTED_SUBS", gson.toJson(selectedSubs, hashSetMap))
        prefsEditor.putString("SELECTED_CUSTOM_SUBS", gson.toJson(selectedCustomSubs, hashSetMap))
        prefsEditor.apply()
        overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
        startActivity(Intent(this, Main::class.java))
        overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
        finish()
    }

    private fun updateTabs() {
        mTabLayout.getTabAt(0)!!.text = getString(R.string.all_tab) + " (${allShownSubsCount})"
        mTabLayout.getTabAt(1)!!.text = getString(R.string.selected_tab) + " (${selectedShownSubsCount})"
    }

    private fun displayList() {
        adp = CustomAdapter(this, R.layout.activity_settings_subs_checkboxes, subsList)
        val lv = findViewById<View>(R.id.listView) as ListView
        lv.adapter = adp
        val editSearch = findViewById<View>(R.id.search) as EditText
        val text = editSearch.text.toString().toLowerCase(Locale.getDefault())
        adp!!.filter(text, mTabLayout.selectedTabPosition == 1)

        // Locate the EditText in activity_settings_subs.xml

        // Locate the TabLayout in activity_settings_subs.xml

        mTabLayout.addOnTabSelectedListener(object :
                TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val text = editSearch.text.toString().toLowerCase(Locale.getDefault())
                adp!!.filter(text, mTabLayout.selectedTabPosition == 1)
                updateTabs()
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
                updateTabs()
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
            subsToolbar.title = getString(R.string.subreddits)
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
            val viewSub = parent.getItemAtPosition(position) as Sub
            val cb = view.findViewById<View>(R.id.checkBox) as CheckBox

            val isSelected = selectedSubs.contains(viewSub.subName) || selectedCustomSubs.contains(viewSub.subName)
            if (isSelected) {
                selectedShownSubsCount--
                if (viewSub.isCustom) {
                    selectedCustomSubs.remove(viewSub.subName)
                } else {
                    selectedSubs.remove(viewSub.subName)
                }

                // If we are showing only selected subs, update the ListView adapter data
                // TODO: refactor with enum
                if (mTabLayout.selectedTabPosition == 1) {
                    subsList.remove(viewSub)
                    adp!!.notifyDataSetChanged()
                }
            } else {
                selectedShownSubsCount++
                if (viewSub.isCustom) {
                    selectedCustomSubs.add(viewSub.subName)
                } else {
                    selectedSubs.add(viewSub.subName)
                }
            }
            updateTabs()
            cb.isChecked = !isSelected
            viewSub.selected = !isSelected
            adp!!.notifyDataSetChanged()
        }

        lv.onItemLongClickListener = AdapterView.OnItemLongClickListener { parent, _, position, _ ->
            val sub = parent.getItemAtPosition(position) as Sub
            val d1: AlertDialog
            if (sub.isCustom) {
                d1 = AlertDialog.Builder(ContextThemeWrapper(this@SubSelector, if (darkmode) R.style.AppThemeDark else R.style.AppThemeLight))
                        .setTitle(getString(R.string.about) + " " + sub.subName + ":")
                        .setNegativeButton(R.string.back) { _, _ ->
                            // Do nothing
                        }
                        .setNeutralButton(R.string.delete) { _, _ ->
                            customSubsMap.remove(sub.subName)
                            val serial = gson.toJson(customSubsMap, intSubMap)
                            prefsEditor.putString("CUSTOM_SUBS", serial)

                            selectedCustomSubs.remove(sub.subName)
                            prefsEditor.putString("SELECTED_CUSTOM_SUBS", gson.toJson(selectedCustomSubs, hashSetMap))

                            prefsEditor.apply()
                            origList.remove(sub)
                            subsList.remove(sub)

                            adp!!.notifyDataSetChanged()

                            runOnUiThread {
                                displayList()
                                updateTabs()
                                Toast.makeText(this@SubSelector, getString(R.string.removed) + " " + sub.subName, Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setMessage(if (sub.desc.isNotEmpty()) sub.desc else getString(R.string.no_description))
                        .create()
            } else {
                d1 = AlertDialog.Builder(ContextThemeWrapper(this@SubSelector, if (darkmode) R.style.AppThemeDark else R.style.AppThemeLight))
                        .setTitle(getString(R.string.about) + " " + sub.subName + ":")
                        .setNegativeButton(R.string.back) { _, _ ->
                            // Do nothing
                        }
                        .setMessage(if (sub.desc.isNotEmpty()) sub.desc else getString(R.string.no_description))
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
            response.append("Saved: \n")
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
            holder.name!!.isChecked = !sb.isCustom && selectedSubs.contains(sb.subName) || sb.isCustom && selectedCustomSubs.contains(sb.subName)
            holder.name!!.tag = sb
            holder.isNSFW!!.text = if (sb.isNSFW) getString(R.string.nsfw) else ""
            holder.subscribers!!.text = Formatter.shortHandFormatter(sb.subscriberCount)
            return convertView
        }

        // Filter Class
        fun filter(searchString: String, onlySelected: Boolean) {
            var lower = searchString.toLowerCase(Locale.getDefault())
            var tmp: ArrayList<Sub> = ArrayList<Sub>()
            var selected: ArrayList<Sub>
            subsList.clear()
            tmp.addAll(origList)

            // Hide NSFW subs by default, exclude custom
            if (!showNsfw) {
                tmp = tmp.filter { s -> !s.isNSFW || s.isCustom } as ArrayList<Sub>
            }

            // Has search term
            if (!lower.isEmpty()) {
                tmp = tmp.filter { s -> s.subName.contains(lower) } as ArrayList<Sub>
            }

            // Only selected
            allShownSubsCount = tmp.size
            selected = tmp.filter { s -> selectedSubs.contains(s.subName) || selectedCustomSubs.contains(s.subName) } as ArrayList<Sub>
            selectedShownSubsCount = selected.size

            // Only show selected subs
            if (onlySelected) {
                tmp = selected
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
            // Parse subreddit names
            if (customSubName.isEmpty())
                return -1
            // if begins with /r/, remove first slash
            if (customSubName.startsWith("/r/"))
                customSubName = customSubName.removePrefix("/")
            // else prefix with r/
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
                val aboutURL = "https://www.reddit.com/r/$customSubName/about.json"
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
                    val newSub = Sub(
                            customSubName,
                            obj.getInt("subscribers"),
                            false,
                            obj.getBoolean("over18"),
                            true,
                            obj.getString("public_description")
                    )
                    customSubsMap[customSubName] = newSub
                    selectedCustomSubs.add(customSubName)
                    origList.add(newSub)
                    subsList.add(newSub)

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
//            var msg: String? = null
            when (result) {
                0 -> addSuccess.add(customSubName)
                -1 -> addFailed.add(customSubName)  // Blank subreddit
                -2 -> addExisting.add(customSubName)  // Subreddit already been added
                -3 -> addFailed.add(customSubName)  // Does not exist or is empty
                -4 -> addFailed.add(customSubName)  // Other error
            }

            // Last task?
            if (addSuccess.size + addExisting.size + addFailed.size == addCount) {
                subsList.sort()
                prefsEditor.putString("CUSTOM_SUBS", gson.toJson(customSubsMap, intSubMap))
                prefsEditor.putString("SELECTED_CUSTOM_SUBS", gson.toJson(selectedCustomSubs, hashSetMap))

                prefsEditor.apply()
                var msg = ""
                if (addSuccess.size > 0) {
                    msg += "Successfully added ${addSuccess[0]}${if (addSuccess.size > 1) " and ${addSuccess.size - 1} others" else ""}"
                }

                if (addExisting.size > 0) {
                    if (!msg.isEmpty())
                        msg += "\n"
                    msg += "${addExisting[0]}${if (addExisting.size > 1) " and ${addExisting.size - 1} others" else ""} already exist!"
                }

                if (addFailed.size > 0) {
                    if (!msg.isEmpty())
                        msg += "\n"
                    msg += "Failed to add ${addFailed[0]}${if (addFailed.size > 1) " and ${addFailed.size - 1} others" else ""}"
                }

                Toast.makeText(applicationContext,
                        msg,
                        Toast.LENGTH_LONG).show()
                progressDialog.dismiss()
            }
        }

        override fun onPreExecute() {

        }

        override fun onProgressUpdate(vararg values: Int?) {

        }
    }
}
