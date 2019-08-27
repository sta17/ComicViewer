package com.example.comicviewer

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.os.Bundle
import android.util.Log.d
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity;

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import android.graphics.BitmapFactory
import android.widget.ImageView
import java.net.URL
import java.io.File
import java.io.FileInputStream
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.util.Log
import androidx.core.graphics.drawable.toDrawable
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.os.Environment
import androidx.core.content.ContextCompat



// https://github.com/shortcut/android-coding-assignment - task
// https://xkcd.com/json.html - format
// https://xkcd.com/614/info.0.json - specific comic
// https://xkcd.com/info.0.json - current
// https://xkcd.com/2165/ - actual comic
class MainActivity : AppCompatActivity() {

    private var latestComicNumber = 2167
    private var lastViewedComic = 2166
    private var currentComicNumber = 2166

    private var comiclist = mutableListOf<Int>()
    private var refidlist: ArrayList<Long> = ArrayList()

    private var img_refidlist: MutableMap<Long, Int> = mutableMapOf()
    private var json_refidlist: MutableMap<Long, Int> = mutableMapOf()

    private val MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 1

    var SHARED_PREFS = "Stevens Comic Viewer"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        registerReceiver(onComplete,  IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        //Setup
        //loadPrefs()

        comiclist = comiclist.union(Filehandler(applicationContext).loadlist("comiclist.txt")).toMutableList()

        updateComic(getComic(lastViewedComic))

        buttonUpdate.setOnClickListener{
            updateComic(getComic(currentComicNumber))
        }

        buttonNext.setOnClickListener{
            if(legalnumber(currentComicNumber+1)) {
                updateComic(getComic(currentComicNumber+1))
            }
        }

        buttonPrevious.setOnClickListener{
            if(legalnumber(currentComicNumber-1)) {
                updateComic(getComic(currentComicNumber-1))
            }
        }

    }

    var onComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            // get the refid from the download manager
            val referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            refidlist.remove(referenceId)

            if(json_refidlist.contains(referenceId)){
                val toGet = json_refidlist[referenceId]
                json_refidlist.remove(referenceId)

                Log.d("getComic", "comic json aquired: " + toGet.toString())

                var downloadedComic = Filehandler(applicationContext).loadJson(toGet.toString() + ".json")
                val imageUrl = downloadedComic.imgUrl
                Filehandler(applicationContext).saveComic(downloadedComic,toGet.toString())

                Log.d("getComic", "img path provided: " + downloadedComic.imgPath)
                Log.d("getComic", "img url provided: " + downloadedComic.imgUrl)

                val refid_IMG = Filehandler(applicationContext).downloadComic(imageUrl,toGet.toString() + ".png","png")
                refidlist.add(refid_IMG);
                if (toGet != null) {
                    img_refidlist.put(refid_IMG,toGet)
                }

            }else if(img_refidlist.contains(referenceId)){
                val toGet = img_refidlist[referenceId]
                img_refidlist.remove(referenceId)
                Log.d("getComic", "comic image aquired: " + toGet.toString())
                if (toGet != null) {
                    currentComicNumber = toGet.toInt()
                }
                comiclist.add(currentComicNumber)
                var downloadedComic = Filehandler(applicationContext).loadJson(toGet.toString() + ".json")
                downloadedComic.imgPath = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"/ComicViewer/" + toGet.toString() + ".png").absolutePath
                Log.d("getComic", "comic image path: " + downloadedComic.imgPath)
                Filehandler(applicationContext).saveComic(downloadedComic,toGet.toString())

                updateComic(getComic(currentComicNumber))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onComplete)
    }

    override fun onStop() {
        super.onStop()
        savePrefs()
        Filehandler(applicationContext).savelist(comiclist,"comiclist.txt")
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        Log.d("permission", "onRequestPermissionsResult started")
        when (requestCode) {
            MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.d("permission", "permission granted")
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("permission", "permission denied")
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }


    fun getComic(toGet: Int): Comic {
        // convert to list checker, if in list attempt to load, if found, then send comic further along. if download accurs then add to list.

        Log.d("getComic", "comic number: " + toGet.toString())
        Log.d("getComic", "list content: " + comiclist.toString())

        if (toGet in comiclist){
            currentComicNumber = toGet
            lastViewedComic = currentComicNumber
            return Filehandler(applicationContext).loadJson(toGet.toString()+".json")
        } else {
            val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE
                )
            } else {
                //TODO
            }

            val refid_TXT = Filehandler(applicationContext).downloadComic("https://xkcd.com/" + toGet.toString() +"/info.0.json",toGet.toString() + ".json","txt")
            refidlist.add(refid_TXT);
            json_refidlist.put(refid_TXT,toGet);
        }

        return Comic(toGet,"Unknown","Comic "+toGet.toString()+" Not Available.","","")
    }

    fun loadPrefs() {
        val prefs =  getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(SHARED_PREFS, true)) {
            latestComicNumber = prefs.getInt("latestComicNumber",latestComicNumber)
            lastViewedComic = prefs.getInt("lastViewedComic",lastViewedComic)
            Toast.makeText(this,"Preferences Loaded",Toast.LENGTH_SHORT).show()
        } else {
            latestComicNumber = 2167
            lastViewedComic = 2166
        }
    }

    fun savePrefs(){
        val prefs =  getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        var editor = prefs.edit()
        editor.putInt("latestComicNumber",latestComicNumber)
        editor.putInt("lastViewedComic",lastViewedComic)
        editor.commit()
        //Toast.makeText(this,"Preferences Saved",Toast.LENGTH_SHORT).show()
    }

    fun updateComic(comic: Comic){
        displayAlt.setText(comic.altText)
        displayTitle.setText(comic.title)

        d("update_comic",comic.imgPath)
        val displayImage = findViewById<ImageView>(R.id.displayImage)

        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"/ComicViewer/" + comic.number.toString() + ".png")
        if(file.exists())
            displayImage.setImageBitmap(BitmapFactory.decodeStream(FileInputStream(File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"/ComicViewer/" + comic.number.toString() + ".png").absolutePath)))
        else
            displayImage.setImageDrawable(R.drawable.ic_launcher_foreground.toDrawable())
    }

    fun legalnumber(number: Int): Boolean {
        if(number < 1){
            d("legal number","to low: $number")
            return false
        }else if(latestComicNumber < number){
            d("legal number","to high: $number , $latestComicNumber")
            return false
        }else{
            d("legal number","acceptable: $number")
            return true
        }
    }
}
