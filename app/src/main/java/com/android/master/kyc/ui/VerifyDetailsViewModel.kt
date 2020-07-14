package com.android.master.kyc.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.master.kyc.model.Features
import com.android.master.kyc.model.Image
import com.android.master.kyc.net.APIService
import com.android.master.kyc.net.model.response.PhotosResponse
import com.android.master.kyc.net.model.request.PhotoRequest
import com.android.master.kyc.net.model.request.PhotosRequest
import com.android.master.kyc.utils.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent
import java.io.ByteArrayOutputStream


class VerifyDetailsViewModel : ViewModel() {
    val apiService: APIService by KoinJavaComponent.inject(APIService::class.java)

    val responses = MutableLiveData<PhotosResponse>()

    fun getDetailsFromPhotos(frontImage: String = "", backImage: String = "", faceImage: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val requests = mutableListOf<PhotoRequest>()
                requests.add(
                    PhotoRequest(
                        Features(IDENTITY_CARD_DETECTION),
                        Image(getBase64FromImage(frontImage))
                    )
                )

                requests.add(
                    PhotoRequest(
                        Features(IDENTITY_CARD_BACK_DETECTION),
                        Image(getBase64FromImage(backImage))
                    )
                )

                requests.add(
                    PhotoRequest(
                        Features(FACE_DETECTION),
                        Image(getBase64FromImage(faceImage))
                    )
                )
                val photosRequest = PhotosRequest(
                    requests
                )
                getDataFromServer(photosRequest)
            }
        }
    }

    @SuppressLint("CheckResult")
    fun getDataFromServer(photosRequest: PhotosRequest) {
        val headers = mutableMapOf<String, String>()
        headers.put(HEADER, HEADER_KEY)
        apiService.getDetailsFromPhotos(headers, photosRequest)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    handleResponse(result)
                },
                { error ->
                    println(error.message)
                }
            )
    }

    private fun handleResponse(result: PhotosResponse) {
        responses.postValue(result)
    }

    private fun getBase64FromImage(imagePath: String): String {
        val bm = BitmapFactory.decodeFile(imagePath)
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos) // bm is the bitmap object
        val byteArrayImage = baos.toByteArray()
        val encodedImage: String = Base64.encodeToString(byteArrayImage, Base64.DEFAULT)

        return encodedImage
    }
}
