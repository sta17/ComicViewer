package no.steven.comicapp

import android.Manifest
import androidx.appcompat.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.text.InputType.TYPE_CLASS_NUMBER
import android.text.SpannableStringBuilder
import android.util.JsonReader
import android.util.Log.d
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.text.bold
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.content_main_alt.*
import java.io.*

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

    private val myPermissionsWriteExternalStorage = 1
    private val myPermissionsReadExternalStorage = 1
    private val myPermissionsInternet = 1

    private var sharedPrefs = "Steven's a Comic App"

    private lateinit var downloadLocation: File

    internal lateinit var viewpager : ViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        downloadLocation = this.getExternalFilesDir(DIRECTORY_DOWNLOADS)!!

        viewpager = findViewById(R.id.viewpager)
        val adapter = ViewPagerAdapter(this,resources,downloadLocation,latestComicNumber)
        viewpager.adapter = adapter

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
        updateComic(currentComicNumber) // get the last viewed

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
                            + System.getProperty("line.separator") + System.getProperty("line.separator") + "App Icon made using romannurik.github.io/AndroidAssetStudio/"
                            + System.getProperty("line.separator") + System.getProperty("line.separator") + "App by Steven Aanetsen"
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
            loadJson("latest.json")
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
                "png"
            )
            downloadRefIdList[refIdImg] = toGet
            imgRefIdList[refIdImg] = toGet
        }
        latestComicNumber = toGet
        return true
    }

    private fun handleJson(toGet: Int, referenceId: Long): Boolean {
        val downloadedComic = loadJson("$toGet.json")

        val imageUrl = downloadedComic.imgUrl

        if (!File(downloadLocation, "$toGet.png").exists()) {
            val refIdImg = download(
                imageUrl,
                "$toGet.png",
                "png"
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
        // Check if in list already:
        return when ((toGet in comicList) && legalNumber(toGet)) {
            true -> true
            false -> return when (toGet == 0 || toGet == -1) {
                true -> downloadLatest(toGet)
                false -> return when ((toGet in comicList) && legalNumber(toGet)) {
                    true -> true
                    false -> return when (!File(downloadLocation, "$toGet.json").exists()) {
                        true -> downloadJson(toGet)
                        false -> return when (!File(
                            downloadLocation,
                            "$toGet.png"
                        ).exists()) {
                            true -> downloadImage(toGet)
                            false -> comicList.add(toGet)
                        }
                    }
                }
            }
        }
    }

    private fun downloadLatest(toGet: Int): Boolean {
        val latest = File(
            downloadLocation,
            "latest.json"
        )
        latest.absoluteFile.delete()
        if (latest.exists()){
            latest.absoluteFile.delete()
        }

        val refIdJson = download("https://xkcd.com/info.0.json", "latest.json", "txt")

        downloadRefIdList[refIdJson] = toGet
        jsonRefIdList[refIdJson] = toGet

        return false
    }

    private fun downloadJson(toGet: Int): Boolean {
        val refIdJson = download("https://xkcd.com/$toGet/info.0.json", "$toGet.json", "txt")

        downloadRefIdList[refIdJson] = toGet
        jsonRefIdList[refIdJson] = toGet
        return false
    }

    private fun downloadImage(toGet: Int): Boolean {
        val refIdImg = download(
            loadJson("$toGet.json").imgUrl, "$toGet.png", "png"
        )

        downloadRefIdList[refIdImg] = toGet
        imgRefIdList[refIdImg] = toGet
        return false
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
                comicList = comicList.union(loadList("comicList.json")).toMutableList()
            }else{
                Toast.makeText(
                    applicationContext,
                    "Comic list not found. No Comics in list.",
                    Toast.LENGTH_LONG
                ).show()
                d("error","ComicList file was not found.")
            }

            if(File(downloadLocation, "favouriteComicList.json").exists()){
                favouriteComicList = favouriteComicList.union(loadList("favouriteComicList.json")).toMutableList()
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
        saveList(comicList, "comicList.json")
        saveList(
            favouriteComicList,
            "favouriteComicList.json"
        )
    }

    private fun updateComic(number: Int) {
        if (legalNumber(number)) {
            val result = getComic(number)
            if (result) {
                d("updateComic", "updating number: $number")
                currentComicNumber = number
                viewpager.currentItem = currentComicNumber
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

    /*
    check if the number is to high or low
    returns true if legal number,
    false if not
     */
    private fun legalNumber(number: Int): Boolean {
        return when ((firstComicNumber <= number) && (number <= latestComicNumber)) {
            true -> true
            false -> false
        }
    }

    private fun download(url: String, filename: String, fileType: String): Long {
        val downloadManagerVar =
            getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(url)
        val request = DownloadManager.Request(downloadUri)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        request.setAllowedOverRoaming(false)
        request.setTitle(resources.getString(R.string.comic_viewer_downloading_comic) + fileType)
        request.setDescription(resources.getString(R.string.comic_viewer_downloading_comic) + fileType)
        request.setDestinationInExternalFilesDir(
            this,
            DIRECTORY_DOWNLOADS,
            filename
        )
        return downloadManagerVar.enqueue(request)
    }

    @Throws(IOException::class)
    private fun saveList(comicList: List<Int>, filename: String) {
        val text = comicList.toString()

        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(
                File(
                    downloadLocation,
                    filename
                )
            )

            //fos = context.openFileOutput(filename, MODE_PRIVATE)
            fos.write(text.toByteArray())
            fos.close()
        } finally {
            if (fos != null) {
                try {
                    fos.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun loadList(filename: String): MutableList<Int> {
        var fis: FileInputStream? = null

        try {
            fis = FileInputStream(
                File(
                    downloadLocation,
                    filename
                )
            )
            val input = BufferedReader(InputStreamReader(fis)).readText()
            val initialInput = input.removeSurrounding("[", "]")
            if (initialInput.isEmpty()) {
                return mutableListOf()
            }

            return initialInput.split(",").map { it.trim() }.map { it.toInt() }.toMutableList()
        } finally {
            if (fis != null) {
                try {
                    fis.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun loadJson(filename: String): Comic {
        val fis = FileInputStream(
            File(
                downloadLocation,
                filename
            )
        )

        val reader = JsonReader(InputStreamReader(fis, "UTF-8"))
        reader.use {
            var number = -2 // want it to be a number not used, and that is nonsense.
            var title = ""
            var altText = ""
            var urlPath = ""
            var year = -1
            var month = -1
            var day = -1

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName().toString()) {
                    "img" -> urlPath = reader.nextString()
                    "alt" -> altText = reader.nextString()
                    "num" -> number = reader.nextInt()
                    "title" -> title = reader.nextString()
                    "year" -> year = reader.nextInt()
                    "month" -> month = reader.nextInt()
                    "day" -> day = reader.nextInt()
                    else -> reader.skipValue()
                }
            }

            reader.endObject()
            reader.close()
            return Comic(number, title, altText, urlPath, "",year,month,day)

        }
    }
}

