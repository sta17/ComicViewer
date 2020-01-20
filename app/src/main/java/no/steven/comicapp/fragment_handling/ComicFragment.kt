package no.steven.comicapp.fragment_handling

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import no.steven.comicapp.R
import java.io.File

// https://developer.android.com/reference/androidx/lifecycle/ViewModelProvider.html
class ComicFragment (private val downloadLocation: File) : Fragment() {

    private var pageViewModel: PageViewModel? = null
    private lateinit var mDescription: TextView
    private lateinit var mTitle: Button
    private lateinit var mImage: ImageView
    private lateinit var mDate: TextView
    private val TAG = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProvider(PageViewModel)
        pageViewModel.setIndex(TAG)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? { // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.viewpage, container, false)
        mDescription = view.findViewById(R.id.displayDescription)
        mTitle = view.findViewById(R.id.buttonTitle)
        mImage = view.findViewById(R.id.displayImage)
        mDate = view.findViewById(R.id.dateView)
        return view
    }

    companion object {
        private const val TAG = "Comic"
        /**
         * @return A new instance of fragment ContactsFragment.
         */
        fun newInstance(): ComicFragment {
            return ComicFragment()
        }
    }
}