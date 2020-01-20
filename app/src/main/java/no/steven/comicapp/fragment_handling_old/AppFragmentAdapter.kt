package no.steven.comicapp.fragment_handling_old

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import java.io.File

class AppFragmentAdapter(fm: FragmentManager, var mContext: Context,private val downloadLocation: File) :
    FragmentStatePagerAdapter(fm,BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT ) {

    private var count = 3
    private var mNumber: Int = -10
    private var mShowComicNumber = false

    override fun getCount(): Int {
        return count
    }

    fun setCount(newCount: Int) {
        count = newCount
    }

    override fun getItem(position: Int): Fragment {
        return UpdatableFragment.newInstance(downloadLocation)
    }

    //received from ManagerFragment
    fun update(
        number: Int,
        showComicNumber: Boolean
    ) {
        mNumber = number
        mShowComicNumber = showComicNumber
        //updated
        notifyDataSetChanged()
    }

    override fun getItemPosition(`object`: Any): Int {
        if (`object` is UpdateableFragmentListener) {
            `object`.update(mNumber,mShowComicNumber)
        }
        return super.getItemPosition(`object`)
    }
}
