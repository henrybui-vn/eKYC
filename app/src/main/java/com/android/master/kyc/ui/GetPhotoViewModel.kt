package com.android.master.kyc.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.master.kyc.extension.toBitmap
import com.android.master.kyc.model.Features
import com.android.master.kyc.model.Image
import com.android.master.kyc.net.APIService
import com.android.master.kyc.net.model.request.FaceRequest
import com.android.master.kyc.net.model.request.PhotoRequest
import com.android.master.kyc.net.model.request.PhotosRequest
import com.android.master.kyc.net.model.request.ScanFaceRequest
import com.android.master.kyc.net.model.response.PhotoResponse
import com.android.master.kyc.net.model.response.PhotosResponse
import com.android.master.kyc.net.model.response.ScanFaceResponse
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
    val scanWaitForRequest = MutableLiveData<Boolean>()

    var photo: Bitmap? = null
    var facePhoto: Bitmap? = null
    var faceStep = FACE_SMILE
    var recording = false
    val photos = mutableListOf<Bitmap?>()
    val responses = mutableListOf<PhotoResponse>()

    var isVerifyDetailsFragment = false
    var isInitVerifyDetailsData = false
    private var isTakingPhoto = false
    var takingPhotoFinished = false
    var isTakingFrontPhoto = true
    private val executor: Executor = Executors.newSingleThreadExecutor()
    lateinit var imageCapture: ImageCapture
    var videoRecorder = MediaRecorder()

    val apiService: APIService by KoinJavaComponent.inject(APIService::class.java)


    val photosResponse = MutableLiveData<PhotosResponse>()

    fun getDetailsFromPhotos(position: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val requests = mutableListOf<PhotoRequest>()
                when (position) {
                    0 -> {
                        requests.add(
                            PhotoRequest(
                                Features(IDENTITY_CARD_DETECTION),
                                Image(getBase64FromImage(photos.get(position)))
                            )
                        )
                    }
                    1 -> {
                        requests.add(
                            PhotoRequest(
                                Features(IDENTITY_CARD_BACK_DETECTION),
                                Image(getBase64FromImage(photos.get(position)))
                            )
                        )
                    }
                    2 -> {
                        requests.add(
                            PhotoRequest(
                                Features(FACE_DETECTION),
                                Image(getBase64FromImage(photos.get(position)))
                            )
                        )
                    }
                }

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
        responses.addAll(result.response)

        if (isVerifyDetailsFragment) {
            photosResponse.postValue(PhotosResponse(responses))
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
        Log.d("QH", "Taking photo: " + isTakingPhoto)
        if (isTakingPhoto) {
            return
        }
        isTakingPhoto = true

        imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
            @SuppressLint("UnsafeExperimentalUsageError")
            override fun onCaptureSuccess(image: ImageProxy) {
                Log.d("QH", "Capture success")
                isTakingPhoto = false
                cropImage(image.image?.toBitmap())
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
                isTakingPhoto = false
                exception.printStackTrace()
            }
        })
    }

    fun takeMultipleFacePhotos(file: File) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                recording = true
                videoRecorder.start()
//                recordVideos(file)
                delay(2500)
                videoRecorder.stop()
                getFramesFromVideo(file)
            }
        }
    }

    fun getFramesFromVideo(file: File) {
        try {
            var action = ""

            scanWaitForRequest.postValue(true)


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
                        (200000 / i).toLong(),
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
                scanWaitForRequest.postValue(false)
                takePhotoImage.postValue(bitmap)
            }

            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                    scanWaitForRequest.postValue(false)
                }
            )
    }

    fun handleFaceScan(result: List<ScanFaceResponse>) {
        scanFaceResult.postValue(result.get(0).faceResponse.liveness)
        scanWaitForRequest.postValue(false)
    }

    fun takeFacePhoto() {
//        imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
//            @SuppressLint("UnsafeExperimentalUsageError")
//            override fun onCaptureSuccess(image: ImageProxy) {
//                Log.d("QH", "Capture success")
//                val outputBitmap = image.image?.toBitmap()
//                photo = outputBitmap
//                takePhotoImage.postValue(outputBitmap)
//                photos.add(photo)
//            }
//
//            override fun onError(exception: ImageCaptureException) {
//                super.onError(exception)
//                exception.printStackTrace()
//            }
//        })
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

    /** Create a file Uri for saving an image or video */
    private fun getOutputMediaFileUri(type: Int): Uri {
        return Uri.fromFile(getOutputMediaFile())
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
