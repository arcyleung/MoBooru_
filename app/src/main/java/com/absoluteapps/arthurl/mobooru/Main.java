package com.absoluteapps.arthurl.mobooru;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.etsy.android.grid.StaggeredGridView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;


public class Main extends AppCompatActivity {

    public static final int MAX_BITMAP_SIZE = 100 * 1024 * 1024; // 100 MB
    public static final int DEFAULT_COLUMNS_PORTRAIT = 2;
    public static final int DEFAULT_COLUMNS_LANDSCAPE = 3;
    public static final int MAX_COLUMNS_PORTRAIT = 6;
    public static final int MAX_COLUMNS_LANDSCAPE = 6;
    public static final int MIN_COLUMNS_PORTRAIT = 1;
    public static final int MIN_COLUMNS_LANDSCAPE = 2;

    // Core config
    final String appName = "MoBooru";
    final String verString = "1.3";
    final int progressBarScale = 100;
    int thumbnail_size = 300;
    int pageSize = 30;
    int current_page = 1;
    String mainsite = "https://redditbooru.com";
    URL url1;
    Display display;
    int screenWidth = 0;
    int screenHeight = 0;
    int bitmapWidth = 0;
    int bitmapHeight = 0;
    DisplayMetrics displayMetrics = new DisplayMetrics();

    // Site config
    String selectedString = "";
    boolean showNsfw;
    boolean showTitles;
    String selectedURL = "https://redditbooru.com/images/?sources=" + selectedString + "&afterDate=";
    Document doc;
    Elements redditSubs;
    String subsJSON = "";
    JSONArray arr;
    LoadJSONasyncInit runner;
    JSONArray jsonObjs;
    ArrayList<Data> datas = new ArrayList<>();
    ArrayList<Data> favorites = new ArrayList<>();
    ArrayList<Data> tmp;
    LoadMorePhotos lm;
    boolean loadingMore = true;
    boolean viewingFavorites;

    // UI, Views, Layout
    ProgressBar inDetProgressBar;
    ProgressBar detProgressBar;
    ProgressDialog progressDialog;
    Toolbar toolbar;
    FloatingActionButton close;
    FloatingActionButton actions;
    FloatingActionButton fab, fab1, fab2, fab3, fab4, fab5;
    boolean isFABOpen;
    NavigationView navigationView;
    StaggeredGridView staggeredGridView;
    DrawerLayout drawerLayout;
    AppBarLayout appBarLayout;
    SwipeRefreshLayout swipeContainer;
    // Sharedprefs, serialization, storage
    HashMap<Integer, Sub> subsList = new HashMap<>();
    HashSet<Integer> selectedSubs = new HashSet<>();
    long lastIndexTime;
    boolean timeToUpdate = false;
    Gson gson = new Gson();
    Type intSubMap = new TypeToken<Map<Integer, Sub>>() {
    }.getType();
    Type intSet = new TypeToken<Set<Integer>>() {
    }.getType();
    Type dataList = new TypeToken<ArrayList<Data>>() {
    }.getType();
    SharedPreferences prefs;
    SharedPreferences.Editor prefsEditor;
    File externalStorageDirectory = Environment.getExternalStorageDirectory();
    private DataAdapter adapter;

    private static Point getDisplaySize(final Display display) {
        final Point point = new Point();
        try {
            display.getSize(point);
        } catch (java.lang.NoSuchMethodError ignore) { // Legacy support
            point.x = display.getWidth();
            point.y = display.getHeight();
        }
        return point;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB) // API 11
    public static <T> void executeAsyncTask(AsyncTask<T, ?, ?> asyncTask, T... params) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        else
            asyncTask.execute(params);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_main);
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        navigationView = (NavigationView) findViewById(R.id.navigationView);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        detProgressBar = (ProgressBar) findViewById(R.id.determinate_progress_bar);
        detProgressBar.setMax(100 * progressBarScale);
        detProgressBar.setVisibility(View.GONE);

        inDetProgressBar = (ProgressBar) findViewById(R.id.indeterminate_progress_bar);
        inDetProgressBar.setVisibility(View.GONE);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        appBarLayout = (AppBarLayout) findViewById(R.id.appBar);
        ActionBarDrawerToggle mDrawerToggle;
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
            mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
            {

                public void onDrawerClosed(View view)
                {
                    supportInvalidateOptionsMenu();
                    //drawerOpened = false;
                }

                public void onDrawerOpened(View drawerView)
                {
                    supportInvalidateOptionsMenu();
                    //drawerOpened = true;
                }
            };
            mDrawerToggle.setDrawerIndicatorEnabled(true);
            drawerLayout.setDrawerListener(mDrawerToggle);
            mDrawerToggle.syncState();
        }
//        toolbar.setNavigationIcon(android.R.drawable.ic_menu;
//        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                drawerLayout.openDrawer(GravityCompat.START);
//            }
//        });


        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Sharedprefs
        try {
            prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            prefsEditor = prefs.edit();

            // Get saved subs
            subsList = gson.fromJson(prefs.getString("SUBS", "{1: {subName: 'Awwnime', subID: 1, subscriberCount: 0, selected: true, isNSFW: false, desc: ''}}"), intSubMap);
            selectedSubs = gson.fromJson(prefs.getString("SELECTED_SUBS", "[1]"), intSet);
            favorites = gson.fromJson(prefs.getString("FAVORITES", "[]"),dataList);
            showNsfw = prefs.getBoolean("SHOW_NSFW", false);
            toolbar.setVisibility(!prefs.getBoolean("FULLSCREEN", false) == true ? View.VISIBLE : View.GONE);
            showTitles = prefs.getBoolean("SHOW_TITLES", true);
            thumbnail_size = prefs.getInt("THUMBNAIL_SIZE", 300);

            // Fullscreen to false by default
            if (!prefs.contains("FULLSCREEN")) {
                prefsEditor.putBoolean("FULLSCREEN", false);
                prefsEditor.apply();
            }
            if (prefs.getBoolean("FULLSCREEN", false)) {
                immersiveFullscreen();
            }

            // Show titles to true by default
            if (!prefs.contains("SHOW_TITLES")) {
                prefsEditor.putBoolean("SHOW_TITLES", true);
                prefsEditor.apply();
            }

            // Thumbnail size is 300 by default
            if (!prefs.contains("THUMBNAIL_SIZE")) {
                prefsEditor.putInt("THUMBNAIL_SIZE", 300);
                prefsEditor.apply();
            }

            // Time to reindex subs
            if (!prefs.contains("UPDATE_TIME") || System.currentTimeMillis() - prefs.getLong("UPDATE_TIME", 0) > 604800000) {
                timeToUpdate = true;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            timeToUpdate = true;
        }

        viewingFavorites = getIntent().getSerializableExtra("viewingFavorites") == null ? false : (boolean) getIntent().getSerializableExtra("viewingFavorites");
        setFavoriteSubs();

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Intent intent;
                switch (item.getItemId()) {
                    case R.id.nav_subs:
                        if (!timeToUpdate) {
                            startActivity(new Intent(Main.this, SubSelector.class).putExtra("subsList", subsList));
                            finish();
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    "Please wait until indexing finishes...",
                                    Toast.LENGTH_LONG).show();
                        }
                        return true;
//                        drawerLayout.closeDrawers();

                    case R.id.nav_favorites:
                        intent = getIntent();
                        intent.putExtra("viewingFavorites", true);
                        finish();
                        startActivity(intent);
                        return true;

                    case R.id.nav_back:
                        intent = getIntent();
                        intent.putExtra("viewingFavorites", false);
                        finish();
                        startActivity(intent);
                        return true;

                    case R.id.nav_about:
                        final AlertDialog d1 = new AlertDialog.Builder(Main.this)
                                .setTitle("About")
                                .setNegativeButton("Back", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // do nothing
                                    }
                                })
                                .setIcon(R.drawable.ic_launcher)
//                                .setMessage(Html.fromHtml(appName+" v"+verString+"<br>Author: arcyleung<br/><a href=\"http://arcyleung.com\">http://arcyleung.com</a><br/><br/><a href=\"https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=NVX2XKNCSFJKG\">Donate</a> to help support development!"))
                                .setMessage(Html.fromHtml(appName + " v" + verString + "<br>Author: arcyleung<br/><a href=\"http://arcyleung.com\">http://arcyleung.com</a><br/><br/>Please consider <a href=\"https://www.paypal.me/arcyleung\">Donating</a> to help support development! Thanks!"))
                                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                                          @Override
                                                          public void onDismiss(DialogInterface dialogInterface) {
                                                              if (prefs.getBoolean("FULLSCREEN", false)) {
                                                                  immersiveFullscreen();
                                                              }
                                                          }
                                                      }
                                )
                                .create();

                        d1.show();

                        ((TextView) d1.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                        return true;
                    case R.id.columns:
                        final Display display = ((WindowManager) Main.this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                        final AlertDialog.Builder d2 = new AlertDialog.Builder(Main.this);
                        LayoutInflater inflater = getLayoutInflater();
                        View dialogView = inflater.inflate(R.layout.number_picker, null);

                        final NumberPicker numberPicker = (NumberPicker) dialogView.findViewById(R.id.dialog_number_picker);
                        numberPicker.setMaxValue(MAX_COLUMNS_PORTRAIT);
                        numberPicker.setMinValue(MIN_COLUMNS_PORTRAIT);

                        d2.setTitle("Number of columns");
                        if (display.getRotation() == Surface.ROTATION_0) {
                            numberPicker.setMaxValue(MAX_COLUMNS_PORTRAIT);
                            numberPicker.setMinValue(MIN_COLUMNS_PORTRAIT);
                            d2.setMessage("Default portrait: " + DEFAULT_COLUMNS_PORTRAIT);
                            numberPicker.setValue(prefs.getInt("COLUMNS_PORTRAIT", DEFAULT_COLUMNS_PORTRAIT));
                        } else {
                            numberPicker.setMaxValue(MAX_COLUMNS_LANDSCAPE);
                            numberPicker.setMinValue(MIN_COLUMNS_LANDSCAPE);
                            d2.setMessage("Default landscape: " + DEFAULT_COLUMNS_LANDSCAPE);
                            numberPicker.setValue(prefs.getInt("COLUMNS_LANDSCAPE", DEFAULT_COLUMNS_LANDSCAPE));
                        }
                        d2.setView(dialogView);

                        numberPicker.setWrapSelectorWheel(false);
                        d2.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                            int rotation = display.getRotation();

                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (rotation == Surface.ROTATION_0) {
                                    prefsEditor.putInt("COLUMNS_PORTRAIT", numberPicker.getValue());
                                    if (numberPicker.getValue() == 1)
                                        prefsEditor.putInt("THUMBNAIL_SIZE", 600);
                                    else
                                        prefsEditor.putInt("THUMBNAIL_SIZE", 300);
                                } else {
                                    prefsEditor.putInt("THUMBNAIL_SIZE", 300);
                                    prefsEditor.putInt("COLUMNS_LANDSCAPE", numberPicker.getValue());
                                }
                                prefsEditor.apply();
                                initializeAdapter();
                                finish();
                                overridePendingTransition(R.transition.fade_in, R.transition.fade_out);
                                startActivity(new Intent(getApplicationContext(), Main.class));
                                overridePendingTransition(R.transition.fade_in, R.transition.fade_out);
                            }
                        });
                        d2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        });
                        AlertDialog alertDialog = d2.create();
                        alertDialog.show();

                        return true;
                }
                return false;
            }
        });

        display = getWindowManager().getDefaultDisplay();
        screenWidth = getDisplaySize(display).x;
        screenHeight = getDisplaySize(display).y;

        selectedURL = "https://redditbooru.com/images/?sources=" + selectedString;
        runner = new LoadJSONasyncInit();

        if (!isNetworkAvailable()) {
            final AlertDialog d = new AlertDialog.Builder(Main.this)
                    .setTitle("No network connection")
//                                .setMessage("Author: arcyleung\nSite: http://arcyleung.com")
                    .setNegativeButton("Quit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            moveTaskToBack(true);
                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(1);
                        }
                    })
                    .setIcon(R.drawable.ic_launcher)
                    .setMessage("Sorry, please connect to Wi-Fi or cellular service and try again later!")
                    .create();

            d.show();
            return;
        }

        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override

            public void onRefresh() {
                // Implement refresh adapter code
                finish();
                overridePendingTransition(R.transition.fade_in, R.transition.fade_out);
                startActivity(new Intent(getApplicationContext(), Main.class));
                overridePendingTransition(R.transition.fade_in, R.transition.fade_out);

            }

        });

        swipeContainer.setColorSchemeResources(
                android.R.color.holo_red_light);

        // Image grid adapter
        initializeAdapter();

        // Set icon click listener
        setOnClickListener();

        ViewCompat.setNestedScrollingEnabled(staggeredGridView, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            staggeredGridView.setNestedScrollingEnabled(true);
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        if (timeToUpdate) {
            Toast.makeText(getApplicationContext(),
                    "Updating subreddit index...",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void initializeAdapter() {
        try {
            if (viewingFavorites == false) {
                jsonObjs = new JSONArray();
                jsonObjs = runner.execute(jsonObjs).get();
                tmp = addToArry(jsonObjs);
            } else {
                tmp = gson.fromJson(prefs.getString("FAVORITES", "[]"), dataList);
            }

            if (tmp.size() == 0 && viewingFavorites){
                Toast.makeText(getApplicationContext(),
                        "You have not added any favorites yet",
                        Toast.LENGTH_LONG).show();
            }

            adapter = new DataAdapter(this, R.layout.staggered, tmp, showNsfw, showTitles);
            setTitle(Html.fromHtml("<font color='#ffffff'>" + appName + "</font>"));
            staggeredGridView = (StaggeredGridView) findViewById(R.id.gridView);
            staggeredGridView.setAdapter(adapter);
            staggeredGridView.setOnScrollListener(new EndlessScrollListener() {
                @Override
                public void onLoadMore(int page, int totalItemsCount) {
                    lm = new LoadMorePhotos();
                    lm.execute();
                }
            });
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                staggeredGridView.setNestedScrollingEnabled(true);
            }
        } catch (Exception e) {
            System.out.println("CRASHED");
            e.printStackTrace();
        }
    }

    public void immersiveFullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    public void setOnClickListener() {
        staggeredGridView.setOnItemClickListener(new StaggeredGridView.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                final Dialog dialog = new Dialog(getActivity());
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                final Data selected = datas.get(position);
                final InteractiveImageView zoomImageView = new InteractiveImageView(getActivity());
                isFABOpen = false;
                progressDialog = ProgressDialog.show(getActivity(), "Downloading", "...", true);

                new Thread() {
                    @Override
                    public void run() {
                        Bitmap img = null;
                        try {

                            final Bitmap tmp = new DownloadImage(zoomImageView).execute(selected.imgUrl).get();
                            if (tmp == null) {
                                Toast.makeText(getApplicationContext(),
                                        "Error rendering image: file corrupted or too large",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                img = tmp;
                                bitmapWidth = img.getWidth();
                                bitmapHeight = img.getHeight();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                if (prefs.getBoolean("FULLSCREEN", false)) {
                                    immersiveFullscreen();
                                }
                            }
                        });

                        try {
                            // code runs in a thread
                            final Bitmap finalImg = img;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); //before
                                    dialog.setContentView(R.layout.popup_imgview);
                                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
//                                    dialog.getWindow().setFeatureDrawable();

                                    InteractiveImageView image = (InteractiveImageView) dialog.findViewById(R.id.imageview);
                                    image.setImageBitmap(finalImg);

                                    dialog.getWindow().setLayout(screenWidth, screenHeight);
                                    dialog.getWindow().setDimAmount(.9f);
                                    dialog.show();
                                    immersiveFullscreen();

                                    close = (FloatingActionButton) dialog.findViewById(R.id.imageClose);
                                    fab = (FloatingActionButton) dialog.findViewById(R.id.fab);
                                    fab1 = (FloatingActionButton) dialog.findViewById(R.id.fab1);
                                    fab2 = (FloatingActionButton) dialog.findViewById(R.id.fab2);
                                    fab3 = (FloatingActionButton) dialog.findViewById(R.id.fab3);
                                    fab4 = (FloatingActionButton) dialog.findViewById(R.id.fab4);
                                    fab5 = (FloatingActionButton) dialog.findViewById(R.id.fab5);

                                    if (display.getRotation() == Surface.ROTATION_0) {
                                        // Vertical
                                        float portTranslate = displayMetrics.heightPixels * 0.09f;
                                        fab5.animate().translationX(-portTranslate);
                                    } else {
                                        // Horizontal
                                        float landTranslate = displayMetrics.heightPixels * 0.15f;
                                        fab5.animate().translationX(-landTranslate);
                                    }

                                    fab.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            if (!isFABOpen) {
                                                showFABMenu();
                                            } else {
                                                closeFABMenu();
                                            }
                                        }
                                    });

                                    fab1.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            new AlertDialog.Builder(Main.this)
                                                    .setTitle("Confirm")
                                                    .setMessage("Do you want to set this wallpaper?")
                                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                                        public void onClick(DialogInterface dialog, int whichButton) {

                                                            progressDialog = ProgressDialog.show(getActivity(), "Setting Wallpaper", "...", true);

                                                            new Thread() {
                                                                @Override
                                                                public void run() {
                                                                    WallpaperManager wallMan = WallpaperManager.getInstance(getApplicationContext());
                                                                    Looper.prepare();
                                                                    try {
                                                                        wallMan.setBitmap(finalImg);
                                                                        progressDialog.dismiss();

                                                                        Main.this.runOnUiThread(new Runnable() {
                                                                            public void run() {
                                                                                Toast.makeText(getApplicationContext(),
                                                                                        "Wallpaper set!",
                                                                                        Toast.LENGTH_LONG).show();
                                                                            }
                                                                        });

                                                                    } catch (Exception e) {
                                                                        progressDialog.dismiss();
                                                                        Main.this.runOnUiThread(new Runnable() {
                                                                            public void run() {
                                                                                Toast.makeText(getApplicationContext(),
                                                                                        "Failed to set wallpaper.",
                                                                                        Toast.LENGTH_LONG).show();
                                                                            }
                                                                        });
                                                                    }
                                                                }
                                                            }.start();


                                                        }
                                                    })
                                                    .setNegativeButton(android.R.string.no, null).show();
                                        }
                                    });

                                    fab2.setOnClickListener(new View.OnClickListener() {
                                        public void onClick(View view) {
                                            Bitmap bitmap;
                                            OutputStream output;

                                            // Retrieve the image from the res folder
                                            bitmap = finalImg;

                                            // Create a new folder AndroidBegin in SD Card
                                            File dir = new File(externalStorageDirectory.getAbsolutePath() + "/MoBooru/");
                                            dir.mkdirs();

                                            // Create a name for the saved image
                                            File file = new File(dir, "tmp.png");

                                            try {
                                                // Share Intent
                                                Intent share = new Intent(Intent.ACTION_SEND);

                                                // Type of file to share
                                                share.setType("image/jpeg");

                                                output = new FileOutputStream(file);

                                                // Compress into png format image from 0% - 100%
                                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
                                                output.flush();
                                                output.close();

                                                // Locate the image to Share
                                                Uri uri = Uri.fromFile(file);

                                                // Pass the image into an Intnet
                                                share.putExtra(Intent.EXTRA_STREAM, uri);

                                                // Show the social share chooser list
                                                startActivity(Intent.createChooser(share, "Share image"));

                                            } catch (Exception e) {
                                                // TODO Auto-generated catch block
                                                e.printStackTrace();
                                            }
                                        }
                                    });

                                    fab3.setOnClickListener(new View.OnClickListener() {
                                        public void onClick(View view) {
                                            Long time = System.nanoTime();

                                            ContentValues values = new ContentValues();
                                            values.put(MediaStore.Images.Media.TITLE, Long.toString(time));
                                            values.put(MediaStore.Images.Media.DISPLAY_NAME, Long.toString(time));
                                            values.put(MediaStore.Images.Media.DESCRIPTION, "");
                                            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                                            values.put(MediaStore.Images.Media.DATE_ADDED, time);
                                            values.put(MediaStore.Images.Media.DATE_TAKEN, time);

                                            Uri uri = null;

                                            try {
                                                uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                                                if (finalImg != null) {
                                                    fixMediaDir();
                                                    OutputStream imageOut = getContentResolver().openOutputStream(uri);

                                                    try {
                                                        finalImg.compress(Bitmap.CompressFormat.JPEG, 100, imageOut);
                                                    } finally {
                                                        imageOut.close();
                                                    }

                                                    Toast.makeText(getApplicationContext(),
                                                            "Saved image " + Long.toString(time) + " to gallery",
                                                            Toast.LENGTH_LONG).show();
                                                } else {
                                                    Toast.makeText(getApplicationContext(),
                                                            "Failed to save image " + Long.toString(time) + " to gallery",
                                                            Toast.LENGTH_LONG).show();
                                                    getContentResolver().delete(uri, null, null);
                                                    uri = null;
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                Toast.makeText(getApplicationContext(),
                                                        "Failed to save image " + Long.toString(time) + " to gallery, please check storage permissions",
                                                        Toast.LENGTH_LONG).show();
                                                if (uri != null) {
                                                    getContentResolver().delete(uri, null, null);
                                                    uri = null;
                                                }
                                            }
                                        }
                                    });

                                    fab4.setOnClickListener(new View.OnClickListener() {
                                        public void onClick(View view) {
                                            try {
                                                // Browser Intent
                                                Intent redditPost = new Intent(Intent.ACTION_VIEW);

                                                // Type of file to share
                                                redditPost.setData(Uri.parse("https://redd.it/" + selected.redditSrc));
                                                startActivity(redditPost);

                                            } catch (Exception e) {
                                                // TODO Auto-generated catch block
                                                e.printStackTrace();
                                            }
                                        }
                                    });

                                    fab5.setOnClickListener(new View.OnClickListener() {
                                        public void onClick(View view) {
                                            try {
                                                favorites.add(selected);
                                                String serial = gson.toJson(favorites, dataList);
                                                prefsEditor.putString("FAVORITES", serial);
                                                prefsEditor.apply();
                                                System.out.println(serial);
                                            } catch (Exception e) {
                                                // TODO Auto-generated catch block
                                                e.printStackTrace();
                                            }
                                        }
                                    });

                                    close.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            isFABOpen = false;
                                            dialog.dismiss();
                                            if (prefs.getBoolean("FULLSCREEN", false)) {
                                                immersiveFullscreen();
                                            }
                                        }
                                    });
                                    progressDialog.dismiss();
                                }
                            });
                        } catch (final Exception ex) {
                        }
                    }
                }.start();
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void showFABMenu() {
        fab.animate().rotation(45);
        isFABOpen = true;

        if (display.getRotation() == Surface.ROTATION_0) {
            // Vertical
            float portTranslate = displayMetrics.heightPixels * 0.09f;
            fab1.animate().translationY(-portTranslate);
            fab2.animate().translationY(2 * -portTranslate);
            fab3.animate().translationY(3 * -portTranslate);
            fab4.animate().translationY(4 * -portTranslate);
        } else {
            // Horizontal
            float landTranslate = displayMetrics.heightPixels * 0.15f;
            fab1.animate().translationY(-landTranslate);
            fab2.animate().translationY(2 * -landTranslate);
            fab3.animate().translationY(3 * -landTranslate);
            fab4.animate().translationY(4 * -landTranslate);
        }
    }

    private void closeFABMenu() {
        isFABOpen = false;
        fab.animate().rotation(0);
        fab1.animate().translationY(0);
        fab2.animate().translationY(0);
        fab3.animate().translationY(0);
        fab4.animate().translationY(0);
    }

    // Append more data into the adapter
    public void customLoadMoreDataFromApi(int offset) {
        // This method probably sends out a network request and appends new data items to your adapter.
        // Use the offset value and add it as a parameter to your API request to retrieve paginated data.
        // Deserialize API response and then construct new objects to append to the adapter
    }

    // External storage
    void fixMediaDir() {
        File sdcard = Environment.getExternalStorageDirectory();
        if (sdcard != null) {
            File mediaDir = new File(sdcard, "DCIM/Camera");
            if (!mediaDir.exists()) {
                mediaDir.mkdirs();
            }
        }
    }

    // Rebuild index with new sub metadata
    //
    public void setFavoriteSubs() {
        selectedString = "";
        for (int i : selectedSubs) {
            selectedString += i + "%2C";
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    public Context getActivity() {
        return this;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        if (((RelativeLayout) (navigationView.findViewById(R.id.nsfw_toggle)) == null)) {
            finish();
        }

        ((RelativeLayout) (findViewById(R.id.nav_subs))).getChildAt(0).setVisibility(viewingFavorites ? View.GONE : View.VISIBLE);
        ((RelativeLayout) (findViewById(R.id.nav_favorites))).getChildAt(0).setVisibility(viewingFavorites ? View.GONE : View.VISIBLE);
        ((RelativeLayout) (findViewById(R.id.nav_back))).getChildAt(0).setVisibility(viewingFavorites ? View.VISIBLE : View.GONE);

        SwitchCompat nsfwToggle = (SwitchCompat) ((RelativeLayout) (navigationView.findViewById(R.id.nsfw_toggle))).getChildAt(0);
        SwitchCompat fullScreenToggle = (SwitchCompat) ((RelativeLayout) (navigationView.findViewById(R.id.fullscreen_toggle))).getChildAt(0);
        SwitchCompat titlesToggle = (SwitchCompat) ((RelativeLayout) (navigationView.findViewById(R.id.title_toggle))).getChildAt(0);
        nsfwToggle.setChecked(prefs.getBoolean("SHOW_NSFW", false));
        fullScreenToggle.setChecked(prefs.getBoolean("FULLSCREEN", false));
        titlesToggle.setChecked(prefs.getBoolean("SHOW_TITLES", true));

        nsfwToggle.setOnClickListener(new CompoundButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                prefsEditor.putBoolean("SHOW_NSFW", !prefs.getBoolean("SHOW_NSFW", false));
                prefsEditor.apply();
                Intent intent = getIntent();
                finish();
                overridePendingTransition(R.transition.fade_in, R.transition.fade_out);
                startActivity(intent);
                overridePendingTransition(R.transition.fade_in, R.transition.fade_out);
            }
        });
        fullScreenToggle.setOnClickListener(new CompoundButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                prefsEditor.putBoolean("FULLSCREEN", !prefs.getBoolean("FULLSCREEN", false));
                prefsEditor.apply();
                Intent intent = getIntent();
                finish();
                overridePendingTransition(R.transition.fade_in, R.transition.fade_out);
                startActivity(intent);
                overridePendingTransition(R.transition.fade_in, R.transition.fade_out);
            }
        });
        titlesToggle.setOnClickListener(new CompoundButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                prefsEditor.putBoolean("SHOW_TITLES", !prefs.getBoolean("SHOW_TITLES", true));
                prefsEditor.apply();
                Intent intent = getIntent();
                finish();
                overridePendingTransition(R.transition.fade_in, R.transition.fade_out);
                startActivity(intent);
                overridePendingTransition(R.transition.fade_in, R.transition.fade_out);
            }
        });
        return true;
    }

    public ArrayList<Data> addToArry(JSONArray ja) {
        if (loadingMore) {

            //IMPLEMENT STOP LOADING ONCE ARRAYSIZE < PAGESIZE
            if (ja == null) {
                loadingMore = false;
            } else {
                if (ja.length() < pageSize) {
                    pageSize = ja.length();
                }
                for (int i = 0; i < pageSize; i++) {
                    Data data = new Data();
                    try {
                        // Image data
                        data.imgUrl = ja.getJSONObject(i).getString("cdnUrl");
                        data.width = ja.getJSONObject(i).getInt("width");
                        data.height = ja.getJSONObject(i).getInt("height");
                        data.rat = 1.0 * data.height / data.width;
                        data.thumbImgUrl = ja.getJSONObject(i).getString("thumb") + "_" + thumbnail_size + "_" + thumbnail_size + ".jpg";

                        // Metadata
                        data.score = ja.getJSONObject(i).getString("score");
                        data.nsfw = ja.getJSONObject(i).getBoolean("nsfw");
                        data.redditSrc = ja.getJSONObject(i).getString("externalId");
                        data.title = ja.getJSONObject(i).getString("title").replaceAll("\\s*\\[.+?\\]\\s*", "").replace("&amp;", "&"); //+"\n("+data.score+"\uD83D\uDD3A)";
                        data.series = ja.getJSONObject(i).getString("title").replaceAll("^[^\\[]*", "").replace("&amp;", "&");
//                        data.ogSrc = ja.getJSONObject(i).getString("sourceUrl");
                        if (i == pageSize - 1) {
                            lastIndexTime = Long.parseLong(ja.getJSONObject(i).getString("dateCreated"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (data.ogSrc.equals("null")) {
                        data.ogSrc = "";
                    }
                    if (data.thumbImgUrl.equals("null")) {
                        data.thumbImgUrl = "";
                    }
                    datas.add(data);
                }
            }
        }
        return datas;
    }

    private class UpdateIndex extends AsyncTask<Void, Integer, Void> {
        int prog = 0;

        @Override
        protected Void doInBackground(Void... params) {

            for (Sub s : subsList.values()) {
                String info;
                JSONObject obj;
                try {
                    String aboutURL = "https://www.reddit.com/" + s.subName + "/about.json";
                    URL url = new URL(aboutURL);
                    Scanner scan = new Scanner(url.openStream());
                    info = "";
                    while (scan.hasNext())
                        info += scan.nextLine();
                    scan.close();
                    obj = new JSONObject(info).getJSONObject("data");
                    subsList.put(
                            s.subID,
                            new Sub(
                                    s.subName,
                                    s.subID,
                                    obj.getInt("subscribers"),
                                    s.selected,
                                    obj.getBoolean("over18"),
                                    obj.getString("public_description")
                            )
                    );
                    String serial = gson.toJson(subsList, intSubMap);
                    prefsEditor.putString("SUBS", serial);
                    prefsEditor.apply();
                } catch (Exception ex) {
//                ex.printStackTrace();
                    subsList.put(s.subID, new Sub(s.subName, s.subID, 0, s.selected, false, ""));
                }
                publishProgress((int) Math.ceil(prog * 1.0 / subsList.values().size() * 100 * progressBarScale));
                prog++;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            prefsEditor.putLong("UPDATE_TIME", System.currentTimeMillis());
            prefsEditor.apply();
            detProgressBar.setVisibility(View.GONE);
            timeToUpdate = false;
        }

        protected void onPreExecute() {
            detProgressBar.setVisibility(View.VISIBLE);
        }

        protected void onProgressUpdate(Integer... values) {
            ProgressBarAnimation anim = new ProgressBarAnimation(detProgressBar, detProgressBar.getProgress(), values[0]);
            anim.setDuration(1000);
            detProgressBar.startAnimation(anim);
        }
    }

    public class ProgressBarAnimation extends Animation {
        private ProgressBar progressBar;
        private float from;
        private float to;

        public ProgressBarAnimation(ProgressBar progressBar, float from, float to) {
            super();
            this.progressBar = progressBar;
            this.from = from;
            this.to = to;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            float value = from + (to - from) * interpolatedTime;
            progressBar.setProgress((int) value);
        }

    }

    private class DownloadImage extends AsyncTask<String, Void, Bitmap> {
        InteractiveImageView bmImage;
        ProgressDialog pDialog;

        DownloadImage(InteractiveImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected void onPreExecute() {
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap img = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                img = BitmapFactory.decodeStream(in);
                int bitmapSize = img.getByteCount();
                if (bitmapSize > MAX_BITMAP_SIZE) {
                    img = null;
                    throw new RuntimeException(
                            "Canvas: trying to draw too large(" + bitmapSize + "bytes) bitmap.");

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return img;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageDrawable(new BitmapDrawable(getResources(), result));
        }
    }

    private class LoadJSONasyncInit extends AsyncTask<JSONArray, Void, JSONArray> {

        protected JSONArray doInBackground(JSONArray... urls) {
            try {
                // Fetch subs list
                doc = Jsoup.connect(mainsite)
                        .header("Accept-Encoding", "gzip, deflate")
                        .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/23.0")
                        .maxBodySize(0)
                        .timeout(6000000)
                        .get();
                redditSubs = doc.select("script");
                int i = 0;
                for (Element sub : redditSubs) {
                    String at = sub.toString();
                    if (i == 2) {
                        subsJSON = at;
                    }
                    i++;
                }

                subsJSON = subsJSON.substring(subsJSON.indexOf("["));
                subsJSON = subsJSON.substring(0, subsJSON.indexOf("]") + 1);
                arr = new JSONArray(subsJSON);

                if (timeToUpdate || arr.length() > subsList.size()) {
                    for (int j = 0; j < arr.length(); j++) {
                        subsList.put(arr.getJSONObject(j).getInt("value"), new Sub(arr.getJSONObject(j).getString("name"), arr.getJSONObject(j).getInt("value")));
                    }
                    executeAsyncTask(new UpdateIndex());

//                    new Thread() {
//                        @Override
//                        public void run() {
//
//                            updateSubsList();
//                            detProgressBar.setVisibility(View.GONE);
//                        }
//                    }.start();
//
//                    new Thread(new Runnable() {
//                        public void run() {
//                            int progressStatusCounter = 0;
//                            while (progressStatusCounter < 100) {
//                                progressStatusCounter += 2;
//                                final int finalProgressStatusCounter = progressStatusCounter;
//                                progressHandler.post(new Runnable() {
//                                    public void run() {
//                                        detProgressBar.setProgress(finalProgressStatusCounter);
//                                    }
//                                });
//                                try {
//                                    Thread.sleep(300);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        }
//                    }).start();
                }


            } catch (Exception e) {
                e.printStackTrace();
            }

            String str = "";

            try {
                selectedURL = "https://redditbooru.com/images/?sources=" + selectedString + "&afterDate=";
                url1 = new URL(selectedURL + lastIndexTime);

                Scanner scan = new Scanner(url1.openStream());
                while (scan.hasNext())
                    str += scan.nextLine();
                scan.close();

                jsonObjs = new JSONArray(str);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return jsonObjs;
        }

        @Override
        protected void onPostExecute(JSONArray result) {
            jsonObjs = result;
        }
    }

    private class LoadMorePhotos extends AsyncTask<Void, Void, Void> {
        JSONArray tmp;

        @Override
        protected Void doInBackground(Void... arg0) {
            // SET LOADING MORE "TRUE"
            loadingMore = true;

            // INCREMENT CURRENT PAGE
            current_page += 1;

            try {
                // refactor into string scanner
                selectedURL = "https://redditbooru.com/images/?sources=" + selectedString + "&afterDate=";
                url1 = new URL(selectedURL + lastIndexTime);

                Scanner scan = new Scanner(url1.openStream());
                String str = "";
                while (scan.hasNext())
                    str += scan.nextLine();
                scan.close();

                tmp = new JSONArray(str);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPreExecute() {
            inDetProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Void result) {

            // get listview current position - used to maintain scroll position
//            int currentPosition = staggeredGridView.getFirstVisiblePosition();

            // APPEND NEW DATA TO THE ARRAYLIST AND SET THE ADAPTER TO THE
            // LISTVIEW
            datas = addToArry(tmp);
            adapter.datas = datas;
            adapter.notifyDataSetChanged();


            // SET LOADINGMORE "FALSE" AFTER ADDING NEW FEEDS TO THE EXISTING
            // LIST
            loadingMore = false;
            inDetProgressBar.setVisibility(View.GONE);
        }
    }
}
