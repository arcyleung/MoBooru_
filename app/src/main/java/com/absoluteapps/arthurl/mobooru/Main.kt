package com.absoluteapps.arthurl.mobooru

import android.Manifest
import android.annotation.TargetApi
import android.app.*
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.*
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.etsy.android.grid.StaggeredGridView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URL
import java.util.*
import kotlin.math.ceil
import kotlin.system.exitProcess

class Main : AppCompatActivity() {

    // Core config
    private var appName = "MoBooru"
    private lateinit var verString: String
    internal val progressBarScale = 100
    private var thumbnailSize = 300
    private var pageSize = 30
    internal var currentPage = 1
    internal var mainsite = "https://redditbooru.com"
    internal lateinit var url: URL
    internal lateinit var display: Display
    internal var screenWidth = 0
    internal var screenHeight = 0
    internal var bitmapWidth = 0
    internal var bitmapHeight = 0
    internal var displayMetrics = DisplayMetrics()

    // Site config
    internal var selectedString = ""
    private var showNsfw: Boolean = false
    private var showTitles: Boolean = false
    internal var selectedURL = "https://redditbooru.com/images/?sources=$selectedString&afterDate="
    internal lateinit var doc: Document
    internal lateinit var redditSubs: Elements
    internal var subsJSON = ""
    internal lateinit var arr: JSONArray
    internal var datas = ArrayList<Data>()
    internal var favorites = ArrayList<Data>()
    private lateinit var lm: LoadMorePhotos
    internal var loadingMore = true
    private var viewingFavorites: Boolean = false

    // UI, Views, Layout
    internal lateinit var inDetProgressBar: ProgressBar
    internal lateinit var detProgressBar: ProgressBar
    internal var progressDialog: ProgressDialog? = null
    internal var dialog: Dialog? = null
    internal lateinit var toolbar: Toolbar
    internal lateinit var closeFAB: FloatingActionButton
    internal lateinit var expandFAB: FloatingActionButton
    internal lateinit var wallpaperFAB: FloatingActionButton
    internal lateinit var shareFAB: FloatingActionButton
    internal lateinit var saveFAB: FloatingActionButton
    internal lateinit var sourceFAB: FloatingActionButton
    internal lateinit var favoriteFAB: FloatingActionButton
    internal lateinit var unfavoriteFAB: FloatingActionButton
    internal var isExpanded: Boolean = false
    private lateinit var navigationView: NavigationView
    private lateinit var staggeredGridView: StaggeredGridView
    internal lateinit var drawerLayout: DrawerLayout
    private lateinit var swipeContainer: SwipeRefreshLayout

    // Sharedprefs, serialization, storage
    internal var subsMap = HashMap<Int, Sub>()
    internal var customSubsMap = HashMap<Int, Sub>()
    internal var selectedSubs = HashSet<Int>()
    internal var selectedCustomSubs = HashSet<Int>()
    internal var nextPages = HashMap<Int, String>()
    internal var lastIndexTime: Long = 0
    internal var timeToUpdate = false
    internal var gson = Gson()
    internal var intSubMap = object : TypeToken<Map<Int, Sub>>() {

    }.type
    private var intSet = object : TypeToken<Set<Int>>() {

    }.type
    internal var dataList = object : TypeToken<ArrayList<Data>>() {

    }.type
    internal lateinit var prefs: SharedPreferences
    internal lateinit var prefsEditor: SharedPreferences.Editor
    private var externalStorageDirectory = Environment.getExternalStorageDirectory()
    internal var currentImg: Bitmap? = null
    private var adapter: DataAdapter? = null

    private val isNetworkAvailable: Boolean
        get() {
            val connectivityManager = getSystemService(Activity.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_main)
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        navigationView = findViewById<View>(R.id.navigationView) as NavigationView
        drawerLayout = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        detProgressBar = findViewById<View>(R.id.determinate_progress_bar) as ProgressBar
        detProgressBar.max = 100 * progressBarScale
        detProgressBar.visibility = View.GONE

        inDetProgressBar = findViewById<View>(R.id.indeterminate_progress_bar) as ProgressBar
        inDetProgressBar.visibility = View.GONE
        inDetProgressBar.visibility = View.GONE

        toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        val mDrawerToggle: ActionBarDrawerToggle
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            mDrawerToggle = object : ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

                override fun onDrawerClosed(drawerView: View) {
                    supportInvalidateOptionsMenu()
                    //drawerOpened = false;
                }

                override fun onDrawerOpened(drawerView: View) {
                    supportInvalidateOptionsMenu()
                    //drawerOpened = true;
                }
            }
            mDrawerToggle.setDrawerIndicatorEnabled(true)
            drawerLayout.setDrawerListener(mDrawerToggle)
            mDrawerToggle.syncState()
        }

        supportActionBar!!.setDisplayShowHomeEnabled(true)

        // Sharedprefs
        try {
            prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
            prefsEditor = prefs.edit()
            // Get saved subs
            subsMap = gson.fromJson(prefs.getString("SUBS", "{1: {customSubName: 'Awwnime', subID: 1, subscriberCount: 0, selected: true, isNSFW: false, desc: ''}}"), intSubMap)
            customSubsMap = gson.fromJson(prefs.getString("CUSTOM_SUBS", "{}"), intSubMap)
            selectedSubs = gson.fromJson(prefs.getString("SELECTED_SUBS", "[1]"), intSet)
            selectedCustomSubs = gson.fromJson(prefs.getString("SELECTED_CUSTOM_SUBS", "[]"), intSet)
            favorites = gson.fromJson(prefs.getString("FAVORITES", "[]"), dataList)
            showNsfw = prefs.getBoolean("SHOW_NSFW", false)
            toolbar.visibility = if (!prefs.getBoolean("FULLSCREEN", false)) {
                View.VISIBLE
            } else View.GONE
            System.out.println("Visibility: " + toolbar.visibility)
            showTitles = prefs.getBoolean("SHOW_TITLES", true)
            thumbnailSize = prefs.getInt("THUMBNAIL_SIZE", 300)

            // Fullscreen to false by default
            if (!prefs.contains("FULLSCREEN")) {
                prefsEditor.putBoolean("FULLSCREEN", false).apply()
            }
            if (prefs.getBoolean("FULLSCREEN", false)) {
                immersiveFullscreen()
            } else {
                nonFullscreen()
            }

            // Show titles to true by default
            if (!prefs.contains("SHOW_TITLES")) {
                prefsEditor.putBoolean("SHOW_TITLES", true).apply()
            }

            // Thumbnail size is 300 by default
            if (!prefs.contains("THUMBNAIL_SIZE")) {
                prefsEditor.putInt("THUMBNAIL_SIZE", 300).apply()
            }

            // Time to reindex subs
            if (!prefs.contains("UPDATE_TIME") || System.currentTimeMillis() - prefs.getLong("UPDATE_TIME", 0) > 604800000) {
                timeToUpdate = true
            }

            // Show help snackbar on first launch
            if (prefs.getBoolean("FIRST_LAUNCH", true)) {
                val snackbar = Snackbar
                        .make(drawerLayout, "Would you like to view the help guide?", 20000)
                        .setAction("YES!") {
                            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
                            startActivity(Intent(applicationContext, Help::class.java))
                            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
                        }

                snackbar.show()
                prefsEditor.putBoolean("FIRST_LAUNCH", false).commit()
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
            timeToUpdate = true
        }

        viewingFavorites = if (intent.getSerializableExtra("viewingFavorites") == null) false else intent.getSerializableExtra("viewingFavorites") as Boolean
        setFavoriteSubs()

        navigationView.setNavigationItemSelectedListener(NavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_subs -> {
                    if (!timeToUpdate) {
                        startActivity(Intent(this@Main, SubSelector::class.java).putExtra("subsMap", subsMap).putExtra("customSubsMap", customSubsMap))
                        finish()
                    } else {
                        Toast.makeText(applicationContext,
                                "Please wait until indexing finishes...",
                                Toast.LENGTH_LONG).show()
                    }
                    return@OnNavigationItemSelectedListener true
                }

                R.id.nav_back -> {
                    restartMain(false)
                    return@OnNavigationItemSelectedListener true
                }

                R.id.nav_favorites -> {
                    if (!timeToUpdate) {
                        restartMain(true)
                    } else {
                        Toast.makeText(applicationContext,
                                "Please wait until indexing finishes...",
                                Toast.LENGTH_LONG).show()
                    }
                    return@OnNavigationItemSelectedListener true
                }

                R.id.columns -> {
                    val display = (this@Main.getSystemService(Activity.WINDOW_SERVICE) as WindowManager).defaultDisplay
                    val d2 = AlertDialog.Builder(this@Main)
                    val inflater = layoutInflater
                    val dialogView = inflater.inflate(R.layout.number_picker, null)

                    val numberPicker = dialogView.findViewById<View>(R.id.dialog_number_picker) as NumberPicker
                    numberPicker.maxValue = MAX_COLUMNS_PORTRAIT
                    numberPicker.minValue = MIN_COLUMNS_PORTRAIT

                    d2.setTitle("Number of columns")
                    if (display.rotation == Surface.ROTATION_0) {
                        numberPicker.maxValue = MAX_COLUMNS_PORTRAIT
                        numberPicker.minValue = MIN_COLUMNS_PORTRAIT
                        d2.setMessage("Default portrait: $DEFAULT_COLUMNS_PORTRAIT")
                        numberPicker.value = prefs.getInt("COLUMNS_PORTRAIT", DEFAULT_COLUMNS_PORTRAIT)
                    } else {
                        numberPicker.maxValue = MAX_COLUMNS_LANDSCAPE
                        numberPicker.minValue = MIN_COLUMNS_LANDSCAPE
                        d2.setMessage("Default landscape: $DEFAULT_COLUMNS_LANDSCAPE")
                        numberPicker.value = prefs.getInt("COLUMNS_LANDSCAPE", DEFAULT_COLUMNS_LANDSCAPE)
                    }
                    d2.setView(dialogView)

                    numberPicker.wrapSelectorWheel = false
                    d2.setPositiveButton("Done", object : DialogInterface.OnClickListener {
                        var rotation = display.rotation

                        override fun onClick(dialogInterface: DialogInterface, i: Int) {
                            if (rotation == Surface.ROTATION_0) {
                                prefsEditor.putInt("COLUMNS_PORTRAIT", numberPicker.value)
                                if (numberPicker.value == 1)
                                    prefsEditor.putInt("THUMBNAIL_SIZE", 600)
                                else
                                    prefsEditor.putInt("THUMBNAIL_SIZE", 300)
                            } else {
                                prefsEditor.putInt("THUMBNAIL_SIZE", 300)
                                prefsEditor.putInt("COLUMNS_LANDSCAPE", numberPicker.value)
                            }
                            prefsEditor.apply()
                            initializeAdapter()
                            finish()
                            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
                            startActivity(Intent(applicationContext, Main::class.java))
                            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
                        }
                    })
                    d2.setNegativeButton("Cancel") { _, _ -> }
                    val alertDialog = d2.create()
                    alertDialog.show()

                    return@OnNavigationItemSelectedListener true
                }

                R.id.nav_help -> {
                    overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
                    startActivity(Intent(this, Help::class.java))
                    overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
                    return@OnNavigationItemSelectedListener true
                }

                R.id.nav_about -> {
                    try {
                        val pInfo = this@Main.packageManager.getPackageInfo(packageName, 0)
                        verString = pInfo.versionName
                    } catch (e: PackageManager.NameNotFoundException) {
                        e.printStackTrace()
                    }

                    val d1 = AlertDialog.Builder(this@Main)
                            .setTitle("About")
                            .setNegativeButton("Back") { _, _ ->
                                // do nothing
                            }
                            .setIcon(R.drawable.ic_launcher)
                            .setMessage(Html.fromHtml(appName + " v" + verString + "<br>Author: arcyleung<br/><a href=\"http://arcyleung.com\">http://arcyleung.com</a><br/><br/>Follow me on <a href=\"https://twitter.com/arcyleung\">Twitter</a> or <a href=\"https://www.linkedin.com/in/arcyleung/\">LinkedIn</a>! <br/> <br/> Please direct any questions about development to <a href=\"mailto:arcyleung@gmail.com?Subject=MoBooru Inquiry\" target=\"_top\">arcyleung@gmail.com</a>\n" +
                                    "</p> or if you want to help support development!"))
                            .setOnDismissListener {
                                if (prefs.getBoolean("FULLSCREEN", false)) {
                                    immersiveFullscreen()
                                } else {
                                    nonFullscreen()
                                }
                            }
                            .create()

                    d1.show()

                    (d1.findViewById<View>(android.R.id.message) as TextView).movementMethod = LinkMovementMethod.getInstance()
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        })
        //
        display = windowManager.defaultDisplay

        selectedURL = "https://redditbooru.com/images/?sources=$selectedString"

        if (!isNetworkAvailable) {
            val d = AlertDialog.Builder(this@Main)
                    .setTitle("No network connection")
                    .setNegativeButton("Quit") { _, _ ->
                        moveTaskToBack(true)
                        Process.killProcess(Process.myPid())
                        exitProcess(1)
                    }
                    .setIcon(R.drawable.ic_launcher)
                    .setMessage("Sorry, please connect to Wi-Fi or cellular service and try again later!")
                    .create()

            d.show()
            return
        }
        swipeContainer = findViewById<View>(R.id.swipe_container) as SwipeRefreshLayout
        if (!viewingFavorites) {
            swipeContainer.isEnabled = true
            swipeContainer.setOnRefreshListener {
                // Implement refresh adapter code
                finish()
                overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
                startActivity(Intent(applicationContext, Main::class.java))
                overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
            }

            swipeContainer.setColorSchemeResources(android.R.color.holo_red_light)
        } else {
            swipeContainer.isEnabled = false
        }

        // Image grid adapter
        initializeAdapter()

        // Set icon click listener
        setOnClickListener()

        ViewCompat.setNestedScrollingEnabled(staggeredGridView, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            staggeredGridView.isNestedScrollingEnabled = true
        }

        if (timeToUpdate) {
            executeAsyncTask(FetchSubs())
            Toast.makeText(applicationContext,
                    "Updating subreddit index...",
                    Toast.LENGTH_LONG).show()
        }

        this@Main.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER

        calcScreenSize()
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
    }

    fun initializeAdapter() {
        try {
            adapter = DataAdapter(this, R.layout.staggered, datas, showNsfw, showTitles)
            title = Html.fromHtml("<font color='#ffffff'>" + (if (viewingFavorites) "Favorites" else appName) + "</font>")
            staggeredGridView = findViewById<View>(R.id.gridView) as StaggeredGridView
            staggeredGridView.adapter = adapter

            if (!viewingFavorites) {

                lm = LoadMorePhotos()
                lm.execute()
                staggeredGridView.setOnScrollListener(object : EndlessScrollListener() {
                    override fun onLoadMore(page: Int, totalItemsCount: Int) {
                        executeAsyncTask(LoadMorePhotos())
                    }
                })
            } else {
                datas = gson.fromJson(prefs.getString("FAVORITES", "[]"), dataList)
                if (datas.size == 0) {
                    val d1: AlertDialog
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        d1 = AlertDialog.Builder(this@Main)
                                .setTitle("Favorites")
                                .setNegativeButton("Back") { _, _ ->
                                    // do nothing
                                }
                                .setMessage("You have not added any favorites yet. To add a favorite, press the heart button when viewing an image!")
                                .setOnDismissListener { restartMain(false) }
                                .create()

                        d1.show()
                    } else {
                        Toast.makeText(applicationContext,
                                "You have not added any favorites yet.",
                                Toast.LENGTH_LONG).show()
                    }
                } else {
                    adapter = DataAdapter(this, R.layout.staggered, datas, showNsfw, showTitles)
                    staggeredGridView.adapter = adapter
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onPause() {
        if (dialog != null)
            dialog!!.dismiss()
        if (progressDialog != null)
            progressDialog!!.dismiss()
        super.onPause()
    }

    override fun onDestroy() {
        if (dialog != null)
            dialog!!.dismiss()
        if (progressDialog != null)
            progressDialog!!.dismiss()
        super.onDestroy()
    }

    private fun calcScreenSize() {
        val outPoint = Point()
        if (Build.VERSION.SDK_INT >= 19) {
            // include navigation bar
            display.getRealSize(outPoint)
        } else {
            // exclude navigation bar
            display.getSize(outPoint)
        }
        val res = this@Main.resources
        val resourceId = this@Main.resources.getIdentifier("status_bar_height", "dimen", "android")

        screenHeight = outPoint.y
        screenWidth = outPoint.x


        if (resourceId > 0)
            screenHeight -= res.getDimensionPixelSize(resourceId)
    }

    fun nonFullscreen() {
        window.decorView.systemUiVisibility = 0
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun immersiveFullscreen() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
    }

    private fun setOnClickListener() {
        staggeredGridView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            dialog = Dialog(this@Main)
            dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
            val selected = datas[position]
            val zoomImageView = InteractiveImageView(this@Main)
            isExpanded = false
            progressDialog = ProgressDialog.show(this@Main, "Downloading", "...", true)

            object : Thread() {
                @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
                override fun run() {
                    var img: Bitmap? = null
                    try {
                        val tmp = DownloadImage(zoomImageView).execute(selected.imgUrl).get()
                        if (tmp == null) {
                            Toast.makeText(applicationContext,
                                    "Error rendering image: file corrupted or too large",
                                    Toast.LENGTH_LONG).show()
                        } else {
                            img = tmp
                            bitmapWidth = img.width
                            bitmapHeight = img.height
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    dialog!!.setOnCancelListener {
                        if (prefs.getBoolean("FULLSCREEN", false)) {
                            immersiveFullscreen()
                        } else {
                            nonFullscreen()
                        }
                    }

                    try {
                        // code runs in a thread
                        val finalImg = img
                        runOnUiThread {
                            dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE) //before
                            dialog!!.setContentView(R.layout.popup_imgview)
                            dialog!!.window!!.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                            //                                    dialog.getWindow().setFeatureDrawable();

                            val image = dialog!!.findViewById<View>(R.id.imageview) as InteractiveImageView
                            image.setImageBitmap(finalImg)

                            dialog!!.window!!.setLayout(screenWidth, screenHeight)
                            dialog!!.window!!.setDimAmount(.9f)
                            dialog!!.show()

                            closeFAB = dialog!!.findViewById<View>(R.id.imageClose) as FloatingActionButton
                            expandFAB = dialog!!.findViewById<View>(R.id.fab) as FloatingActionButton
                            wallpaperFAB = dialog!!.findViewById<View>(R.id.fab1) as FloatingActionButton
                            shareFAB = dialog!!.findViewById<View>(R.id.fab2) as FloatingActionButton
                            saveFAB = dialog!!.findViewById<View>(R.id.fab3) as FloatingActionButton
                            sourceFAB = dialog!!.findViewById<View>(R.id.fab4) as FloatingActionButton
                            favoriteFAB = dialog!!.findViewById<View>(R.id.fab5) as FloatingActionButton
                            unfavoriteFAB = dialog!!.findViewById<View>(R.id.fab6) as FloatingActionButton

                            val favorited = gson.toJson(favorites, dataList).contains(gson.toJson(selected))

                            if (favorited) {
                                // Is in favorites
                                favoriteFAB.hide()
                                unfavoriteFAB.show()
                                if (display.rotation == Surface.ROTATION_0) {
                                    // Vertical
                                    val portTranslate = displayMetrics.heightPixels * 0.09f
                                    unfavoriteFAB.animate().translationX(-portTranslate)
                                } else {
                                    // Horizontal
                                    val landTranslate = displayMetrics.heightPixels * 0.15f
                                    unfavoriteFAB.animate().translationX(-landTranslate)
                                }
                            } else {
                                favoriteFAB.show()
                                unfavoriteFAB.hide()
                                if (display.rotation == Surface.ROTATION_0) {
                                    // Vertical
                                    val portTranslate = displayMetrics.heightPixels * 0.09f
                                    favoriteFAB.animate().translationX(-portTranslate)
                                } else {
                                    // Horizontal
                                    val landTranslate = displayMetrics.heightPixels * 0.15f
                                    favoriteFAB.animate().translationX(-landTranslate)
                                }
                            }


                            expandFAB.setOnClickListener {
                                if (!isExpanded) {
                                    showFABMenu()
                                } else {
                                    closeFABMenu()
                                }
                            }

                            wallpaperFAB.setOnClickListener {
                                val d = AlertDialog.Builder(this@Main)
                                        .setTitle("Confirm")
                                        .setMessage("Do you want to set this wallpaper?")
                                        .setIcon(getDrawable(R.drawable.ic_wallpaper_white_48dp))
                                        .setPositiveButton(android.R.string.yes) { _, _ ->
                                            progressDialog = ProgressDialog.show(this@Main, "Setting Wallpaper", "...", true)

                                            object : Thread() {
                                                override fun run() {
                                                    val wallMan = WallpaperManager.getInstance(applicationContext)
                                                    Looper.prepare()
                                                    try {
                                                        wallMan.setBitmap(finalImg)
                                                        progressDialog!!.dismiss()

                                                        this@Main.runOnUiThread {
                                                            Toast.makeText(applicationContext,
                                                                    "Wallpaper set!",
                                                                    Toast.LENGTH_LONG).show()
                                                        }

                                                    } catch (e: Exception) {
                                                        progressDialog!!.dismiss()
                                                        this@Main.runOnUiThread {
                                                            Toast.makeText(applicationContext,
                                                                    "Failed to set wallpaper.",
                                                                    Toast.LENGTH_LONG).show()
                                                        }
                                                    }

                                                }
                                            }.start()
                                        }
                                        .setNegativeButton(android.R.string.no, null).show()
                                d.window!!.setDimAmount(.8f)
                                dialog!!.window!!.setDimAmount(.8f)
                                d.show()
                            }

                            shareFAB.setOnClickListener {
                                val options = arrayOf("Share image", "Share image link", "Share Reddit link")
                                val d = AlertDialog.Builder(this@Main)
                                        .setTitle("Share")
                                        .setItems(options) { _, which ->
                                            when (which) {
                                                0 ->
                                                    // Perms
                                                    if (Build.VERSION.SDK_INT >= 23) {
                                                        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                                            shareWallpaperExtStorage(finalImg)
                                                        } else {
                                                            currentImg = finalImg
                                                            ActivityCompat.requestPermissions(this@Main, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 2)
                                                        }
                                                    } else {
                                                        shareWallpaperExtStorage(finalImg)
                                                    }
                                                1 -> shareWallpaperLink(selected.imgUrl)
                                                2 -> shareWallpaperLink(selected.redditSrc)
                                            }
                                        }
                                        .create()
                                d.window!!.setDimAmount(.8f)
                                d.show()
                            }

                            saveFAB.setOnClickListener {
                                // Perms
                                if (Build.VERSION.SDK_INT >= 23) {
                                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                        saveToStorage(finalImg)
                                    } else {
                                        currentImg = finalImg
                                        ActivityCompat.requestPermissions(this@Main, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                                    }
                                } else {
                                    saveToStorage(finalImg)
                                }
                            }

                            sourceFAB.setOnClickListener {
                                try {
                                    // Browser Intent
                                    val redditPost = Intent(Intent.ACTION_VIEW)

                                    // Type of file to share
                                    redditPost.data = Uri.parse(selected.redditSrc)
                                    startActivity(redditPost)

                                } catch (e: Exception) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace()
                                }
                            }

                            favoriteFAB.setOnClickListener {
                                try {
                                    if (favorites.contains(selected)) {
                                        Toast.makeText(applicationContext,
                                                "Already in favorites!",
                                                Toast.LENGTH_LONG).show()
                                    } else {
                                        favorites.add(selected)
                                        val serial = gson.toJson(favorites, dataList)
                                        prefsEditor.putString("FAVORITES", serial)
                                        prefsEditor.apply()
                                        Toast.makeText(applicationContext,
                                                "Added to favorites!",
                                                Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace()
                                }
                            }

                            unfavoriteFAB.setOnClickListener {
                                try {
                                    //                                                String serial = gson.toJson(favorites, dataList);
                                    datas.removeAt(position)
                                    val serial = gson.toJson(datas, dataList)
                                    prefsEditor.putString("FAVORITES", serial)
                                    prefsEditor.apply()
                                    adapter!!.notifyDataSetChanged()
                                    dialog!!.dismiss()
                                    if (datas.size == 0) {
                                        restartMain(false)
                                    }
                                    Toast.makeText(applicationContext,
                                            "Removed from favorites",
                                            Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            closeFAB.setOnClickListener {
                                isExpanded = false
                                dialog!!.dismiss()
                                if (prefs.getBoolean("FULLSCREEN", false)) {
                                    immersiveFullscreen()
                                } else {
                                    nonFullscreen()
                                }
                            }
                            progressDialog!!.dismiss()
                        }
                    } catch (ex: Exception) {

                    }
                }
            }.start()
        }
    }

    private fun showFABMenu() {
        expandFAB.animate().rotation(45f)
        isExpanded = true

        if (display.rotation == Surface.ROTATION_0) {
            // Vertical
            val portTranslate = displayMetrics.heightPixels * 0.09f
            wallpaperFAB.animate().translationY(-portTranslate)
            shareFAB.animate().translationY(2 * -portTranslate)
            saveFAB.animate().translationY(3 * -portTranslate)
            sourceFAB.animate().translationY(4 * -portTranslate)
        } else {
            // Horizontal
            val landTranslate = displayMetrics.heightPixels * 0.15f
            wallpaperFAB.animate().translationY(-landTranslate)
            shareFAB.animate().translationY(2 * -landTranslate)
            saveFAB.animate().translationY(3 * -landTranslate)
            sourceFAB.animate().translationY(4 * -landTranslate)
        }
    }

    private fun closeFABMenu() {
        isExpanded = false
        expandFAB.animate().rotation(0f)
        wallpaperFAB.animate().translationY(0f)
        shareFAB.animate().translationY(0f)
        saveFAB.animate().translationY(0f)
        sourceFAB.animate().translationY(0f)
    }

    // External storage
    private fun fixMediaDir() {
        val sdcard = Environment.getExternalStorageDirectory()
        if (sdcard != null) {
            val mediaDir = File(sdcard, "DCIM/Camera")
            if (!mediaDir.exists()) {
                mediaDir.mkdirs()
            }
        }
    }

    // todo: add time filter

    // Rebuild index with new sub metadata
    //
    private fun setFavoriteSubs() {
        selectedString = ""
        for (i in selectedSubs) {
            selectedString += "$i%2C"
        }
    }

    override fun onBackPressed() {
        if (viewingFavorites) {
            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
            restartMain(false)
            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
        } else {
            finish()
        }
    }

    fun restartMain(viewingFavorites: Boolean) {
        val intent = intent
        intent.putExtra("viewingFavorites", viewingFavorites)
        finish()
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)

        navigationView.menu.findItem(R.id.nav_subs).isVisible = !viewingFavorites
        navigationView.menu.findItem(R.id.nav_favorites).isVisible = !viewingFavorites
        navigationView.menu.findItem(R.id.nav_back).isVisible = viewingFavorites

        val nsfwToggle = (navigationView.findViewById<View>(R.id.nsfw_toggle) as RelativeLayout).getChildAt(0) as SwitchCompat
        val fullScreenToggle = (navigationView.findViewById<View>(R.id.fullscreen_toggle) as RelativeLayout).getChildAt(0) as SwitchCompat
        val titlesToggle = (navigationView.findViewById<View>(R.id.title_toggle) as RelativeLayout).getChildAt(0) as SwitchCompat
        nsfwToggle.isChecked = prefs.getBoolean("SHOW_NSFW", false)
        fullScreenToggle.isChecked = prefs.getBoolean("FULLSCREEN", false)
        titlesToggle.isChecked = prefs.getBoolean("SHOW_TITLES", true)

        nsfwToggle.setOnClickListener {
            prefsEditor.putBoolean("SHOW_NSFW", !prefs.getBoolean("SHOW_NSFW", false))
            prefsEditor.apply()
            val intent = intent
            finish()
            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
            startActivity(intent)
            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
        }
        fullScreenToggle.setOnClickListener {
            prefsEditor.putBoolean("FULLSCREEN", !prefs.getBoolean("FULLSCREEN", false))
            prefsEditor.apply()
            val intent = intent
            finish()
            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
            startActivity(intent)
            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
        }
        titlesToggle.setOnClickListener {
            prefsEditor.putBoolean("SHOW_TITLES", !prefs.getBoolean("SHOW_TITLES", true))
            prefsEditor.apply()
            val intent = intent

            finish()
            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
            startActivity(intent)
            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
        }
        return true
    }

    fun addToArry(ja: JSONArray?) {
        if (loadingMore) {

            //IMPLEMENT STOP LOADING ONCE ARRAYSIZE < PAGESIZE
            if (ja == null) {
                loadingMore = false
            } else {
                if (ja.length() < pageSize) {
                    pageSize = ja.length()
                }
                for (i in 0 until pageSize) {
                    val data = Data()
                    try {
                        // Image data
                        data.imgUrl = ja.getJSONObject(i).getString("cdnUrl")
                        data.width = ja.getJSONObject(i).getInt("width")
                        data.height = ja.getJSONObject(i).getInt("height")
                        data.rat = 1.0 * data.height / data.width
                        data.thumbImgUrl = ja.getJSONObject(i).getString("thumb") + "_" + thumbnailSize + "_" + thumbnailSize + ".jpg"

                        // Metadata
                        data.score = Formatter.shortHandFormatter(Integer.parseInt(ja.getJSONObject(i).getString("score")))
                        data.nsfw = ja.getJSONObject(i).getBoolean("nsfw")
                        data.redditSrc = "https://redd.it/" + ja.getJSONObject(i).getString("externalId")
                        data.title = ja.getJSONObject(i).getString("title").replace("\\s*\\[.+?]\\s*".toRegex(), "").replace("&amp;", "&") //+"\n("+data.score+"\uD83D\uDD3A)";
                        data.series = ja.getJSONObject(i).getString("title").replace("^[^\\[]*".toRegex(), "").replace("&amp;", "&")

                        if (i == pageSize - 1) {
                            lastIndexTime = java.lang.Long.parseLong(ja.getJSONObject(i).getString("dateCreated"))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    if (data.ogSrc == "null") {
                        data.ogSrc = ""
                    }
                    if (data.thumbImgUrl == "null") {
                        data.thumbImgUrl = ""
                    }
                    datas.add(data)
                }
            }
        }
        return
    }

    fun addToArryDirect(jas: Array<JSONArray?>) {
        if (loadingMore) {

            //IMPLEMENT STOP LOADING ONCE ARRAYSIZE < PAGESIZE
            if (jas.isEmpty()) {
                loadingMore = false
            } else {
                for (i in jas.indices) {
                    if (jas[i] == null)
                        continue
                    for (j in 0 until jas[i]!!.length()) {
                        try {
                            val post = (jas[i]!!.get(j) as JSONObject).get("data") as JSONObject
                            val url = post.getString("url")
                            if (url.contains(".png") || url.contains(".jpg")) {
                                val data = Data()
                                data.imgUrl = url
                                val previews = (post.getJSONObject("preview").getJSONArray("images") as JSONArray).get(0) as JSONObject
                                val resolutions = previews.getJSONArray("resolutions")
                                val preview = resolutions.get(resolutions.length() / 2) as JSONObject
                                data.width = preview.getInt("width")
                                data.height = preview.getInt("height")
                                data.rat = 1.0 * data.height / data.width
                                data.thumbImgUrl = preview.getString("url").replace("&amp;", "&")

                                data.score = Formatter.shortHandFormatter(Integer.parseInt(post.getString("score")))
                                data.nsfw = post.getBoolean("over_18")
                                data.redditSrc = "https://reddit.com" + post.getString("permalink")
                                //                                data.title = post.getString("title").replaceAll("\\s*\\[.+?\\]\\s*", "").replace("&amp;", "&"); //+"\n("+data.score+"\uD83D\uDD3A)";
                                data.series = post.getString("subreddit_name_prefixed")
                                data.title = post.getString("title").replace("&amp;", "&")

                                datas.add(data)
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        }
        return
    }

    /*
        Tasks that require permission
     */

    fun saveToStorage(finalImg: Bitmap?) {
        val time = System.currentTimeMillis()

        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, time.toString())
        values.put(MediaStore.Images.Media.DISPLAY_NAME, time.toString())
        values.put(MediaStore.Images.Media.DESCRIPTION, "")
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        values.put(MediaStore.Images.Media.DATE_ADDED, time)
        values.put(MediaStore.Images.Media.DATE_TAKEN, time)

        var uri: Uri? = null

        try {
            uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            if (finalImg != null) {
                fixMediaDir()
                val imageOut = contentResolver.openOutputStream(uri!!)

                try {
                    finalImg.compress(Bitmap.CompressFormat.JPEG, 100, imageOut)
                } finally {
                    imageOut!!.close()
                }

                Toast.makeText(applicationContext,
                        "Saved image $time to gallery",
                        Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(applicationContext,
                        "Failed to save image $time to gallery",
                        Toast.LENGTH_LONG).show()
                contentResolver.delete(uri!!, null, null)
                uri = null
            }
        } catch (e: Exception) {
            Toast.makeText(applicationContext,
                    "Failed to save image $time to gallery",
                    Toast.LENGTH_LONG).show()
            if (uri != null) {
                contentResolver.delete(uri, null, null)
            }
        }
    }

    fun shareWallpaperLink(url: String) {
        val share = Intent(Intent.ACTION_SEND)
        share.type = "text/plain"
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)

        share.putExtra(Intent.EXTRA_TEXT, url)

        startActivity(Intent.createChooser(share, "Share link"))
    }

    fun shareWallpaperExtStorage(finalImg: Bitmap?) {
        // Create a new folder AndroidBegin in SD Card
        val dir = File(externalStorageDirectory.absolutePath + "/MoBooru/")
        dir.mkdirs()

        // Create a name for the saved image
        val file = File(dir, "tmp.png")
        val output: OutputStream

        try {
            // Share Intent
            val share = Intent(Intent.ACTION_SEND)

            // Type of file to share
            share.type = "image/jpeg"

            output = FileOutputStream(file)

            // Compress into png format image from 0% - 100%
            finalImg!!.compress(Bitmap.CompressFormat.JPEG, 100, output)
            output.flush()
            output.close()

            // Locate the image to Share
            val uri = Uri.fromFile(file)

            // Pass the image into an intent
            share.putExtra(Intent.EXTRA_STREAM, uri)

            // Show the social share chooser list
            startActivity(Intent.createChooser(share, "Share image"))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(applicationContext,
                    "Failed to share image, please check storage permissions",
                    Toast.LENGTH_LONG).show()
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveToStorage(currentImg)
            } else {
                Toast.makeText(applicationContext,
                        "Permission to save images was not granted",
                        Toast.LENGTH_LONG).show()
            }
            2 -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                shareWallpaperExtStorage(currentImg)
            } else {
                Toast.makeText(applicationContext,
                        "Permission to share/ save images was not granted",
                        Toast.LENGTH_LONG).show()
            }

            else -> {
            }
        }
    }

    inner class ProgressBarAnimation(private val progressBar: ProgressBar, private val from: Float, private val to: Float) : Animation() {

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            val value = from + (to - from) * interpolatedTime
            progressBar.progress = value.toInt()
        }
    }

    /*
        Asynchronous tasks that leverage parallelism
     */

    private inner class FetchSubs : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void): Void? {
            try {
                // Fetch subs list
                doc = Jsoup.connect(mainsite)
                        .header("Accept-Encoding", "gzip, deflate")
                        .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/23.0")
                        .maxBodySize(0)
                        .timeout(6000000)
                        .get()
                redditSubs = doc.select("script")
                for ((i, sub) in redditSubs.withIndex()) {
                    val at = sub.toString()
                    if (i == 2) {
                        subsJSON = at
                    }
                }

                subsJSON = subsJSON.substring(subsJSON.indexOf("["))
                subsJSON = subsJSON.substring(0, subsJSON.indexOf("]") + 1)
                arr = JSONArray(subsJSON)

                if (timeToUpdate || arr.length() > subsMap.size) {
                    for (j in 0 until arr.length()) {
                        subsMap[arr.getJSONObject(j).getInt("value")] = Sub(arr.getJSONObject(j).getString("name"), arr.getJSONObject(j).getInt("value"))
                    }
                    executeAsyncTask(UpdateIndex())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return null
        }
    }

    private inner class UpdateIndex : AsyncTask<Void, Int, Void>() {
        internal var prog = 0

        override fun doInBackground(vararg params: Void): Void? {
            for (s in subsMap.values) {
                var info: String
                val obj: JSONObject
                try {
                    val aboutURL = "https://www.reddit.com/" + s.subName + "/about.json"
                    val url = URL(aboutURL)
                    val scan = Scanner(url.openStream())
                    info = ""
                    while (scan.hasNext())
                        info += scan.nextLine()
                    scan.close()
                    obj = JSONObject(info).getJSONObject("data")
                    subsMap[s.subID] = Sub(
                            s.subName,
                            s.subID,
                            obj.getInt("subscribers"),
                            s.selected,
                            obj.getBoolean("over18"),
                            s.isCustom,
                            obj.getString("public_description")
                    )
                } catch (ex: Exception) {
                    //                ex.printStackTrace();
                    subsMap[s.subID] = Sub(
                            s.subName,
                            s.subID,
                            0,
                            s.selected,
                            isNSFW = false,
                            isCustom = false,
                            desc = ""
                    )
                }
                publishProgress(ceil(prog * 1.0 / subsMap.values.size * 100.0 * progressBarScale.toDouble()).toInt())
                prog++
            }

            val serial = gson.toJson(subsMap, intSubMap)
            prefsEditor.putString("SUBS", serial)
            prefsEditor.apply()
            return null
        }

        override fun onPostExecute(result: Void?) {
            prefsEditor.putLong("UPDATE_TIME", System.currentTimeMillis())
            prefsEditor.apply()
            timeToUpdate = false
            this@Main.runOnUiThread {
                detProgressBar.visibility = View.GONE
                Toast.makeText(applicationContext,
                        "Indexing completed",
                        Toast.LENGTH_LONG).show()
            }
        }

        override fun onPreExecute() {
            this@Main.runOnUiThread { detProgressBar.visibility = View.VISIBLE }
        }

        override fun onProgressUpdate(vararg values: Int?) {
            val anim = ProgressBarAnimation(detProgressBar, detProgressBar.progress.toFloat(), values[0]!!.toFloat())
            anim.duration = 1000
            this@Main.runOnUiThread { detProgressBar.startAnimation(anim) }
        }
    }

    private inner class DownloadImage internal constructor(internal var bmImage: InteractiveImageView) : AsyncTask<String, Void, Bitmap>() {

        override fun onPreExecute() {}

        override fun doInBackground(vararg urls: String): Bitmap? {
            val urldisplay = urls[0]
            var img: Bitmap? = null
            try {
                val `in` = URL(urldisplay).openStream()
                img = BitmapFactory.decodeStream(`in`)
                val bitmapSize = img!!.byteCount
                if (bitmapSize > MAX_BITMAP_SIZE) {
                    img = null
                    throw RuntimeException(
                            "Canvas: trying to draw too large(" + bitmapSize + "bytes) bitmap.")
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

            return img
        }

        override fun onPostExecute(result: Bitmap) {
            bmImage.setImageDrawable(BitmapDrawable(resources, result))
        }
    }

    inner class LoadMorePhotos : AsyncTask<Void, Void, Void>() {
        private lateinit var tmp: JSONArray
        private var customTmp = arrayOfNulls<JSONArray>(selectedCustomSubs.size)

        override fun doInBackground(vararg arg0: Void): Void? {
            // set loadingMore flag
            loadingMore = true

            // Increment current page
            currentPage += 1

            // Normal subs: via Redditbooru
            if (selectedSubs.size == 0 && selectedCustomSubs.size == 0 || selectedSubs.size != 0) {
                try {
                    // refactor into string scanner
                    selectedURL = "https://redditbooru.com/images/?sources=$selectedString&afterDate="
                    url = URL(selectedURL + lastIndexTime)

                    val scan = Scanner(url.openStream())
                    var str = ""
                    while (scan.hasNext())
                        str += scan.nextLine()
                    scan.close()

                    tmp = JSONArray(str)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

            // Custom subs: from Reddit directly
            if (selectedCustomSubs.size != 0) {
                var ix = 0
                for (i in selectedCustomSubs) {
                    try {
                        var jsonURL = "https://www.reddit.com/" + customSubsMap[i]!!.subName + "/.json"
                        if (nextPages.containsKey(i)) {
                            jsonURL += "?after=" + nextPages[i]!!
                        }
                        url = URL(jsonURL)
                        val scan = Scanner(url.openStream())
                        var posts = ""
                        while (scan.hasNext())
                            posts += scan.nextLine()
                        scan.close()
                        val obj = JSONObject(posts).getJSONObject("data")
                        customTmp[ix] = obj.getJSONArray("children")
                        nextPages[i] = obj.getString("after")
                        ix++
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            }
            return null
        }

        override fun onPreExecute() {
            inDetProgressBar.visibility = View.VISIBLE
        }

        override fun onPostExecute(result: Void?) {
            // APPEND NEW DATA TO THE ARRAYLIST AND SET THE ADAPTER TO THE
            // LISTVIEW

            // 4 cases:

            // 1. has no default or custom subs selected
            //    show all from default
            // 2. has >=1 default and no custom subs selected
            //    show default
            // 3. has no default and >=1 custom subs selected
            //    show custom
            // 4. has >=1 default and >=1 custom subs selected
            //    show both

            // Initialize with normal data
            try {
                if (selectedSubs.size == 0 && selectedCustomSubs.size == 0 || selectedSubs.size != 0)
                    addToArry(tmp)

                // Append custom subs data
                if (selectedCustomSubs.size != 0)
                    addToArryDirect(customTmp)

                adapter!!.datas = datas
                adapter!!.notifyDataSetChanged()

                // SET LOADINGMORE "FALSE" AFTER ADDING NEW FEEDS TO THE EXISTING
                loadingMore = false
                inDetProgressBar.visibility = View.GONE
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    companion object {

        const val MAX_BITMAP_SIZE = 100 * 1024 * 1024 // 100 MB
        const val DEFAULT_COLUMNS_PORTRAIT = 3
        const val DEFAULT_COLUMNS_LANDSCAPE = 5
        const val MAX_COLUMNS_PORTRAIT = 6
        const val MAX_COLUMNS_LANDSCAPE = 8
        const val MIN_COLUMNS_PORTRAIT = 1
        const val MIN_COLUMNS_LANDSCAPE = 2

        @TargetApi(Build.VERSION_CODES.HONEYCOMB) // API 11
        fun <T> executeAsyncTask(asyncTask: AsyncTask<T, *, *>, vararg params: T) {
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, *params)
        }
    }
}
