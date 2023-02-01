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
import android.graphics.Color
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
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URL
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil
import kotlin.system.exitProcess

class Main : AppCompatActivity(), CoroutineScope {

    // Core config
    private var appName = "MoBooru"
    private lateinit var verString: String
    internal val progressBarScale = 100
    private var thumbnailSize = 300
    private var pageSize = 30
    internal var currentPage = 1
    internal lateinit var url: URL
    internal lateinit var display: Display
    internal var screenWidth = 0
    internal var screenHeight = 0
    internal var bitmapWidth = 0
    internal var bitmapHeight = 0
    internal var displayMetrics = DisplayMetrics()

    // Site config
    private var showNsfw: Boolean = false
    private var showTitles: Boolean = false
    private var fullscreen: Boolean = false
    private var darkmode: Boolean = false
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
    private lateinit var noDataView: LinearLayout
    internal lateinit var drawerLayout: DrawerLayout
    private lateinit var swipeContainer: SwipeRefreshLayout

    // Sharedprefs, serialization, storage
    internal lateinit var subsMap: HashMap<String, Sub>
    internal lateinit var customSubsMap: HashMap<String, Sub>
    internal var selectedSubs = HashSet<String>()
    internal var selectedCustomSubs = HashSet<String>()
    internal var nextPages = HashMap<String, String>()
    internal var lastIndexTime: Long = 0
    internal var timeToUpdate = false
    internal var gson = Gson()
    internal var intSubMap = object : TypeToken<HashMap<String, Sub>>() {}.type
    private var intSet = object : TypeToken<HashSet<String>>() {}.type
    internal var dataList = object : TypeToken<ArrayList<Data>>() {}.type
    internal lateinit var prefs: SharedPreferences
    internal lateinit var prefsEditor: SharedPreferences.Editor
    private var externalStorageDirectory = Environment.getExternalStorageDirectory()
    internal var currentImg: Bitmap? = null
    private var adapter: DataAdapter? = null

    // Coroutines
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val isNetworkAvailable: Boolean
        get() {
            val connectivityManager = getSystemService(Activity.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        job = Job()
        // Sharedprefs
        try {
            prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
            prefsEditor = prefs.edit()
            // Get saved subs
            subsMap = gson.fromJson(prefs.getString("SUBS", "{'awwnime': {subName: 'awwnime', subscriberCount: 0, selected: false, isNSFW: false, desc: '', isCustom: false}}"), intSubMap)
            customSubsMap = gson.fromJson(prefs.getString("CUSTOM_SUBS", "{}"), intSubMap)
            selectedSubs = gson.fromJson(prefs.getString("SELECTED_SUBS", "['awwnime']"), intSet)
            selectedCustomSubs = gson.fromJson(prefs.getString("SELECTED_CUSTOM_SUBS", "[]"), intSet)
            favorites = gson.fromJson(prefs.getString("FAVORITES", "[]"), dataList)
            showNsfw = prefs.getBoolean("SHOW_NSFW", false)
            fullscreen = prefs.getBoolean("FULLSCREEN", false)
            darkmode = prefs.getBoolean("DARK_MODE", false)
            showTitles = prefs.getBoolean("SHOW_TITLES", true)
            thumbnailSize = prefs.getInt("THUMBNAIL_SIZE", 300)

            // Fullscreen to false by default
            if (!prefs.contains("FULLSCREEN")) {
                prefsEditor.putBoolean("FULLSCREEN", false).apply()
            }

            // Show titles to true by default
            if (!prefs.contains("SHOW_TITLES")) {
                prefsEditor.putBoolean("SHOW_TITLES", true).apply()
            }

            // Dark mode
            if (!prefs.contains("DARK_MODE")) {
                prefsEditor.putBoolean("DARK_MODE", false).apply()
            }

            // Thumbnail size is 300 by default
            if (!prefs.contains("THUMBNAIL_SIZE")) {
                prefsEditor.putInt("THUMBNAIL_SIZE", 300).apply()
            }

            // Time to reindex subs
            if (!prefs.contains("UPDATE_TIME") || System.currentTimeMillis() - prefs.getLong("UPDATE_TIME", 0) > 604800000) {
                timeToUpdate = true
            }

            // Dark mode
            setTheme(if (prefs.getBoolean("DARK_MODE", false)) R.style.AppThemeDark else R.style.AppThemeLight)

        } catch (ex: Exception) {
            ex.printStackTrace()
//            timeToUpdate = true
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        windowManager.defaultDisplay.getMetrics(displayMetrics)

        navigationView = findViewById<View>(R.id.navigationView) as NavigationView
        drawerLayout = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        detProgressBar = findViewById<View>(R.id.determinate_progress_bar) as ProgressBar
        detProgressBar.max = 100 * progressBarScale
        detProgressBar.visibility = View.GONE

        inDetProgressBar = findViewById<View>(R.id.indeterminate_progress_bar) as ProgressBar
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

        viewingFavorites = if (intent.getSerializableExtra("viewingFavorites") == null) false else intent.getSerializableExtra("viewingFavorites") as Boolean
//        setFavoriteSubs()

        navigationView.setBackgroundColor(if (darkmode) Color.parseColor("#434343") else Color.parseColor("#FFFFFF"))
        navigationView.setNavigationItemSelectedListener(NavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_subs -> {
                    if (!timeToUpdate) {
                        startActivity(Intent(this@Main, SubSelector::class.java).putExtra("subsMap", subsMap).putExtra("customSubsMap", customSubsMap))
                        finish()
                    } else {
                        Toast.makeText(applicationContext,
                                R.string.indexing_wait,
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
                                R.string.indexing_wait,
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

                    d2.setTitle(R.string.number_of_columns)
                    if (display.rotation == Surface.ROTATION_0) {
                        numberPicker.maxValue = MAX_COLUMNS_PORTRAIT
                        numberPicker.minValue = MIN_COLUMNS_PORTRAIT
                        d2.setMessage(getString(R.string.default_portrait, DEFAULT_COLUMNS_PORTRAIT))
                        numberPicker.value = prefs.getInt("COLUMNS_PORTRAIT", DEFAULT_COLUMNS_PORTRAIT)
                    } else {
                        numberPicker.maxValue = MAX_COLUMNS_LANDSCAPE
                        numberPicker.minValue = MIN_COLUMNS_LANDSCAPE
                        d2.setMessage(getString(R.string.default_landscape, DEFAULT_COLUMNS_LANDSCAPE))
                        numberPicker.value = prefs.getInt("COLUMNS_LANDSCAPE", DEFAULT_COLUMNS_LANDSCAPE)
                    }
                    d2.setView(dialogView)

                    numberPicker.wrapSelectorWheel = false
                    d2.setPositiveButton(R.string.done, object : DialogInterface.OnClickListener {
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
                            restartMain(viewingFavorites)
                            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
                        }
                    })
                    d2.setNegativeButton(R.string.cancel) { _, _ -> }
                    d2.setOnDismissListener { updateScreenMode() }
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
                            .setTitle(R.string.about)
                            .setNegativeButton(R.string.back) { _, _ ->
                                // do nothing
                            }
                            .setIcon(R.drawable.ic_launcher)
                            .setMessage(Html.fromHtml(appName + " v" + verString + "<br>Developer: Arthur Leung<br/><a href=\"http://arcyleung.com\">http://arcyleung.com</a><br/><br/>Follow me on <a href=\"https://twitter.com/arcyleung\">Twitter</a> or <a href=\"https://www.linkedin.com/in/arcyleung/\">LinkedIn</a>! <br/> <br/> Please direct any questions and suggestions to <a href=\"mailto:arcyleung@gmail.com?Subject=MoBooru Inquiry\" target=\"_top\">arcyleung@gmail.com</a>\n" +
                                    "</p> or if you want to help support development!"))
                            .setOnDismissListener { updateScreenMode() }
                            .create()

                    d1.show()

                    (d1.findViewById<View>(android.R.id.message) as TextView).movementMethod = LinkMovementMethod.getInstance()
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        })

        display = windowManager.defaultDisplay

        if (!isNetworkAvailable) {
            val d = AlertDialog.Builder(this@Main)
                    .setTitle(R.string.no_internet)
                    .setNegativeButton(R.string.quit) { _, _ ->
                        moveTaskToBack(true)
                        Process.killProcess(Process.myPid())
                        exitProcess(1)
                    }
                    .setIcon(R.drawable.ic_launcher)
                    .setMessage(R.string.no_internet_help)
                    .create()

            d.show()
            return
        }
        swipeContainer = findViewById<View>(R.id.swipe_container) as SwipeRefreshLayout
        if (!viewingFavorites) {
            swipeContainer.isEnabled = true
            swipeContainer.setOnRefreshListener {
                recreate()
                // Implement refresh adapter code
//                finish()
//                overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
//                restartMain(viewingFavorites)
//                overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
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
                    R.string.updating_index,
                    Toast.LENGTH_LONG).show()
        }

        this@Main.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER

        calcScreenSize()
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
    }

    override fun onResume() {
        super.onResume()
        updateScreenMode()
    }

    fun initializeAdapter() {
        try {
            adapter = DataAdapter(this, R.layout.staggered, datas, showNsfw, showTitles)
            title = Html.fromHtml("<font color='#ffffff'>" + (if (viewingFavorites) getString(R.string.favorites) else appName) + "</font>")
            staggeredGridView = findViewById<View>(R.id.gridView) as StaggeredGridView
            noDataView = findViewById(R.id.noData)
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
                                .setTitle(R.string.favorites)
                                .setNegativeButton(R.string.back) { _, _ ->
                                    // do nothing
                                }
                                .setMessage(R.string.no_favorites)
                                .setOnDismissListener { restartMain(false) }
                                .create()

                        d1.show()
                    } else {
                        Toast.makeText(applicationContext,
                                R.string.favorites,
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

//    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
//        super.onSaveInstanceState(outState, outPersistentState)
//    }

    override fun onDestroy() {
        if (dialog != null)
            dialog!!.dismiss()
        if (progressDialog != null)
            progressDialog!!.dismiss()
        job.cancel()
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

    fun updateScreenMode() {
        if (fullscreen) {
            immersiveFullscreen()
        } else {
            nonFullscreen()
        }
    }

    fun nonFullscreen() {
        window.decorView.systemUiVisibility = 0
        toolbar.visibility = View.VISIBLE
    }

    fun immersiveFullscreen() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        toolbar.visibility = View.GONE
    }

    private fun setOnClickListener() {
        staggeredGridView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            dialog = Dialog(this@Main)
            dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
            val selected = datas[position]
            val zoomImageView = InteractiveImageView(this@Main)
            isExpanded = false
            progressDialog = ProgressDialog.show(this@Main, getString(R.string.downloading), "…", true)

            object : Thread() {
                @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
                override fun run() {
                    var img: Bitmap? = null
                    try {
                        val tmp = DownloadImage(zoomImageView).execute(selected.imgUrl).get()
                        if (tmp == null) {
//                            this@Main.runOnUiThread {
//                                Toast.makeText(applicationContext,
//                                        "Error loading image: file corrupted or too large",
//                                        Toast.LENGTH_LONG).show()
//                            }
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
                                    val portTranslate = displayMetrics.widthPixels * 0.18f
                                    unfavoriteFAB.animate().translationX(-portTranslate)
                                } else {
                                    // Horizontal
                                    val landTranslate = displayMetrics.heightPixels * 0.17f
                                    unfavoriteFAB.animate().translationX(-landTranslate)
                                }
                            } else {
                                favoriteFAB.show()
                                unfavoriteFAB.hide()
                                if (display.rotation == Surface.ROTATION_0) {
                                    // Vertical
                                    val portTranslate = displayMetrics.widthPixels * 0.18f
                                    favoriteFAB.animate().translationX(-portTranslate)
                                } else {
                                    // Horizontal
                                    val landTranslate = displayMetrics.heightPixels * 0.17f
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
                                        .setTitle(R.string.confirm)
                                        .setMessage(R.string.set_wallpaper_prompt)
                                        .setIcon(getDrawable(R.drawable.ic_wallpaper_white_48dp))
                                        .setPositiveButton(android.R.string.yes) { _, _ ->
                                            progressDialog = ProgressDialog.show(this@Main, getString(R.string.setting_wallpaper), "…", true)

                                            object : Thread() {
                                                override fun run() {
                                                    val wallMan = WallpaperManager.getInstance(applicationContext)
                                                    Looper.prepare()
                                                    try {
                                                        wallMan.setBitmap(finalImg)
                                                        progressDialog!!.dismiss()

                                                        this@Main.runOnUiThread {
                                                            Toast.makeText(applicationContext,
                                                                    R.string.setting_wallpaper_success,
                                                                    Toast.LENGTH_LONG).show()
                                                        }

                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        progressDialog!!.dismiss()
                                                        this@Main.runOnUiThread {
                                                            Toast.makeText(applicationContext,
                                                                    R.string.setting_wallpaper_fail,
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
                                val options = resources.getStringArray(R.array.share_options)
                                val d = AlertDialog.Builder(this@Main)
                                        .setTitle(R.string.share)
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
                                                R.string.already_favorited,
                                                Toast.LENGTH_LONG).show()
                                    } else {
                                        favorites.add(selected)
                                        val serial = gson.toJson(favorites, dataList)
                                        prefsEditor.putString("FAVORITES", serial)
                                        prefsEditor.apply()
                                        Toast.makeText(applicationContext,
                                                R.string.favorited,
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
                                            R.string.unfavorited,
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
                        ex.printStackTrace()
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
            val portTranslate = displayMetrics.widthPixels * 0.18f
            wallpaperFAB.animate().translationY(-portTranslate)
            shareFAB.animate().translationY(2 * -portTranslate)
            saveFAB.animate().translationY(3 * -portTranslate)
            sourceFAB.animate().translationY(4 * -portTranslate)
        } else {
            // Horizontal
            val landTranslate = displayMetrics.heightPixels * 0.17f
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
//    private fun setFavoriteSubs() {
//        selectedString = ""
//        for (i in selectedSubs) {
//            selectedString += "$i%2C"
//        }
//    }

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

        val nsfwToggle = (navigationView.findViewById<View>(R.id.nsfw_toggle) as? RelativeLayout)?.getChildAt(0) as? SwitchCompat
        val fullScreenToggle = (navigationView.findViewById<View>(R.id.fullscreen_toggle) as? RelativeLayout)?.getChildAt(0) as? SwitchCompat
        val titlesToggle = (navigationView.findViewById<View>(R.id.title_toggle) as? RelativeLayout)?.getChildAt(0) as? SwitchCompat
        val darkModeToggle = (navigationView.findViewById<View>(R.id.darkmode_toggle) as? RelativeLayout)?.getChildAt(0) as? SwitchCompat
        nsfwToggle?.isChecked = prefs.getBoolean("SHOW_NSFW", false)
        fullScreenToggle?.isChecked = prefs.getBoolean("FULLSCREEN", false)
        titlesToggle?.isChecked = prefs.getBoolean("SHOW_TITLES", true)
        darkModeToggle?.isChecked = prefs.getBoolean("DARK_MODE", false)

        nsfwToggle?.setOnClickListener {
            val showing = prefs.getBoolean("SHOW_NSFW", false)
            if (!showing) {
                // Ask for user confirmation if not previously set
                val confirm = AlertDialog.Builder(this@Main)
                        .setTitle(R.string.show_nsfw_prompt)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            prefsEditor.putBoolean("SHOW_NSFW", true)
                            prefsEditor.apply()

                            finish()
                            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
                            restartMain(viewingFavorites)
                            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
                        }
                        .setNegativeButton("No") { _, _ ->
                            nsfwToggle.isChecked = false
                        }
                        .setIcon(R.drawable.ic_launcher)
                        .setMessage(R.string.show_nsfw_confirmation)
                        .setOnDismissListener {
                            updateScreenMode()
                            nsfwToggle.isChecked = prefs.getBoolean("SHOW_NSFW", false)
                        }
                        .create()
                confirm.show()
            } else {
                prefsEditor.putBoolean("SHOW_NSFW", false)
                prefsEditor.apply()

                finish()
                overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
                restartMain(viewingFavorites)
                overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
            }
        }

        fullScreenToggle?.setOnClickListener {
            prefsEditor.putBoolean("FULLSCREEN", !prefs.getBoolean("FULLSCREEN", false))
            prefsEditor.apply()
            recreate()

//            finish()
//            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
//            restartMain(viewingFavorites)
//            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
        }

        titlesToggle?.setOnClickListener {
            prefsEditor.putBoolean("SHOW_TITLES", !prefs.getBoolean("SHOW_TITLES", true))
            prefsEditor.apply()
            recreate()

//            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
//            restartMain(viewingFavorites)
//            overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
        }

        darkModeToggle?.setOnClickListener {
            val enabled = prefs.getBoolean("DARK_MODE", false)
            prefsEditor.putBoolean("DARK_MODE", !enabled)
            prefsEditor.apply()
            recreate()

//            AppCompatDelegate.setDefaultNightMode(when (!enabled) {
//                true -> MODE_NIGHT_YES
//                false -> MODE_NIGHT_NO
//            })
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
                        // Metadata
                        data.nsfw = ja.getJSONObject(i).getBoolean("nsfw")
                        if (!showNsfw && data.nsfw)
                            continue
                        data.score = Formatter.shortHandFormatter(Integer.parseInt(ja.getJSONObject(i).getString("score")))
                        data.redditSrc = "https://redd.it/" + ja.getJSONObject(i).getString("externalId")
                        data.title = ja.getJSONObject(i).getString("title").replace("\\s*\\[.+?]\\s*".toRegex(), "").replace("&amp;", "&") //+"\n("+data.score+"\uD83D\uDD3A)";
                        data.series = ja.getJSONObject(i).getString("title").replace("^[^\\[]*".toRegex(), "").replace("&amp;", "&")

                        // Image data
                        data.imgUrl = ja.getJSONObject(i).getString("cdnUrl")
                        data.width = ja.getJSONObject(i).getInt("width")
                        data.height = ja.getJSONObject(i).getInt("height")
                        data.rat = 1.0 * data.height / data.width
                        data.thumbImgUrl = ja.getJSONObject(i).getString("thumb") + "_" + thumbnailSize + "_" + thumbnailSize + ".jpg"


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
                        getString(R.string.save_gallery_success, time.toString()),
                        Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(applicationContext,
                        getString(R.string.save_gallery_fail, time.toString()),
                        Toast.LENGTH_LONG).show()
                contentResolver.delete(uri!!, null, null)
                uri = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(applicationContext,
                    getString(R.string.save_gallery_fail, time.toString()),
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

        startActivity(Intent.createChooser(share, getString(R.string.share_link)))
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
            startActivity(Intent.createChooser(share, getString(R.string.share_image)))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(applicationContext,
                    R.string.share_failed_permissions,
                    Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveToStorage(currentImg)
            } else {
                Toast.makeText(applicationContext,
                        R.string.no_permission_share,
                        Toast.LENGTH_LONG).show()
            }
            2 -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                shareWallpaperExtStorage(currentImg)
            } else {
                Toast.makeText(applicationContext,
                        R.string.no_permission_share_save,
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
                val presetSubs = resources.getStringArray(R.array.preset_subs)
                    for (subName in presetSubs) {
                        subsMap[subName] = Sub(subName)
                    }
                    executeAsyncTask(UpdateIndex())

                    // If first time launching app, show help snackbar
                    if (prefs.getBoolean("FIRST_LAUNCH", true)) {
                        val snackbar = Snackbar
                                .make(drawerLayout, getString(R.string.help_prompt), 20000)
                                .setAction(R.string.yes) {
                                    overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
                                    startActivity(Intent(applicationContext, Help::class.java))
                                    overridePendingTransition(R.transition.fade_in, R.transition.fade_out)
                                }

                        snackbar.show()
                        prefsEditor.putBoolean("FIRST_LAUNCH", false).commit()
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                timeToUpdate = true
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
                    val aboutURL = "https://www.reddit.com/r/" + s.subName + "/about.json"
                    val url = URL(aboutURL)
                    val scan = Scanner(url.openStream())
                    info = ""
                    while (scan.hasNext())
                        info += scan.nextLine()
                    scan.close()
                    obj = JSONObject(info).getJSONObject("data")
                    subsMap[s.subName] = Sub(
                            s.subName,
                            obj.getInt("subscribers"),
                            s.selected,
                            obj.getBoolean("over18"),
                            s.isCustom,
                            obj.getString("public_description")
                    )
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    subsMap[s.subName] = Sub(
                            s.subName,
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
                        R.string.indexing_complete,
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

    private inner class DownloadImage internal constructor(internal var bmImage: InteractiveImageView) : AsyncTask<String, Void, Bitmap?>() {

        override fun onPreExecute() {}

        override fun doInBackground(vararg urls: String): Bitmap? {
            val urldisplay = urls[0]
            var img: Bitmap? = null
            try {
                val input = URL(urldisplay).openStream()
                img = BitmapFactory.decodeStream(input)
                if (img == null) {
                    this@Main.runOnUiThread {
                        Toast.makeText(applicationContext,
                                R.string.error_loading_image,
                                Toast.LENGTH_LONG).show()
                    }
                    return null
                }
                val bitmapSize = img!!.byteCount
                if (bitmapSize > MAX_BITMAP_SIZE) {
                    this@Main.runOnUiThread {
                        Toast.makeText(applicationContext,
                                R.string.error_loading_image_size,
                                Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return img
        }

        override fun onPostExecute(result: Bitmap?) {
            bmImage.setImageDrawable(BitmapDrawable(resources, result))
        }
    }

    inner class LoadMorePhotos : AsyncTask<Void, Void, Void>() {
        private var tmp = JSONArray()
        private var fetchedCustomPages = AtomicInteger(0)

        override fun doInBackground(vararg arg0: Void): Void? {
            // set loadingMore flag
            loadingMore = true

            // Increment current page
            currentPage += 1

            if (selectedSubs.size == 0 && selectedCustomSubs.size == 0) {
                noDataView!!.visibility = View.VISIBLE
                staggeredGridView!!.visibility = View.GONE

                tmp = JSONArray()
                loadingMore = false
                return null
            }

            // Merge the default and custom subs


            // Custom subs: from Reddit directly
            selectedSubs.addAll(selectedCustomSubs)
                var ix = 0
                for (i in selectedSubs) {
                    launch(Dispatchers.IO) {
                        var dataJSON = JSONArray()
                        try {
                            var jsonURL = "https://www.reddit.com/r/" + i + "/.json"
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
                            dataJSON = obj.getJSONArray("children")
                            nextPages[i] = obj.getString("after")
                            ix++
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        if (dataJSON != null) {
                            var posts = ArrayList<Data>()
                            for (j in 0 until dataJSON!!.length()) {
                                try {
                                    val post = (dataJSON!!.get(j) as JSONObject).get("data") as JSONObject
                                    val url = post.getString("url")
                                    val extension = url.split(".")
                                    // find better way of matching extensions
                                    if (extension.size > 1 &&
                                        (extension.last() == "png" || extension.last() == "jpg") &&
                                        (!post.isNull("preview") || !post.isNull("thumbnail"))
                                    )
                                    {
                                        val data = Data()

                                        // Metadata
                                        data.nsfw = post.getBoolean("over_18")
                                        if (!showNsfw && data.nsfw)
                                            continue
                                        data.score = Formatter.shortHandFormatter(Integer.parseInt(post.getString("score")))
                                        data.redditSrc = "https://reddit.com" + post.getString("permalink")
                                        data.series = post.getString("subreddit_name_prefixed")
                                        data.title = post.getString("title").replace("&amp;", "&")

                                        // Image data
                                        data.imgUrl = url
                                        val previews = (post.getJSONObject("preview").getJSONArray("images") as JSONArray).get(0) as JSONObject
                                        val resolutions = previews.getJSONArray("resolutions")
                                        val preview = resolutions.get(resolutions.length() / 2) as JSONObject
                                        data.width = preview.getInt("width")
                                        data.height = preview.getInt("height")
                                        data.rat = 1.0 * data.height / data.width
                                        data.thumbImgUrl = preview.getString("url").replace("&amp;", "&")

                                        posts.add(data)
                                    }
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                }
                            }

                            withContext(Dispatchers.Main) {
                                datas.addAll(posts)
                                adapter!!.datas = datas
                                adapter!!.notifyDataSetChanged()
                            }
                        }
                        fetchedCustomPages.incrementAndGet()
                    }
                }
            return null
        }

        override fun onPreExecute() {
            inDetProgressBar.visibility = View.VISIBLE
        }

        override fun onPostExecute(result: Void?) {
            // Initialize with normal data
            try {
                if (selectedSubs.size == 0 && selectedCustomSubs.size == 0 || selectedSubs.size != 0)
                    addToArry(tmp)

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
