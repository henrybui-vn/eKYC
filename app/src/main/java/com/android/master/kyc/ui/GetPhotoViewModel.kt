package com.android.master.kyc.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.VideoCapture
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.master.kyc.model.Features
import com.android.master.kyc.model.Image
import com.android.master.kyc.net.APIService
import com.android.master.kyc.net.model.request.*
import com.android.master.kyc.net.model.response.PhotoResponse
import com.android.master.kyc.net.model.response.PhotosResponse
import com.android.master.kyc.net.model.response.ScanFaceResponse
import com.android.master.kyc.net.model.response.VerifyFaceResponse
import com.android.master.kyc.utils.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent
import java.io.ByteArrayOutputStream


class GetPhotoViewModel : ViewModel() {
    val takeImage = MutableLiveData<Bitmap>()
    val takePhotoImage = MutableLiveData<Bitmap>()
    val scanFaceResult = MutableLiveData<Float>()
    val scanFaceWaitForRequest = MutableLiveData<Boolean>()
    val progress = MutableLiveData<Int>()
    val verifyIdentityCard = MutableLiveData<PhotosResponse>()
    val verifyFace = MutableLiveData<VerifyFaceResponse>()
    val scanFaceAction = MutableLiveData<Boolean>()

    var photo: Bitmap? = null
    var facePhoto: Bitmap? = null
    var scanFaceBitmap: Bitmap? = null
    var faceStep = FACE_SMILE
    var isScanning = false
    val photos = mutableListOf<Bitmap?>()
    val verifyIdentityCardResponses = mutableListOf<PhotoResponse>()
    val verifyFaceResponse = VerifyFaceResponse()
    var progressScanFace = 0

    var isVerifyDetailsFragment = false
    var isInitVerifyDetailsData = false
    private var isTakingPhoto = false
    var takingPhotoFinished = false
    var isTakingFrontPhoto = true

    lateinit var imageCapture: ImageCapture
    lateinit var videoCapture: VideoCapture

    val apiService: APIService by KoinJavaComponent.inject(APIService::class.java)

    fun getDetailsFromPhotos(position: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val identityCardRequests = mutableListOf<PhotoRequest>()
                when (position) {
                    0 -> {
                        identityCardRequests.add(
                            PhotoRequest(
                                Features(IDENTITY_CARD_DETECTION),
                                Image(getBase64FromImage(photos.get(position)))
                            )
                        )

                        val photosRequest = PhotosRequest(
                            identityCardRequests
                        )

                        verifyIdentityCard(photosRequest)
                    }
                    1 -> {
                        identityCardRequests.add(
                            PhotoRequest(
                                Features(IDENTITY_CARD_BACK_DETECTION),
                                Image(getBase64FromImage(photos.get(position)))
                            )
                        )

                        val photosRequest = PhotosRequest(
                            identityCardRequests
                        )

                        verifyIdentityCard(photosRequest)
                    }
                    2 -> {
                        val verifyFaceRequests = VerifyFaceRequest(
                            "SYNC",
                            listOf(
                                ImagesRequest(
                                    listOf(
                                        ImageRequest(
                                            Image(
                                                getBase64FromImage(photos.get(0))
                                            )
                                        ),
                                        ImageRequest(
                                            Image(
                                                getBase64FromImage(photos.get(2))
                                            )
                                        )
                                    )
                                )
                            )
                        )

                        verifyFace(verifyFaceRequests)
                    }
                    else -> {
                        return@withContext
                    }
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    fun verifyIdentityCard(photosRequest: PhotosRequest) {
        val headers = mutableMapOf<String, String>()
        headers.put(HEADER, HEADER_KEY)
        apiService.getDetailsFromPhotos(headers, photosRequest)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    handleIdentityCardResponse(result)
                },
                { error ->
                    println(error.message)
                }
            )
    }

    @SuppressLint("CheckResult")
    fun verifyFace(verifyFaceRequest: VerifyFaceRequest) {
        val headers = mutableMapOf<String, String>()
        headers.put(HEADER, HEADER_KEY)
        apiService.verifyFace(headers, verifyFaceRequest)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    handleVerifyFaceResponse(result)
                },
                { error ->
                    println(error.message)
                }
            )
    }

    private fun handleIdentityCardResponse(result: PhotosResponse) {
        verifyIdentityCardResponses.addAll(result.response)

        if (isVerifyDetailsFragment) {
            verifyIdentityCard.postValue(PhotosResponse(verifyIdentityCardResponses))
        }
    }

    private fun handleVerifyFaceResponse(result: VerifyFaceResponse) {
        verifyFaceResponse.responses = result.responses

        if (isVerifyDetailsFragment) {
            verifyFace.postValue(verifyFaceResponse)
        }
    }

    private fun getBase64FromImage(bitmap: Bitmap?): String {
        val baos = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 20, baos) // bm is the bitmap object
        val byteArrayImage = baos.toByteArray()
        val encodedImage: String = Base64.encodeToString(byteArrayImage, Base64.DEFAULT)

        return encodedImage
    }

    fun takePhoto(bitmap: Bitmap?) {
        if (isTakingPhoto) {
            return
        }
        isTakingPhoto = true
        cropImage(bitmap)
        Log.d("QH", "Capture success")
        isTakingPhoto = false
    }

    @SuppressLint("CheckResult")
    fun checkFace(faceRequest: FaceRequest) {
        val faceRequest = ScanFaceRequest(listOf(faceRequest))
        val headers = mutableMapOf<String, String>()
        headers.put(HEADER, HEADER_KEY)
        apiService.scanFaces(headers, faceRequest)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    handleFaceScan(result.response)
                    isScanning = false
                },
                { error ->
                    println(error.message)
                    isScanning = false
                    scanFaceResult.postValue(0f)
                    scanFaceWaitForRequest.postValue(false)
                }
            )
    }

    fun handleFaceScan(result: List<ScanFaceResponse>) {
        scanFaceResult.postValue(result.get(0).faceResponse.liveness)
        scanFaceWaitForRequest.postValue(false)
    }

    fun scanFace() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                isScanning = true

                println("Start")

                var action = ""

                scanFaceWaitForRequest.postValue(true)

                when (faceStep) {
                    FACE_SMILE -> action = "SMILE"
                    FACE_CLOSE_EYE -> {
                        delay(1000)
                        action = "CLOSE_LEFT_EYE"
                    }
                    FACE_NORMAL -> {
                        delay(1000)
                        action = "NORMAL"
                    }
                }

                val images = mutableListOf<Image>()

                for (i in 1..15) {
                    scanFaceAction.postValue(true)
                    delay(150)
                    images.add(
                        Image(
                            getBase64FromImage(scanFaceBitmap)
                        )
                    )
                }

                if (action != "NORMAL") {
                    checkFace(FaceRequest(action, images))
                } else {
                    photos.add(scanFaceBitmap)
                    scanFaceWaitForRequest.postValue(false)
                    takePhotoImage.postValue(scanFaceBitmap)
                }

                println("Stop")
            }
        }
    }

    fun cropImage(bitmap: Bitmap?) {
        if (bitmap != null) {
            val outputBitmap = Bitmap.createBitmap(
                bitmap,
                (bitmap.width * 0.15).toInt(),
                (bitmap.height * 0.24).toInt(),
                (bitmap.width * 0.7).toInt(),
                (bitmap.height * 0.3).toInt()
            )

            try {
                photo = outputBitmap
                takeImage.postValue(outputBitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateProgress(increaseProgress: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                for (i in 1..increaseProgress) {
                    progressScanFace += 1
                    progress.postValue(progressScanFace)
                    delay(50)
                }
            }
        }
    }
}
