package xyz.pokkst.pokket.cash.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

fun <T1, T2, R> combine(
    liveData1: LiveData<T1>,
    liveData2: LiveData<T2>,
    combineFn: (value1: T1?, value2: T2?) -> R
): LiveData<R> = MediatorLiveData<R>().apply {
    addSource(liveData1) {
        value = combineFn(it, liveData2.value)
    }
    addSource(liveData2) {
        value = combineFn(liveData1.value, it)
    }
}