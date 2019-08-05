package com.example.comicviewer

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
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.graphics.drawable.toDrawable

// https://github.com/shortcut/android-coding-assignment - task
// https://xkcd.com/json.html - format
// https://xkcd.com/614/info.0.json - specific comic
// https://xkcd.com/info.0.json - current
// https://xkcd.com/2165/ - actual comic
// https://www.youtube.com/watch?v=FBQnMfSyD98&list=PLt72zDbwBnAV-5HxCmVTP80iKNJ88e10b - tutorial series for backup
// http://www.gadgetsaint.com/android/download-manager/ - download manager code
class MainActivity : AppCompatActivity() {

    private var latestComicNumber = 2167
    private var lastViewedComic = 2166
    private var currentComicNumber = 2166

    private var comiclist = mutableListOf<Int>()
    private var refidlist: ArrayList<Long> = ArrayList()

    var SHARED_PREFS = "Stevens Comic Viewer"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        registerReceiver(onComplete,  IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        //Setup
        saveForSetup()
        loadPrefs()

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

            /*
            check if image or json file.

            if json:

            do loadJson to tempComic
            save tempComic as txt
            do updateComic(tempComic)

            else if image:
            load image and update the screen if current.

            else:
            updateComic(getComic(currentComicNumber))

             */

            updateComic(getComic(currentComicNumber))
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

    fun getComic(toGet: Int): Comic {
        // TODO:
        //  check if comic exist locally,
        //  if yes, fetch,
        //  if not, download

        // saving and loading internally
        // https://www.youtube.com/watch?v=EcfUkjlL9RI

        // convert to list checker, if in list attempt to load, if found, then send comic further along. if download accurs then add to list.

        Log.d("getComic", "comic number: " + toGet.toString())
        Log.d("getComic", "list content: " + comiclist.toString())

        if (toGet in comiclist){
            currentComicNumber = toGet
            lastViewedComic = currentComicNumber
            return Filehandler(applicationContext).loadComic(toGet.toString()+".txt")
        } else {
            val refid = Filehandler(applicationContext).downloadComic("https://xkcd.com/" + toGet.toString() +"/info.0.json",toGet.toString() + ".json","txt")
            refidlist.add(refid);
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
        d("update_comic",comic.imgPath)
        displayAlt.setText(comic.altText)
        displayTitle.setText(comic.title)

        d("update_comic",comic.imgPath)
        val displayImage = findViewById<ImageView>(R.id.displayImage)

        if (comic.imgPath.trim().length != 0){
                displayImage.setImageBitmap(BitmapFactory.decodeStream(FileInputStream(File(comic.imgPath.trim()))))
        } else {
            displayImage.setImageDrawable(R.drawable.ic_launcher_foreground.toDrawable())
        }
    }

    @JvmOverloads fun generatePath(latest: Boolean, number: Int): String {
        if(latest == true){
            return "https://xkcd.com/info.0.json"
        }else{
            return "https://xkcd.com/" + number + "/info.0.json"
        }
    }

    fun saveForSetup(){
        savePrefs()
        var path = ""

        val millennials = BitmapFactory.decodeResource(this.getResources(), R.drawable.millennials)
        path = Filehandler(applicationContext).saveImage(millennials,"2165.jpg")
        Filehandler(applicationContext).saveComic(Comic(2165, "Millennials", "Ironically, I've been having these same arguments for at least a decade now. I thought we would have moved on by now, but somehow the snide complaints about millennials continue.","https://imgs.xkcd.com/comics/millennials.png", path),"2165.txt")
        comiclist.add(2165)

        val stack = BitmapFactory.decodeResource(this.getResources(), R.drawable.stack)
        path = Filehandler(applicationContext).saveImage(stack,"2166.jpg")
        Filehandler(applicationContext).saveComic(Comic(2166, "Stack","Gotta feel kind of bad for nation-state hackers who spend years implanting and cultivating some hardware exploit, only to discover the entire target database is already exposed to anyone with a web browser.","https://imgs.xkcd.com/comics/stack.png",path),"2166.txt")
        comiclist.add(2166)

        val motivated_reasoning_olympics = BitmapFactory.decodeResource(this.getResources(), R.drawable.motivated_reasoning_olympics)
        path = Filehandler(applicationContext).saveImage(motivated_reasoning_olympics,"2167.jpg")
        Filehandler(applicationContext).saveComic(Comic(2167, "Motivated Reasoning Olympics", "[later] I can't believe how bad corruption has become, especially given that our league split off from the statewide one a month ago SPECIFICALLY to protest this kind of flagrantly biased judging.","https://imgs.xkcd.com/comics/motivated_reasoning_olympics.png", path),"2167.txt")
        comiclist.add(2167)
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
