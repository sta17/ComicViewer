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

//TODO: Fix the latest comic thing, the need to check for latest number and to update it.
class MainActivity : AppCompatActivity() {

    private var latestComicNumber = 0
    private var currentComicNumber = 0

    private var comiclist = mutableListOf<Int>()
    private var refidlist: ArrayList<Long> = ArrayList()

    private var img_refidlist: MutableMap<Long, Int> = mutableMapOf()
    private var json_refidlist: MutableMap<Long, Int> = mutableMapOf()

    private val MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 1
    private val MY_PERMISSIONS_READ_EXTERNAL_STORAGE = 1
    private val MY_PERMISSIONS_INTERNET = 1

    var SHARED_PREFS = "Stevens Comic Viewer"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        registerReceiver(onComplete,  IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        //Setup
        loadPrefs()

        buttonFirst.setOnClickListener{
            getComic(1)
        }

        buttonLast.setOnClickListener{
            getComic(0)
        }

        buttonUpdate.setOnClickListener{
            getComic(currentComicNumber)
        }

        buttonNext.setOnClickListener{
            if(legalnumber(currentComicNumber+1)) {
                getComic(currentComicNumber+1)
            }
        }

        buttonPrevious.setOnClickListener{
            if(legalnumber(currentComicNumber-1)) {
                getComic(currentComicNumber-1)
            }
        }

    }

    var onComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            // get the refid from the download manager
            val referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            refidlist.remove(referenceId)

            if(json_refidlist.contains(referenceId)){
                var toGet = json_refidlist[referenceId]
                json_refidlist.remove(referenceId)
                lateinit var downloadedComic:Comic

                if(toGet == 0) {
                    Log.d("onComplete", "latest comic json aquired")
                    downloadedComic = Filehandler(applicationContext).loadJson("latest.json")
                    toGet = downloadedComic.number
                    latestComicNumber = downloadedComic.number

                    val from = File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"/ComicViewer/latest.json")
                    val to = File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"/ComicViewer/" + toGet.toString() + ".json")

                    if ((from.exists()) && (!to.exists())){
                        from.renameTo(to)
                    } else {
                        latestComicNumber = toGet
                        currentComicNumber = toGet
                        if(!(toGet in comiclist)){
                            comiclist.add(toGet)
                        }
                        Log.d("onComplete", "comic number: " + toGet.toString())
                        Log.d("onComplete", "list content: " + comiclist.toString())
                        updateComic(toGet)
                        return
                    }

                } else {
                    Log.d("onComplete", "comic json aquired: " + toGet.toString())
                    downloadedComic = Filehandler(applicationContext).loadJson(toGet.toString() + ".json")
                }

                val imageUrl = downloadedComic.imgUrl

                Log.d("onComplete", "img path provided: " + downloadedComic.imgPath)
                Log.d("onComplete", "img url provided: " + downloadedComic.imgUrl)

                if(!File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"/ComicViewer/" + toGet.toString() + ".png").exists()){
                    val refid_IMG = Filehandler(applicationContext).download(imageUrl,toGet.toString() + ".png","png")
                    refidlist.add(refid_IMG);
                    if (toGet != null) {
                        img_refidlist.put(refid_IMG,toGet)
                    }
                }

            }else if(img_refidlist.contains(referenceId)){
                var toGet = img_refidlist[referenceId]
                if (toGet != null) {
                    toGet = toGet.toInt()
                }
                img_refidlist.remove(referenceId)
                Log.d("getComic", "comic image aquired: " + toGet.toString())

                currentComicNumber = toGet!!
                comiclist.add(toGet)

                updateComic(toGet)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onComplete)
        savePrefs()
        Filehandler(applicationContext).savelist(comiclist,"comiclist.json")
    }

    override fun onStop() {
        super.onStop()
        savePrefs()
        Filehandler(applicationContext).savelist(comiclist,"comiclist.json")
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
                    Log.d("permission", "write permission granted")
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("permission", "write permission denied")
                }
                return
            }
            MY_PERMISSIONS_READ_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.d("permission", "read permission granted")
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("permission", "read permission denied")
                }
                return
            }
            MY_PERMISSIONS_INTERNET -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.d("permission", "internet permission granted")
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("permission", "internet permission denied")
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


    fun getComic(toGet: Int): Boolean {
        Log.d("getComic", "comic number: " + toGet.toString())
        Log.d("getComic", "list content: " + comiclist.toString())

        if ((toGet in comiclist) && !(toGet == 0)){
            currentComicNumber = toGet
            Log.d("getComic", "comic in list")
            updateComic(toGet)
            return true
        } else {
            Log.d("getComic", "comic not in list")
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

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.INTERNET),
                    MY_PERMISSIONS_INTERNET
                )
            } else {
                //TODO
            }

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_READ_EXTERNAL_STORAGE
                )
            } else {
                //TODO
            }

            if(toGet == 0){
                val latest = File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"/ComicViewer/latest.json")
                if(latest.exists())
                    latest.delete()

                var refid_TXT = Filehandler(applicationContext).download("https://xkcd.com/info.0.json","latest.json","txt")
                refidlist.add(refid_TXT);
                json_refidlist.put(refid_TXT,toGet);
                Log.d("getComic", "getting latest")
                return false
            }else {
                if(!File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"/ComicViewer/" + toGet.toString() + ".json").exists()){
                    var refid_TXT = Filehandler(applicationContext).download("https://xkcd.com/" + toGet.toString() +"/info.0.json",toGet.toString() + ".json","txt")
                    refidlist.add(refid_TXT);
                    json_refidlist.put(refid_TXT,toGet);
                    Log.d("getComic", "getting comic: "+toGet.toString())
                    return false
                } else if(!File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"/ComicViewer/" + toGet.toString() + ".png").exists()) {
                    var refid_IMG = Filehandler(applicationContext).download(Filehandler(applicationContext).loadJson(toGet.toString() + ".json").imgUrl,toGet.toString() + ".png","png")
                    refidlist.add(refid_IMG);
                    img_refidlist.put(refid_IMG,toGet);
                    Log.d("getComic", "getting image: "+toGet.toString())
                    return false
                } else {
                    comiclist.add(toGet)
                    Log.d("getComic", "updated comiclist")
                    Log.d("getComic", "list content: " + comiclist.toString())
                    updateComic(toGet)
                    return true
                }
            }
        }
        Log.d("getComic", "get comic error")
        return false
    }

    fun loadPrefs() {
        val prefs =  getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        if((prefs.contains("initialized")) && (prefs.getBoolean("initialized",false) == true)){
            latestComicNumber = prefs.getInt("latestComicNumber",latestComicNumber)
            currentComicNumber = prefs.getInt("currentComicNumber",currentComicNumber)
            comiclist = comiclist.union(Filehandler(applicationContext).loadlist("comiclist.json")).toMutableList()
            Toast.makeText(this,"Preferences Loaded",Toast.LENGTH_SHORT).show()
            getComic(currentComicNumber)
        } else {
            latestComicNumber = 1
            currentComicNumber = 1
            getComic(1)
            Toast.makeText(this,"New App Set up",Toast.LENGTH_SHORT).show()
        }
    }

    fun savePrefs(){
        val prefs =  getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        var editor = prefs.edit()
        editor.putInt("latestComicNumber",latestComicNumber)
        editor.putInt("currentComicNumber",currentComicNumber)
        editor.putBoolean("initialized", true);
        editor.commit()
        //Toast.makeText(this,"Preferences Saved",Toast.LENGTH_SHORT).show()
    }

    fun updateComic(number: Int){
        if(File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"/ComicViewer/" + number.toString() + ".json").exists()){
            var comic: Comic = Filehandler(applicationContext).loadJson(number.toString() + ".json")
            displayAlt.setText(comic.altText)
            displayTitle.setText(comic.title)
            d("update_comic",comic.imgPath)
        } else {
            displayAlt.setText(" Comic Not Found")
            displayTitle.setText("Unknown")
        }

        val displayImage = findViewById<ImageView>(R.id.displayImage)
        if(File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"/ComicViewer/" + number.toString() + ".png").exists())
            displayImage.setImageBitmap(BitmapFactory.decodeStream(FileInputStream(File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"/ComicViewer/" + number.toString() + ".png").absolutePath)))
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
