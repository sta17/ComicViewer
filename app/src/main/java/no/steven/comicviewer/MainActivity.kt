package no.steven.comicviewer

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.os.Bundle
import android.util.Log.d
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import android.graphics.BitmapFactory
import android.widget.ImageView
import java.io.File
import java.io.FileInputStream
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.core.graphics.drawable.toDrawable
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.widget.EditText
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
    private var favouritecomiclist = mutableListOf<Int>()
    private var refidlist: ArrayList<Long> = ArrayList()

    private val address = "/ComicViewer/"

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

        //Setup
        loadPrefs()

        buttonFirst.setOnClickListener {
            val result = getComic(1)
            if (result) {
                currentComicNumber = 1
            }
        }

        buttonLast.setOnClickListener {
            val result = getComic(0)
            if (result) {
                currentComicNumber = latestComicNumber
            }
        }

        buttonfavourte.setOnClickListener {
            if ((currentComicNumber !in favouritecomiclist)) {
                favouritecomiclist.add(currentComicNumber)
            } else if (currentComicNumber in favouritecomiclist) {
                favouritecomiclist.remove(currentComicNumber)
            }
        }

        buttonUpdate.setOnClickListener {
            val changeto = findViewById<EditText>(R.id.ComicNumberField).text.toString().toInt()
            findViewById<EditText>(R.id.ComicNumberField).text.clear()
            val result = getComic(changeto)
            if (result) {
                currentComicNumber = changeto
            }
        }

        buttonNext.setOnClickListener {
            if (legalnumber(currentComicNumber + 1)) {
                val result = getComic(currentComicNumber + 1)
                if (result) {
                    currentComicNumber += 1
                }
            }
        }

        buttonPrevious.setOnClickListener {
            if (legalnumber(currentComicNumber - 1)) {
                val result = getComic(currentComicNumber - 1)
                if (result) {
                    currentComicNumber -= 1
                }
            }
        }

    }

    var onComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            // get the refid from the download manager
            val referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            refidlist.remove(referenceId)

            // check if download is json download
            if (jsonRefidlist.contains(referenceId)) {
                var toGet = jsonRefidlist[referenceId]
                jsonRefidlist.remove(referenceId)
                lateinit var downloadedComic: Comic

                // check if latest comic
                if (toGet == 0) {
                    d("onComplete", "latest comic json aquired")
                    downloadedComic =
                        Filehandler(applicationContext, address).loadJson("latest.json")
                    toGet = downloadedComic.number
                    latestComicNumber = downloadedComic.number

                    val from = File(
                        applicationContext.getExternalFilesDir(DIRECTORY_DOWNLOADS),
                        "/ComicViewer/latest.json"
                    )
                    val to = File(
                        applicationContext.getExternalFilesDir(DIRECTORY_DOWNLOADS),
                        "/ComicViewer/$toGet.json"
                    )

                    //if latest comic number exist, rename to proper comic if not
                    if ((from.exists()) && (!to.exists())) {
                        from.renameTo(to)

                        // if latest exist already, stop download set current to latest, add to list if not already,
                    } else {
                        latestComicNumber = toGet
                        currentComicNumber = toGet
                        if ((toGet !in comiclist)) {
                            comiclist.add(toGet)
                        }
                        d("onComplete", "comic number: $toGet")
                        d("onComplete", "list content: $comiclist")
                        updateComic(toGet)
                        return
                    }

                    // if not latest that exists already, load comic and proceed
                } else {
                    d("onComplete", "comic json aquired: " + toGet.toString())
                    downloadedComic = Filehandler(
                        applicationContext,
                        address
                    ).loadJson(toGet.toString() + ".json")
                }

                val imageUrl = downloadedComic.imgUrl

                d("onComplete", "img path provided: " + downloadedComic.imgPath)
                d("onComplete", "img url provided: " + downloadedComic.imgUrl)

                // if image not exist, download
                if (!File(
                        applicationContext.getExternalFilesDir(DIRECTORY_DOWNLOADS),
                        "$address$toGet.png"
                    ).exists()
                ) {
                    val refidIMG = Filehandler(applicationContext, address).download(
                        imageUrl,
                        "$toGet.png",
                        "png"
                    )
                    refidlist.add(refidIMG)
                    if (toGet != null) {
                        imgRefidlist[refidIMG] = toGet
                    }
                }

                // if image, check if image download
            } else if (imgRefidlist.contains(referenceId)) {
                var toGet = imgRefidlist[referenceId]
                if (toGet != null) {
                    toGet = toGet.toInt()
                }
                imgRefidlist.remove(referenceId)
                d("getComic", "comic image aquired: $toGet")

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
        Filehandler(applicationContext, address).savelist(comiclist, "comiclist.json")
        Filehandler(applicationContext, address).savelist(
            favouritecomiclist,
            "favouritecomiclist.json"
        )
    }

    override fun onStop() {
        super.onStop()
        savePrefs()
        Filehandler(applicationContext, address).savelist(comiclist, "comiclist.json")
        Filehandler(applicationContext, address).savelist(
            favouritecomiclist,
            "favouritecomiclist.json"
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        d("permission", "onRequestPermissionsResult started")
        when (requestCode) {
            myPermissionsWriteExternalStorage -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    d("permission", "write permission granted")
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    d("permission", "write permission denied")
                }
                return
            }
            myPermissionsReadExternalStorage -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    d("permission", "read permission granted")
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    d("permission", "read permission denied")
                }
                return
            }
            myPermissionsInternet -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    d("permission", "internet permission granted")
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    d("permission", "internet permission denied")
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
        d("getComic", "comic number: $toGet")
        d("getComic", "list content: $comiclist")

        // Check if in list already:
        if ((toGet in comiclist) && (toGet != 0)) {
            currentComicNumber = toGet
            d("getComic", "comic in list")
            updateComic(toGet)
            return true
            // if not in list
        } else {
            d("getComic", "comic not in list")

            // get permission
            val permissionCheck =
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    myPermissionsWriteExternalStorage
                )
            }

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.INTERNET),
                    myPermissionsInternet
                )
            }

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    myPermissionsReadExternalStorage
                )
            }

            // if comic to get is 0, then save as latest, since comic count starts at 1
            if (toGet == 0) {
                val latest = File(
                    applicationContext.getExternalFilesDir(DIRECTORY_DOWNLOADS),
                    "/ComicViewer/latest.json"
                )
                if (latest.exists())
                    latest.delete()

                val refidTXT = Filehandler(
                    applicationContext,
                    address
                ).download("https://xkcd.com/info.0.json", "latest.json", "txt")
                refidlist.add(refidTXT)
                jsonRefidlist[refidTXT] = toGet
                d("getComic", "getting latest")
                return false
            } else {
                // if json not exist, then download, which will kickstart image download
                if (!File(
                        applicationContext.getExternalFilesDir(DIRECTORY_DOWNLOADS),
                        "$address$toGet.json"
                    ).exists()
                ) {
                    val refidJson = Filehandler(
                        applicationContext,
                        address
                    ).download("https://xkcd.com/$toGet/info.0.json", "$toGet.json", "txt")
                    refidlist.add(refidJson)
                    jsonRefidlist[refidJson] = toGet
                    d("getComic", "getting comic: $toGet")
                    return false
                    // if image not exist, then download
                } else if (!File(
                        applicationContext.getExternalFilesDir(DIRECTORY_DOWNLOADS),
                        "$address$toGet.png"
                    ).exists()
                ) {
                    val refidIMG = Filehandler(applicationContext, address).download(
                        Filehandler(
                            applicationContext,
                            address
                        ).loadJson("$toGet.json").imgUrl, "$toGet.png", "png"
                    )
                    refidlist.add(refidIMG)
                    imgRefidlist[refidIMG] = toGet
                    d("getComic", "getting image: $toGet")
                    return false
                    // else get comic and text.
                } else {
                    comiclist.add(toGet)
                    d("getComic", "updated comiclist")
                    d("getComic", "list content: $comiclist")
                    updateComic(toGet)
                    return true
                }
            }
        }
    }

    //load preferences and comiclist
    private fun loadPrefs() {
        val prefs = getSharedPreferences(sharedPrefs, Context.MODE_PRIVATE)
        if ((prefs.contains("initialized")) && (prefs.getBoolean("initialized", false))) {
            latestComicNumber = prefs.getInt("latestComicNumber", latestComicNumber)
            currentComicNumber = prefs.getInt("currentComicNumber", currentComicNumber)
            comiclist =
                comiclist.union(Filehandler(applicationContext, address).loadlist("comiclist.json"))
                    .toMutableList()
            favouritecomiclist = favouritecomiclist.union(
                Filehandler(
                    applicationContext,
                    address
                ).loadlist("favouritecomiclist.json")
            ).toMutableList()
            Toast.makeText(
                this,
                resources.getString(R.string.preferences_loaded),
                Toast.LENGTH_SHORT
            ).show()
            getComic(currentComicNumber)
        } else {
            latestComicNumber = 1
            currentComicNumber = 1
            getComic(1)
            Toast.makeText(this, resources.getString(R.string.new_app_set_up), Toast.LENGTH_SHORT)
                .show()
        }
    }

    //save preferences and comiclist
    private fun savePrefs() {
        val prefs = getSharedPreferences(sharedPrefs, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt("latestComicNumber", latestComicNumber)
        editor.putInt("currentComicNumber", currentComicNumber)
        editor.putBoolean("initialized", true)
        editor.apply()
        //Toast.makeText(this,"Preferences Saved",Toast.LENGTH_SHORT).show()
    }


    //update display and text
    private fun updateComic(number: Int) {
        if (File(getExternalFilesDir(DIRECTORY_DOWNLOADS), "$address$number.json").exists()) {
            val comic: Comic = Filehandler(
                applicationContext,
                address
            ).loadJson("$number.json")
            displayAlt.text = comic.altText
            displayTitle.text = comic.title
            d("update_comic", comic.imgPath)
        } else {
            displayAlt.text = resources.getString(R.string.comic_not_found)
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
        if ((1 <= number) && (number >= latestComicNumber)) return true
        else return false
    }
}