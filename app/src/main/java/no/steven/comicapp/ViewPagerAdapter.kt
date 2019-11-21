package no.steven.comicapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.text.SpannableStringBuilder
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

    private var layoutInflater : LayoutInflater? = null
    private var showComicNumber = false
    private lateinit var currentView: View


    fun getCurrentView(): View {
        return currentView
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
       return view ===  `object`
    }

    override fun getCount(): Int {
       return latestComicNumber // get size, should be maybe either size of the items in comic list or the latest number, look into.
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
        if (legalNumber(number,firstComicNumber,latestComicNumber)) {
            val result = getComic(number,comicList,downloadLocation,downloadRefIdList,jsonRefIdList,imgRefIdList,context,firstComicNumber,latestComicNumber)
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
    @SuppressLint("SetTextI18n")
    private fun updateGraphics(position: Int, v:View): View {
        val displayImage = v.findViewById<View>(R.id.photo_view) as ImageView
        val displayDescription = v.findViewById(R.id.displayDescription) as TextView
        val dateView = v.findViewById(R.id.dateView) as TextView
        val buttonTitle = v.findViewById(R.id.buttonTitle) as Button

        if (File(downloadLocation, "$position.json").exists()) {
            val comic: Comic = loadJson("$position.json",downloadLocation)

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

}