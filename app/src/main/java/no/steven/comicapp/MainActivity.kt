package no.steven.comicapp

import android.Manifest
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.text.InputType.TYPE_CLASS_NUMBER
import android.util.JsonReader
import android.util.Log.d
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import kotlinx.android.synthetic.main.content_main.*
import java.io.*

// https://github.com/shortcut/android-coding-assignment - task.
// https://www.journaldev.com/10096/android-viewpager-example-tutorial - viewpager tutorial.
// https://tutorial.eyehunts.com/android/android-toolbar-example-android-app-bar-kotlin/ - action bar example
// http://actionbarsherlock.com/ - action bar stuff
// https://xkcd.com/
// https://www.flaticon.com/free-icon/transparency_1076744#term=search&page=1&position=5 - Designed by ? from www.Flaticon - Gear
// https://www.flaticon.com/free-icon/transparency_1076744#term=search&page=1&position=5 - Designed by Freepik from www.Flaticon - Magnifying glass.
// https://www.flaticon.com/free-icon/star_149222 - Designed by smashicons from www.Flaticon - Star
// https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html - Comic Icon
// http://romannurik.github.io/AndroidAssetStudio/ - Asset generators
//TODO: make most of the comic pages cleanable as junk except for favourites.
//TODO: set the page update to happen upon comic image download, immediately.
class MainActivity : AppCompatActivity() {

    private var currentComicNumber = 0
    private var latestComicNumber = 0
    private var firstComicNumber = 1

    private var comiclist = mutableListOf<Int>()
    private var favouritecomiclist = mutableListOf<Int>()

    private val address = "/ComicViewer/"

    private var downloadrefidlist: MutableMap<Long, Int> = mutableMapOf()
    private var imgRefidlist: MutableMap<Long, Int> = mutableMapOf()
    private var jsonRefidlist: MutableMap<Long, Int> = mutableMapOf()

    private val myPermissionsWriteExternalStorage = 1
    private val myPermissionsReadExternalStorage = 1
    private val myPermissionsInternet = 1

    private var sharedPrefs = "Stevens Comic Viewer"

    private lateinit var downloadLocation: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //setSupportActionBar(toolbar)
        //setting toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        //home navigation
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        downloadLocation = this.getExternalFilesDir(DIRECTORY_DOWNLOADS)!!

        // get permission
        val permissionCheckWriteExternalStorage =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permissionCheckWriteExternalStorage != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                myPermissionsWriteExternalStorage
            )
        }

        val permissionCheckInternet =
            ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
        if (permissionCheckInternet != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.INTERNET),
                myPermissionsInternet
            )
        }

        val permissionCheckReadExternalStorage =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (permissionCheckReadExternalStorage != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                myPermissionsReadExternalStorage
            )
        }

        //Setup
        loadPrefs() // get the preferences
        getComic(-1) // get latest
        downloadFavourites(favouritecomiclist) // get favouties
        updateComic(currentComicNumber) // get the last viewed

        buttonFirst.setOnClickListener {
            updateComic(1)
        }

        buttonLast.setOnClickListener {
            updateComic(latestComicNumber)
        }

        buttonNext.setOnClickListener {
            updateComic(currentComicNumber + 1)
        }

        buttonPrevious.setOnClickListener {
            updateComic(currentComicNumber - 1)
        }

    }

    //setting menu in action bar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (currentComicNumber in favouritecomiclist) {
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
            if (currentComicNumber in favouritecomiclist) {
                favouritecomiclist.remove(currentComicNumber)
                Toast.makeText(applicationContext, "Favourite Removed", Toast.LENGTH_LONG).show()
            } else {
                favouritecomiclist.add(currentComicNumber)
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
            // get the refid from the download manager
            val referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            // check if download is json download
            if (jsonRefidlist.contains(referenceId)) {
                val toGet: Int = jsonRefidlist[referenceId]!!
                jsonRefidlist.remove(referenceId)

                // check if latest comic
                if (toGet == 0 || toGet == -1) {
                    handleLatest(toGet)
                } else {
                    handleJson(toGet, referenceId)
                }
            } else if (imgRefidlist.contains(referenceId)) {
                handleImage(referenceId)
            }
        }
    }

    private fun handleLatest(state: Int): Boolean {
        val downloadedComic =
            loadJson("latest.json")
        val toGet = downloadedComic.number
        latestComicNumber = downloadedComic.number

        val notRenamed = File(
            downloadLocation,
            "/ComicViewer/latest.json"
        )
        val renameTo = File(
            downloadLocation,
            "/ComicViewer/$toGet.json"
        )

        if ((notRenamed.exists()) && (!renameTo.exists()) && (toGet !in comiclist)) {
            notRenamed.renameTo(renameTo)
        }

        if ((state == 0) && (!File(downloadLocation, "$address$toGet.png").exists())) {
            val imageUrl = downloadedComic.imgUrl
            val refidIMG = download(
                imageUrl,
                "$toGet.png",
                "png"
            )
            downloadrefidlist[refidIMG] = toGet
            imgRefidlist[refidIMG] = toGet
        }
        latestComicNumber = toGet
        return true
    }

    private fun handleJson(toGet: Int, referenceId: Long): Boolean {
        val downloadedComic = loadJson("$toGet.json")

        val imageUrl = downloadedComic.imgUrl

        if (!File(downloadLocation, "$address$toGet.png").exists()) {
            val refidIMG = download(
                imageUrl,
                "$toGet.png",
                "png"
            )
            downloadrefidlist.remove(referenceId)
            downloadrefidlist[refidIMG] = toGet
            imgRefidlist[refidIMG] = toGet
        }
        return true
    }

    private fun handleImage(referenceId: Long): Boolean {
        val toGet: Int = imgRefidlist[referenceId]!!

        imgRefidlist.remove(referenceId)
        downloadrefidlist.remove(referenceId)
        comiclist.add(toGet)

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


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            myPermissionsWriteExternalStorage -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }
            myPermissionsReadExternalStorage -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }
            myPermissionsInternet -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
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

    private fun getComic(toGet: Int): Boolean {
        // Check if in list already:
        return when ((toGet in comiclist) && legalnumber(toGet)) {
            true -> true
            false -> return when (toGet == 0 || toGet == -1) {
                true -> downloadLatest()
                false -> return when ((toGet in comiclist) && legalnumber(toGet)) {
                    true -> true
                    false -> return when (!File(downloadLocation, "$address$toGet.json").exists()) {
                        true -> downloadJson(toGet)
                        false -> return when (!File(
                            downloadLocation,
                            "$address$toGet.png"
                        ).exists()) {
                            true -> downloadImage(toGet)
                            false -> comiclist.add(toGet)
                        }
                    }
                }
            }
        }
    }

    private fun downloadLatest(): Boolean {
        val latest = File(
            downloadLocation,
            "/ComicViewer/latest.json"
        )
        if (latest.exists())
            latest.delete()

        val refidJson = download("https://xkcd.com/info.0.json", "latest.json", "txt")

        downloadrefidlist[refidJson] = 0
        jsonRefidlist[refidJson] = 0
        return false
    }

    private fun downloadJson(toGet: Int): Boolean {
        val refidJson = download("https://xkcd.com/$toGet/info.0.json", "$toGet.json", "txt")

        downloadrefidlist[refidJson] = toGet
        jsonRefidlist[refidJson] = toGet
        return false
    }

    private fun downloadImage(toGet: Int): Boolean {
        val refidIMG = download(
            loadJson("$toGet.json").imgUrl, "$toGet.png", "png"
        )

        downloadrefidlist[refidIMG] = toGet
        imgRefidlist[refidIMG] = toGet
        return false
    }

    private fun downloadFavourites(favouritecomiclist: MutableList<Int>) {
        if (favouritecomiclist.isNotEmpty()) {
            val iterator = favouritecomiclist.listIterator()
            for (comicNumber in iterator) {
                getComic(comicNumber)
            }
        }
    }

    /*
    load preferences, comiclist and favourites
     */
    private fun loadPrefs() {
        val prefs = getSharedPreferences(sharedPrefs, Context.MODE_PRIVATE)
        if ((prefs.contains("initialized")) && (prefs.getBoolean("initialized", false))) {
            latestComicNumber = prefs.getInt("latestComicNumber", latestComicNumber)
            currentComicNumber = prefs.getInt("currentComicNumber", currentComicNumber)
            comiclist = comiclist.union(loadlist("comiclist.json")).toMutableList()
            favouritecomiclist =
                favouritecomiclist.union(loadlist("favouritecomiclist.json")).toMutableList()
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
    save preferences, comiclist and favourites
     */
    private fun saveState() {
        val prefs = getSharedPreferences(sharedPrefs, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt("latestComicNumber", latestComicNumber)
        editor.putInt("currentComicNumber", currentComicNumber)
        editor.putBoolean("initialized", true)
        editor.apply()
        Toast.makeText(applicationContext, "Preferences Saved", Toast.LENGTH_SHORT).show()
        savelist(comiclist, "comiclist.json")
        savelist(
            favouritecomiclist,
            "favouritecomiclist.json"
        )
    }

    private fun updateComic(number: Int) {
        if (legalnumber(number)) {
            val result = getComic(number)
            if (result) {
                d("updateComic", "updating number: $number")
                currentComicNumber = number
                updateGraphics(currentComicNumber)
            }
            //else if (!result){
            //while (number in imgRefidlist.values){}
            //    currentComicNumber = waitingToUpdateTo
            //    updateGraphics(currentComicNumber)
            //}
        }
        d("updateComic", "number: $number")
        d("updateComic", "comiclist: $comiclist")
        d("updateComic", "favouritecomiclist: $favouritecomiclist")
        d("updateComic", "latest: $latestComicNumber")
    }

    /*
    update display and text
     */
    private fun updateGraphics(number: Int) {
        if (File(downloadLocation, "$address$number.json").exists()) {
            val comic: Comic = loadJson("$number.json")
            displayDescription.text = comic.altText
            displayTitle.text = comic.title
        } else {
            displayDescription.text = resources.getString(R.string.comic_not_found)
            displayTitle.text = resources.getString(R.string.unknown)
        }

        val displayImage = findViewById<ImageView>(R.id.displayImage)
        if (File(downloadLocation, "$address$number.png").exists())
            displayImage.setImageBitmap(
                BitmapFactory.decodeStream(
                    FileInputStream(
                        File(
                            downloadLocation,
                            "$address$number.png"
                        ).absolutePath
                    )
                )
            )
        else
            displayImage.setImageDrawable(R.drawable.ic_launcher_foreground.toDrawable())
        this.invalidateOptionsMenu()
    }

    /*
    check if the number is to high or low
    returns true if legal number,
    false if not
     */
    private fun legalnumber(number: Int): Boolean {
        return when ((firstComicNumber <= number) && (number <= latestComicNumber)) {
            true -> true
            false -> false
        }
    }

    private fun download(url: String, filename: String, filetype: String): Long {
        val downloadManagervar =
            getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(url)
        val request = DownloadManager.Request(downloadUri)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        request.setAllowedOverRoaming(false)
        request.setTitle(resources.getString(R.string.comic_viewer_downloading_comic) + filetype)
        request.setDescription(resources.getString(R.string.comic_viewer_downloading_comic) + filetype)
        request.setDestinationInExternalFilesDir(
            this,
            DIRECTORY_DOWNLOADS,
            address + filename
        )
        return downloadManagervar.enqueue(request)
    }

    @Throws(IOException::class)
    private fun savelist(comiclist: List<Int>, filename: String) {
        val text = comiclist.toString()

        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(
                File(
                    downloadLocation,
                    address + filename
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
    private fun loadlist(filename: String): MutableList<Int> {
        var fis: FileInputStream? = null

        try {
            fis = FileInputStream(
                File(
                    downloadLocation,
                    address + filename
                )
            )
            val input = BufferedReader(InputStreamReader(fis)).readText()
            val inititalInput = input.removeSurrounding("[", "]")
            if (inititalInput.isEmpty()) {
                return mutableListOf()
            }

            return inititalInput.split(",").map { it.trim() }.map { it.toInt() }.toMutableList()
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
                address + filename
            )
        )

        val reader = JsonReader(InputStreamReader(fis, "UTF-8"))
        reader.use {
            var number = -2 // want it to be a number not used, and that is nonsense.
            var title = ""
            var altText = ""
            var urlPath = ""

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName().toString()) {
                    "img" -> urlPath = reader.nextString()
                    "alt" -> altText = reader.nextString()
                    "num" -> number = reader.nextInt()
                    "title" -> title = reader.nextString()
                    else -> reader.skipValue()
                }
            }

            reader.endObject()
            reader.close()
            return Comic(number, title, altText, urlPath, "")

        }
    }

}