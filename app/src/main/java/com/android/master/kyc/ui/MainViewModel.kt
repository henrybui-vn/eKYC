package com.android.master.kyc.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    val typeData = MutableLiveData<Int>()

    fun clickItem(type: Int) {
        typeData.postValue(type)
    }
}
