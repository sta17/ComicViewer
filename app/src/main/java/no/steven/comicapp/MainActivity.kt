package no.steven.comicapp

import android.Manifest
import androidx.appcompat.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.text.InputType.TYPE_CLASS_NUMBER
import android.util.JsonReader
import android.util.Log.d
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.content_main.*
import java.io.*
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import no.steven.comicapp.fragment_handling.ManagerFragment


// https://github.com/shortcut/android-coding-assignment - task.
// https://www.journaldev.com/10096/android-viewpager-example-tutorial - viewpager tutorial.
// https://tutorial.eyehunts.com/android/android-toolbar-example-android-app-bar-kotlin/ - action bar example
// http://actionbarsherlock.com/ - action bar stuff
// https://xkcd.com/
// https://www.flaticon.com/free-icon/transparency_1076744#term=search&page=1&position=5 - Designed by ? from Flaticon - Gear
// https://www.flaticon.com/free-icon/transparency_1076744#term=search&page=1&position=5 - Designed by Freepik from Flaticon - Magnifying glass.
// https://www.flaticon.com/free-icon/star_149222 - Designed by smashicons from Flaticon - Star
// https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html - Comic Icon
// http://romannurik.github.io/AndroidAssetStudio/ - Asset generators
// https://codingjuction.com/2018/12/13/how-to-make-image-slider-by-view-pager-in-the-android-studio-kotlin/ - sliding/flick viewpager tutorial
// https://github.com/thedeveloperworldisyours/CommunicatingActivityWithFragmentsTabBar
// https://thedeveloperworldisyours.com/android/communicating-activity-with-fragments-in-tabbar-2/#sthash.jPZCqJLO.LzcU74z1.dpbs
//TODO: make most of the comic pages cleanable as junk except for favourites.
//TODO: set the page update to happen upon comic image download, immediately, rather then second button push.
//BUG: update image on button click. Handle download done, update now and clean download handling code.
class MainActivity : AppCompatActivity() {

    private var currentComicNumber = 0
    private var latestComicNumber = 0
    private var firstComicNumber = 1

    private var comicList = mutableListOf<Int>()
    private var favouriteComicList = mutableListOf<Int>()

    private var downloadRefIdList: MutableMap<Long, Int> = mutableMapOf()
    private var imgRefIdList: MutableMap<Long, Int> = mutableMapOf()
    private var jsonRefIdList: MutableMap<Long, Int> = mutableMapOf()

    private var sharedPrefs = "Steven's a Comic App"

    private lateinit var downloadLocation: File

    private lateinit var viewpager : ViewPager

    private lateinit var mManagerFragment: ManagerFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        downloadLocation = this.getExternalFilesDir(DIRECTORY_DOWNLOADS)!!

        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            if (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.INTERNET
                )
            ) {
                // Normal stuff here
                // Need to move stuff into here? since otherwise, it get weird if they say no.
            } else {
                //if permission is not granted, then we ask for it
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.INTERNET
                    ),
                    1
                )
            }
        }

        //Setup
        loadPrefs() // get the preferences
        getComic(-1) // get latest
        downloadFavourites(favouriteComicList) // get favorites
        //updateComic(currentComicNumber) // get the last viewed

        viewpager = findViewById(R.id.viewpager)

        mManagerFragment = ManagerFragment.newInstance(downloadLocation,currentComicNumber)

        viewpager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                // https://developer.android.com/reference/androidx/viewpager/widget/ViewPager.OnPageChangeListener.html
                // https://stackoverflow.com/questions/11293300/determine-when-a-viewpager-changes-pages
                // https://stackoverflow.com/questions/8117523/how-do-you-get-the-current-page-number-of-a-viewpager-for-android
                // TODO: Start here figure out change.
            }

            override fun onPageSelected(position: Int) {
                // Check if this is the page you want.
            }
        })

        buttonFirst.setOnClickListener {
            updateComic(1)
        }

        buttonLast.setOnClickListener {
            updateComic(latestComicNumber)
        }

    }

    //setting menu in action bar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (currentComicNumber in favouriteComicList) {
            menu!!.findItem(R.id.action_favourite).setIcon(R.drawable.star_filled)

        } else {
            menu!!.findItem(R.id.action_favourite).setIcon(R.drawable.star)
        }
        return super.onPrepareOptionsMenu(menu)
    }

    // actions on click menu items
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        R.id.action_search -> {
            val dDialog = AlertDialog.Builder(this)
            val input = EditText(this)
            input.inputType = TYPE_CLASS_NUMBER

            dDialog.setTitle("Comic Retrieval")
                .setMessage("Please Input desired comic number")
                .setIcon(R.drawable.transparency)
                .setCancelable(true)
                .setView(input)
                .setPositiveButton("Go") { _, _ -> updateComic(input.text.toString().toInt()) }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
                .create()
                .show()
            true
        }
        R.id.action_favourite -> {
            if (currentComicNumber in favouriteComicList) {
                favouriteComicList.remove(currentComicNumber)
                Toast.makeText(applicationContext, "Favourite Removed", Toast.LENGTH_LONG).show()
            } else {
                favouriteComicList.add(currentComicNumber)
                Toast.makeText(applicationContext, "Favourite Added", Toast.LENGTH_LONG).show()
            }
            this.invalidateOptionsMenu()
            true
        }
        R.id.action_credits -> {
            val dDialog = AlertDialog.Builder(this, R.style.AppTheme_DialogTheme)
                .setTitle("Credits")
                .setMessage(
                    "xkcd by Randall Munroe at xkcd.com"
                            + System.getProperty("line.separator") + System.getProperty("line.separator") + "Magnifying glass/Search button designed by Freepik from Flaticon "
                            + System.getProperty("line.separator") + System.getProperty("line.separator") + "Star/Favourite button designed by smashicons from Flaticon"
                            + System.getProperty("line.separator") + System.getProperty("line.separator") + "App by Steven Aanetsen."
                )
                .setIcon(R.mipmap.ic_launcher)
                .setCancelable(true)
                .setNegativeButton("Back") { dialog, _ -> dialog.cancel() }
                .create()
            dDialog.show()
            true
        }
        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    private var onComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            // get the RefId from the download manager
            val referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            // check if download is json download
            if (jsonRefIdList.contains(referenceId)) {
                val toGet: Int = jsonRefIdList[referenceId]!!
                jsonRefIdList.remove(referenceId)

                // check if latest comic
                if (toGet == 0 || toGet == -1) {
                    handleLatest(toGet)
                } else {
                    handleJson(toGet, referenceId)
                }
            } else if (imgRefIdList.contains(referenceId)) {
                handleImage(referenceId)
            }
        }
    }

    private fun handleLatest(state: Int): Boolean {
        val downloadedComic =
            loadJson("latest.json",downloadLocation)
        val toGet = downloadedComic.number
        latestComicNumber = downloadedComic.number

        if ((state == 0) && (!File(downloadLocation, "$toGet.json").exists())) {
            val notRenamed = File(
                downloadLocation,
                "/latest.json"
            )
            val renameTo = File(
                downloadLocation,
                "/$toGet.json"
            )

            if ((notRenamed.exists()) && (!renameTo.exists()) && (toGet !in comicList)) {
                notRenamed.renameTo(renameTo)
            }
        }

        if ((state == 0) && (!File(downloadLocation, "$toGet.png").exists())) {
            val imageUrl = downloadedComic.imgUrl
            val refIdImg = download(
                imageUrl,
                "$toGet.png",
                "png",
                "Comic Downloader",
                this
            )
            downloadRefIdList[refIdImg] = toGet
            imgRefIdList[refIdImg] = toGet
        }
        latestComicNumber = toGet
        return true
    }

    private fun handleJson(toGet: Int, referenceId: Long): Boolean {
        val downloadedComic = loadJson("$toGet.json",downloadLocation)

        val imageUrl = downloadedComic.imgUrl

        if (!File(downloadLocation, "$toGet.png").exists()) {
            val refIdImg = download(
                imageUrl,
                "$toGet.png",
                "png",
                "Comic Downloader",
                this
            )
            downloadRefIdList.remove(referenceId)
            downloadRefIdList[refIdImg] = toGet
            imgRefIdList[refIdImg] = toGet
        }
        return true
    }

    private fun handleImage(referenceId: Long): Boolean {
        val toGet: Int = imgRefIdList[referenceId]!!

        imgRefIdList.remove(referenceId)
        downloadRefIdList.remove(referenceId)
        comicList.add(toGet)

        //var view = viewpager[toGet]
        //do stuff
        //viewpager[toGet] = view // ERROR NOT POSSIBLE

        if(toGet == currentComicNumber){
            if(toGet in favouriteComicList){
                mManagerFragment.update(toGet,true)
            }else {
                mManagerFragment.update(toGet,false)
            }
            viewpager.adapter?.notifyDataSetChanged()
            //var group = viewpager.adapter as ViewPagerAdapter
            //var view = group.updateGraphics(toGet,viewpager[toGet])
            //viewpager.removeViewAt(toGet)
            //viewpager.addView(view,toGet)
        }

        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onComplete)
        saveState()
    }

    override fun onStop() {
        super.onStop()
        saveState()
    }

    private fun getComic(toGet: Int): Boolean {
        if((toGet in comicList) && (firstComicNumber <= toGet) && (toGet <= latestComicNumber)){
            return true
        } else if (toGet == 0 || toGet == -1) {
            val latest = File(
                downloadLocation,
                "latest.json"
            )
            latest.absoluteFile.delete()
            if (latest.exists()){
                latest.absoluteFile.delete()
            }

            val refIdJson = download("https://xkcd.com/info.0.json", "latest.json", "txt","Comic Downloader",this)

            downloadRefIdList[refIdJson] = toGet
            jsonRefIdList[refIdJson] = toGet

            return false

        } else if (!File(downloadLocation, "$toGet.json").exists())  {
            val refIdJson = download("https://xkcd.com/$toGet/info.0.json", "$toGet.json", "txt","Comic Downloader", this)

            downloadRefIdList[refIdJson] = toGet
            jsonRefIdList[refIdJson] = toGet
            return false

        } else if (!File(downloadLocation, "$toGet.png").exists()) {
            val refIdImg = download(loadJson("$toGet.json",downloadLocation).imgUrl, "$toGet.png", "png","Comic Downloader", this)

            downloadRefIdList[refIdImg] = toGet
            imgRefIdList[refIdImg] = toGet
            return false

        } else {
            comicList.add(toGet)
            return true
        }
    }

    private fun downloadFavourites(favouriteComicList: MutableList<Int>) {
        if (favouriteComicList.isNotEmpty()) {
            val iterator = favouriteComicList.listIterator()
            for (comicNumber in iterator) {
                getComic(comicNumber)
            }
        }
    }

    /*
    load preferences, comicList and favourites
     */
    private fun loadPrefs() {
        val prefs = getSharedPreferences(sharedPrefs, Context.MODE_PRIVATE)
        if ((prefs.contains("initialized")) && (prefs.getBoolean("initialized", false))) {
            latestComicNumber = prefs.getInt("latestComicNumber", latestComicNumber)
            currentComicNumber = prefs.getInt("currentComicNumber", currentComicNumber)

            if(File(downloadLocation, "comicList.json").exists()){
                comicList = comicList.union(loadList("comicList.json",downloadLocation)).toMutableList()
            }else{
                Toast.makeText(
                    applicationContext,
                    "Comic list not found. No Comics in list.",
                    Toast.LENGTH_LONG
                ).show()
                d("error","ComicList file was not found.")
            }

            if(File(downloadLocation, "favouriteComicList.json").exists()){
                favouriteComicList = favouriteComicList.union(loadList("favouriteComicList.json",downloadLocation)).toMutableList()
            }else{
                Toast.makeText(
                    applicationContext,
                    "Favourite Comic list not found. No Comics in list.",
                    Toast.LENGTH_LONG
                ).show()
                d("error","ComicList file was not found.")
            }

            Toast.makeText(
                applicationContext,
                resources.getString(R.string.preferences_loaded),
                Toast.LENGTH_LONG
            ).show()
        } else {
            latestComicNumber = 1
            currentComicNumber = 1
            Toast.makeText(
                applicationContext,
                resources.getString(R.string.new_app_set_up),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /*
    save preferences, comicList and favourites
     */
    private fun saveState() {
        val prefs = getSharedPreferences(sharedPrefs, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt("latestComicNumber", latestComicNumber)
        editor.putInt("currentComicNumber", currentComicNumber)
        editor.putBoolean("initialized", true)
        editor.apply()
        Toast.makeText(applicationContext, "Preferences Saved", Toast.LENGTH_SHORT).show()
        saveList(comicList, "comicList.json",downloadLocation)
        saveList(
            favouriteComicList,
            "favouriteComicList.json",downloadLocation
        )
    }

    private fun updateComic(number: Int) {
        if ((firstComicNumber <= number) && (number <= latestComicNumber)) {
            val result = getComic(number)
            if (result) {
                d("updateComic", "updating number: $number")
                currentComicNumber = number
                viewpager.currentItem = currentComicNumber
                //viewpager.invalidate()
                viewpager.postInvalidate()
                viewpager.invalidate()
                viewpager.adapter?.notifyDataSetChanged()
            }
            //else if (!result){
            //while (number in imgRefIdList.values){}
            //    currentComicNumber = waitingToUpdateTo
            //    updateGraphics(currentComicNumber)
            //}
        }
        d("updateComic", "number: $number")
        d("updateComic", "comicList: $comicList")
        d("updateComic", "favouriteComicList: $favouriteComicList")
        d("updateComic", "latest: $latestComicNumber")
    }

}

