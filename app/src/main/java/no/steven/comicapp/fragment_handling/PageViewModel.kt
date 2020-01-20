package no.steven.comicapp.fragment_handling

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel

// https://developer.android.com/reference/androidx/lifecycle/ViewModel.html
class PageViewModel : ViewModel() {
    private val mTitle = MutableLiveData<String>()
    val text = Transformations.map(
        mTitle
    ) { input -> "Contact not available in $input" }

    fun setIndex(index: String) {
        mTitle.value = index
    }

}