package no.steven.comicapp.fragment_handling

import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.text.bold
import androidx.fragment.app.Fragment
import no.steven.comicapp.Comic
import no.steven.comicapp.R
import no.steven.comicapp.loadJson
import java.io.File
import java.io.FileInputStream

class UpdatableFragment(private val downloadLocation: File) : Fragment(),
    UpdateableFragmentListener {
    private lateinit var mDescription: TextView
    private lateinit var mTitle: Button
    private lateinit var mImage: ImageView
    private lateinit var mDate: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.viewpage, container, false)
        mDescription = view.findViewById(R.id.displayDescription)
        mTitle = view.findViewById(R.id.buttonTitle)
        mImage = view.findViewById(R.id.displayImage)
        mDate = view.findViewById(R.id.dateView)
        return view
    }

    //received from Adapter with our Listener
    override fun update(number: Int,showComicNumber: Boolean) {
        if (File(downloadLocation, "$number.json").exists()) {
            val comic: Comic = loadJson("$number.json",downloadLocation)

            mDescription.text = comic.altText
            mTitle.text = comic.title
            if(showComicNumber){
                mTitle.text = "${comic.number}: ${comic.title}"
            } else {
                mTitle.text = comic.title
            }

            val s = SpannableStringBuilder().bold { append(resources.getString(R.string.date)) }.append(" ${comic.day}.${comic.month}.${comic.year}" )
            mDate.text = s
        } else {
            mDescription.text = resources.getString(R.string.comic_not_found)
            mTitle.text = resources.getString(R.string.unknown)
            mDate.text = ""
        }

        if (File(downloadLocation, "$number.png").exists())
            mImage.setImageBitmap(
                BitmapFactory.decodeStream(
                    FileInputStream(
                        File(
                            downloadLocation,
                            "$number.png"
                        ).absolutePath
                    )
                )
            )
        else {
            mImage.setImageDrawable(R.drawable.ic_launcher_foreground.toDrawable())
        }

    }

    companion object {
        fun newInstance(downloadLocation: File): UpdatableFragment {
            return UpdatableFragment(downloadLocation)
        }
    }

}