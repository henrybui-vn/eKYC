package com.android.master.kyc.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.util.Base64
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.VideoCapture
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.master.kyc.extension.toBitmap
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors


class GetPhotoViewModel : ViewModel() {
    val takeImage = MutableLiveData<Bitmap>()
    val takePhotoImage = MutableLiveData<Bitmap>()
    val scanFaceResult = MutableLiveData<Float>()
    val scanIdentificationWaitForRequest = MutableLiveData<Boolean>()
    val scanFaceWaitForRequest = MutableLiveData<Boolean>()

    var photo: Bitmap? = null
    var facePhoto: Bitmap? = null
    var faceStep = FACE_SMILE
    var recording = false
    val photos = mutableListOf<Bitmap?>()
    val verifyIdentityCardResponses = mutableListOf<PhotoResponse>()
    val verifyFaceResponse = VerifyFaceResponse()

    var isVerifyDetailsFragment = false
    var isInitVerifyDetailsData = false
    private var isTakingPhoto = false
    var takingPhotoFinished = false
    var isTakingFrontPhoto = true
    private val executor: Executor = Executors.newSingleThreadExecutor()
    lateinit var imageCapture: ImageCapture
    lateinit var videoCapture: VideoCapture

    val apiService: APIService by KoinJavaComponent.inject(APIService::class.java)

    val verifyIdentityCard = MutableLiveData<PhotosResponse>()
    val verifyFace = MutableLiveData<VerifyFaceResponse>()

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
            verifyIdentityCard.postValue(PhotosResponse(verifyIdentityCardResponses))
        }
    }

    private fun getBase64FromImage(bitmap: Bitmap?): String {
        val baos = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 20, baos) // bm is the bitmap object
        val byteArrayImage = baos.toByteArray()
        val encodedImage: String = Base64.encodeToString(byteArrayImage, Base64.DEFAULT)

        return encodedImage
    }

    fun takePhoto() {
        if (isTakingPhoto) {
            return
        }
        isTakingPhoto = true

        scanIdentificationWaitForRequest.postValue(true)
        imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
            @SuppressLint("UnsafeExperimentalUsageError")
            override fun onCaptureSuccess(image: ImageProxy) {
                Log.d("QH", "Capture success")
                isTakingPhoto = false
                cropImage(image.image?.toBitmap())
                scanIdentificationWaitForRequest.postValue(false)
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
                isTakingPhoto = false
                scanIdentificationWaitForRequest.postValue(false)
                exception.printStackTrace()
            }
        })
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
                    recording = false
                },
                { error ->
                    println(error.message)
                    recording = false
                    scanFaceResult.postValue(0f)
                    scanFaceWaitForRequest.postValue(false)
                }
            )
    }

    fun handleFaceScan(result: List<ScanFaceResponse>) {
        scanFaceResult.postValue(result.get(0).faceResponse.liveness)
        scanFaceWaitForRequest.postValue(false)
    }

    @SuppressLint("RestrictedApi")
    fun recordVideo(file: File) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                recording = true

                println("Start")
                videoCapture.startRecording(
                    file,
                    executor,
                    object : VideoCapture.OnVideoSavedCallback {
                        override fun onVideoSaved(file: File) {
                            println("onVideoSaved")
                            getFramesFromVideo(file)
                        }

                        override fun onError(
                            videoCaptureError: Int,
                            message: String,
                            cause: Throwable?
                        ) {
                            scanFaceWaitForRequest.postValue(false)
                            scanFaceResult.postValue(0f)
                        }

                    })
                delay(2500)
                videoCapture.stopRecording()
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

    fun getFramesFromVideo(file: File) {
        try {
            var action = ""

            scanFaceWaitForRequest.postValue(true)

            when (faceStep) {
                FACE_SMILE -> action = "SMILE"
                FACE_CLOSE_EYE -> action = "CLOSE_LEFT_EYE"
                FACE_NORMAL -> action = "NORMAL"
            }

            val images = mutableListOf<Image>()
            val retriever = MediaMetadataRetriever()

            retriever.setDataSource(file.getAbsolutePath());

            for (i in 1..15) {
                val bitmap =
                    retriever.getFrameAtTime(
                        (2000000 / i).toLong(),
                        MediaMetadataRetriever.OPTION_CLOSEST
                    )
                images.add(
                    Image(
                        getBase64FromImage(bitmap)
                    )
                )
            }

            if (action != "NORMAL") {
                checkFace(FaceRequest(action, images))
            } else {
                val bitmap =
                    retriever.getFrameAtTime(
                        (1000000).toLong(),
                        MediaMetadataRetriever.OPTION_CLOSEST
                    )
                photos.add(bitmap)
                scanFaceWaitForRequest.postValue(false)
                takePhotoImage.postValue(bitmap)
            }

            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Create a File for saving an image or video */
    @SuppressLint("SimpleDateFormat")
    fun getOutputMediaFile(): File {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        val mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "MyCameraApp"
        )
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        mediaStorageDir.apply {
            if (!exists()) {
                mkdirs()
            }
        }

        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return File("${mediaStorageDir.path}${File.separator}VID_$timeStamp.mp4")
    }
}
