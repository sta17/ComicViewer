package no.steven.comicviewer

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import android.graphics.BitmapFactory
import android.widget.ImageView
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.core.graphics.drawable.toDrawable
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.util.JsonReader
import android.util.Log.d
import android.widget.EditText
import androidx.core.content.ContextCompat
import java.io.*


// https://github.com/shortcut/android-coding-assignment - task.
// https://www.journaldev.com/10096/android-viewpager-example-tutorial - viewpager tutorial.
//TODO: Fix the latest comic thing, the need to check for latest number and to update it.
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

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

        buttonfavourte.setOnClickListener {
            if ((currentComicNumber !in favouritecomiclist)) {
                favouritecomiclist.add(currentComicNumber)
            } else if (currentComicNumber in favouritecomiclist) {
                favouritecomiclist.remove(currentComicNumber)
            }
        }

        buttonUpdate.setOnClickListener {
            val changeTo = findViewById<EditText>(R.id.ComicNumberField).text.toString().toInt()
            findViewById<EditText>(R.id.ComicNumberField).text.clear()
            updateComic(changeTo)
        }

        buttonNext.setOnClickListener {
            updateComic(currentComicNumber + 1)
        }

        buttonPrevious.setOnClickListener {
            updateComic(currentComicNumber - 1)
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
                    handleJson(toGet,referenceId)
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
            applicationContext.getExternalFilesDir(DIRECTORY_DOWNLOADS),
            "/ComicViewer/latest.json"
        )
        val renameTo = File(
            applicationContext.getExternalFilesDir(DIRECTORY_DOWNLOADS),
            "/ComicViewer/$toGet.json"
        )

        if ((notRenamed.exists()) && (!renameTo.exists()) && (toGet !in comiclist)) {
            notRenamed.renameTo(renameTo)
        }

        if(state == 0){
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

    private fun handleJson(toGet: Int,referenceId:Long): Boolean {
        val downloadedComic = loadJson("$toGet.json")

        val imageUrl = downloadedComic.imgUrl

        val refidIMG = download(
            imageUrl,
            "$toGet.png",
            "png"
        )
        downloadrefidlist.remove(referenceId)
        downloadrefidlist[refidIMG] = toGet
        imgRefidlist[refidIMG] = toGet

        return true
    }

    private fun handleImage(referenceId:Long): Boolean {
        val toGet:Int = imgRefidlist[referenceId]!!

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
        if ((toGet in comiclist) && legalnumber(toGet)) {
            return true
            // if not in list
        } else {
            // if comic to get is 0, then save as latest, since comic count starts at 1
            if (toGet == 0 || toGet == -1) {
                downloadLatest()
                return false
            } else {
                // if json not exist, then download, which will kickstart image download
                if (!File(applicationContext.getExternalFilesDir(DIRECTORY_DOWNLOADS), "$address$toGet.json").exists()) {
                    downloadJson(toGet)
                    return false
                } else if (!File(applicationContext.getExternalFilesDir(DIRECTORY_DOWNLOADS), "$address$toGet.png").exists()) {
                    downloadImage(toGet)
                    return false
                } else {
                    comiclist.add(toGet)
                    return true
                }
            }
        }
    }

    private fun downloadLatest(){
        val latest = File(
            applicationContext.getExternalFilesDir(DIRECTORY_DOWNLOADS),
            "/ComicViewer/latest.json"
        )
        if (latest.exists())
            latest.delete()

        val refidJson = download("https://xkcd.com/info.0.json", "latest.json", "txt")

        downloadrefidlist[refidJson] = 0
        jsonRefidlist[refidJson] = 0
    }

    private fun downloadJson(toGet:Int){
        val refidJson = download("https://xkcd.com/$toGet/info.0.json", "$toGet.json", "txt")

        downloadrefidlist[refidJson] = toGet
        jsonRefidlist[refidJson] = toGet
    }

    private fun downloadImage(toGet:Int){
        val refidIMG = download(
            loadJson("$toGet.json").imgUrl, "$toGet.png", "png"
        )

        downloadrefidlist[refidIMG] = toGet
        imgRefidlist[refidIMG] = toGet
    }

    private fun downloadFavourites(favouritecomiclist: MutableList<Int>){
        if(favouritecomiclist.isNotEmpty()){
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
            favouritecomiclist = favouritecomiclist.union(loadlist("favouritecomiclist.json")).toMutableList()
            Toast.makeText(this, resources.getString(R.string.preferences_loaded), Toast.LENGTH_LONG).show()
        } else {
            latestComicNumber = 1
            currentComicNumber = 1
            Toast.makeText(this, resources.getString(R.string.new_app_set_up), Toast.LENGTH_LONG).show()
        }
    }

    /*
    save preferences, comiclist and favourites
     */
    private fun saveState(){
        val prefs = getSharedPreferences(sharedPrefs, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt("latestComicNumber", latestComicNumber)
        editor.putInt("currentComicNumber", currentComicNumber)
        editor.putBoolean("initialized", true)
        editor.apply()
        //Toast.makeText(this,"Preferences Saved",Toast.LENGTH_SHORT).show()
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
                d("updateComic","updating number: $number")
                currentComicNumber = number
                updateGraphics(currentComicNumber)
            }
            //else if (!result){
            //while (number in imgRefidlist.values){}
            //    currentComicNumber = waitingToUpdateTo
            //    updateGraphics(currentComicNumber)
            //}
        }
        d("updateComic","number: $number")
        d("updateComic","comiclist: $comiclist")
        d("updateComic","latest: $latestComicNumber")
    }

    /*
    update display and text
     */
    private fun updateGraphics(number: Int) {
        if (File(getExternalFilesDir(DIRECTORY_DOWNLOADS), "$address$number.json").exists()) {
            val comic: Comic = loadJson("$number.json")
            displayDescription.text = comic.altText
            displayTitle.text = comic.title
        } else {
            displayDescription.text = resources.getString(R.string.comic_not_found)
            displayTitle.text = resources.getString(R.string.unknown)
        }

        val displayImage = findViewById<ImageView>(R.id.displayImage)
        if (File(getExternalFilesDir(DIRECTORY_DOWNLOADS), "$address$number.png").exists())
            displayImage.setImageBitmap(
                BitmapFactory.decodeStream(
                    FileInputStream(
                        File(
                            getExternalFilesDir(DIRECTORY_DOWNLOADS),
                            "$address$number.png"
                        ).absolutePath
                    )
                )
            )
        else
            displayImage.setImageDrawable(R.drawable.ic_launcher_foreground.toDrawable())
    }

    /*
    check if the number is to high or low
    returns true if legal number,
    false if not
     */
    private fun legalnumber(number: Int): Boolean {
        if ((firstComicNumber <= number) && (number <= latestComicNumber)) return true
        else return false
    }

    fun download(url: String, filename: String, filetype: String): Long {
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
    fun savelist(comiclist: List<Int>, filename: String) {
        val text = comiclist.toString()

        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(
                File(
                    getExternalFilesDir(DIRECTORY_DOWNLOADS),
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
    fun loadlist(filename: String): MutableList<Int> {
        var fis: FileInputStream? = null

        try {
            fis = FileInputStream(
                File(
                    getExternalFilesDir(DIRECTORY_DOWNLOADS),
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
    fun loadJson(filename: String): Comic {
        val fis = FileInputStream(
            File(
                getExternalFilesDir(DIRECTORY_DOWNLOADS),
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