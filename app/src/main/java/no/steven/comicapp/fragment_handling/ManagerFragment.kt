package no.steven.comicapp.fragment_handling

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import no.steven.comicapp.R
import java.io.File


class ManagerFragment(private val downloadLocation: File, private val currentComicNumber: Int) : Fragment() {

    private var mViewPager: ViewPager? = null
    private var mAdapter: AppFragmentAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mViewPager = view.findViewById(R.id.viewpager) as ViewPager

        mAdapter = AppFragmentAdapter(
            this.fragmentManager!!,
            this.activity!!,
            downloadLocation
        )
        //mAdapter!!.changeNumber(currentComicNumber)

        mViewPager!!.adapter = mAdapter

    }

    fun update(number: Int,showComicNumber: Boolean) {
        mAdapter!!.update(number,showComicNumber)
    }

    companion object {

        fun newInstance(downloadLocation: File, currentComicNumber: Int): ManagerFragment {
            return ManagerFragment(downloadLocation,currentComicNumber)
        }
    }

}// Required empty public constructor