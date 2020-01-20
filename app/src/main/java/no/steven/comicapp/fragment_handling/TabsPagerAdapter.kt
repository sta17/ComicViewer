package no.steven.comicapp.fragment_handling

import android.content.Context
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class TabsPagerAdapter(
    private val mContext: Context,
    fm: FragmentManager?
) :
    FragmentPagerAdapter(fm!!) {
    override fun getItem(position: Int): Fragment {
        return ComicFragment.newInstance()
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return mContext.resources
            .getString(TAB_TITLES[position])
    }

    override fun getCount(): Int { // Show 3 total pages.
        return 3
    }

    companion object {
        @StringRes
        private val TAB_TITLES =
            intArrayOf(R.string.tab_text_1, R.string.tab_text_2, R.string.tab_text_3)
    }

}