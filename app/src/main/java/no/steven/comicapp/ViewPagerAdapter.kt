package no.steven.comicapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.text.SpannableStringBuilder
import android.util.JsonReader
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
    private val latestComicNumber: Int
) : PagerAdapter() {
    private var layoutInflater : LayoutInflater? = null
    private var showComicNumber = false

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
       return view ===  `object`
    }

    override fun getCount(): Int {
       return latestComicNumber // get size, should be maybe either size of the items in comiclist or the latest number, look into.
    }

    @SuppressLint("InflateParams")
    override fun instantiateItem(container: ViewGroup, position: Int): Any { // merge with update comic.
        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val v = layoutInflater!!.inflate(R.layout.viewpage , null)

        val buttonTitle = v.findViewById(R.id.buttonTitle) as Button

        buttonTitle.setOnClickListener {
            if(showComicNumber){
                showComicNumber = false
                updateGraphics(position,v)
            } else {
                showComicNumber = true
                updateGraphics(position,v)
            }
        }
        updateGraphics(position,v)
        val vp = container as ViewPager
        vp.addView(v , 0)

        return v
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
      val vp = container as ViewPager
        val v = `object` as View
        vp.removeView(v)
    }

    /*
    update display and text
     */
    private fun updateGraphics(number: Int,v:View) {
        val displayImage = v.findViewById<View>(R.id.displayImage) as ImageView
        val displayDescription = v.findViewById(R.id.displayDescription) as TextView
        val dateView = v.findViewById(R.id.dateView) as TextView
        val buttonTitle = v.findViewById(R.id.buttonTitle) as Button

        if (File(downloadLocation, "$number.json").exists()) {
            val comic: Comic = loadJson("$number.json")

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

        if (File(downloadLocation, "$number.png").exists())
            displayImage.setImageBitmap(
                BitmapFactory.decodeStream(
                    FileInputStream(
                        File(
                            downloadLocation,
                            "$number.png"
                        ).absolutePath
                    )
                )
            )
        else
            displayImage.setImageDrawable(R.drawable.ic_launcher_foreground.toDrawable())
        //this.invalidateOptionsMenu() //this is for updating the favourite, drop for now. implement later.
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

}