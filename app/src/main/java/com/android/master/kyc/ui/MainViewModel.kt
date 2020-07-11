package com.android.master.kyc.ui

import androidx.lifecycle.ViewModel
import com.google.gson.Gson

class MainViewModel : ViewModel() {
//    val films = MutableLiveData<List<FilmModel>>()
//    val filmRepository: FilmRepository by KoinJavaComponent.inject(FilmRepository::class.java)

    init {
        getDataFromServer()
    }

    private fun getDataFromServer() {
    }
}
