package xyz.pokkst.pokket.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PageViewModel : ViewModel() {

    private val _index = MutableLiveData<Int>()
    fun setIndex(index: Int) {
        _index.value = index
    }
}