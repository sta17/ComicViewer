package no.steven.comicapp

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.text.SpannableStringBuilder
import android.util.JsonReader
import android.util.Log
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.text.bold
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

class ViewPagerAdapter(
    private val context: Context,
    private val resources: Resources,
    private val downloadLocation: File,
    private var latestComicNumber: Int,
    private val firstComicNumber: Int,
    private var comicList: MutableList<Int>,
    private var downloadRefIdList: MutableMap<Long, Int>,
    private var imgRefIdList: MutableMap<Long, Int>,
    private var jsonRefIdList: MutableMap<Long, Int>
) : PagerAdapter() {

    private var layoutInflater : LayoutInflater? = null;
    private var showComicNumber = false;
    private lateinit var currentView: View


    fun getCurrentView(): View {
        return currentView;
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
       return view ===  `object`
    }

    override fun getCount(): Int {
       return latestComicNumber // get size, should be maybe either size of the items in comiclist or the latest number, look into.
    }

    @SuppressLint("InflateParams")
    override fun instantiateItem(container: ViewGroup, position: Int): Any { // merge with update comic.
        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var v = layoutInflater!!.inflate(R.layout.viewpage , null)
        currentView = v

        val buttonTitle = v.findViewById(R.id.buttonTitle) as Button

        buttonTitle.setOnClickListener {
            if(showComicNumber){
                showComicNumber = false
                this.notifyDataSetChanged()
            } else {
                showComicNumber = true
                this.notifyDataSetChanged()
            }
        }
        v = updateComic(position,v)
        val vp = container as ViewPager
        vp.addView(v , 0)

        return v
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
      val vp = container as ViewPager
        val v = `object` as View
        vp.removeView(v)
    }

    private fun updateComic(number: Int, v: View): View? {
        if (legalNumber(number)) {
            val result = getComic(number)
            if (result) {
                Log.d("updateComic", "updating number: $number")
                updateGraphics(number,v)
                return v
            }
        }
        Log.d("updateComic", "number: $number")
        Log.d("updateComic", "comicList: $comicList")
        Log.d("updateComic", "latest: $latestComicNumber")
        return v
    }

    /*
    update display and text
     */
    private fun updateGraphics(position: Int, v:View): View {
        val displayImage = v.findViewById<View>(R.id.photo_view) as ImageView
        val displayDescription = v.findViewById(R.id.displayDescription) as TextView
        val dateView = v.findViewById(R.id.dateView) as TextView
        val buttonTitle = v.findViewById(R.id.buttonTitle) as Button

        if (File(downloadLocation, "$position.json").exists()) {
            val comic: Comic = loadJson("$position.json")

            displayDescription.text = comic.altText
            buttonTitle.text = comic.title
            if(showComicNumber){
                buttonTitle.text = "${comic.number}: ${comic.title}"
            } else {
                buttonTitle.text = comic.title
            }

            val s = SpannableStringBuilder().bold { append(resources.getString(R.string.date)) }.append(" ${comic.day}.${comic.month}.${comic.year}" )
            dateView.text = s
        } else {
            displayDescription.text = resources.getString(R.string.comic_not_found)
            buttonTitle.text = resources.getString(R.string.unknown)
            dateView.text = ""
        }

        if (File(downloadLocation, "$position.png").exists())
            displayImage.setImageBitmap(
                BitmapFactory.decodeStream(
                    FileInputStream(
                        File(
                            downloadLocation,
                            "$position.png"
                        ).absolutePath
                    )
                )
            )
        else {
            displayImage.setImageDrawable(R.drawable.ic_launcher_foreground.toDrawable())
        }
        //this.invalidateOptionsMenu() //this is for updating the favourite, drop for now. implement later.
        return v
    }

    /*
    Get the wanted Json file
     */
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
                            false -> downloadImage(toGet)
                            true -> comicList.add(toGet)
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

    private fun download(url: String, filename: String, fileType: String): Long {
        val downloadManagerVar = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(url)
        val request = DownloadManager.Request(downloadUri)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        request.setAllowedOverRoaming(false)
        request.setTitle(resources.getString(R.string.comic_viewer_downloading_comic) + fileType)
        request.setDescription(resources.getString(R.string.comic_viewer_downloading_comic) + fileType)
        request.setDestinationInExternalFilesDir(
            context,
            Environment.DIRECTORY_DOWNLOADS,
            filename
        )
        return downloadManagerVar.enqueue(request)
    }


    private fun legalNumber(number: Int): Boolean {
        return when ((firstComicNumber <= number) && (number <= latestComicNumber)) {
            true -> true
            false -> false
        }
    }

}