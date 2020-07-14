package com.android.master.kyc.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.annotation.NonNull
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class CameraViewModel : ViewModel() {
    val takeImage = MutableLiveData<Bitmap>()
    var photo = ""
    val photos = mutableListOf<String>()
    private var isTakingPhoto = false
    var isTakingFrontPhoto = true
    private val executor: Executor = Executors.newSingleThreadExecutor()
    lateinit var imageCapture: ImageCapture

    fun takePhoto() {
        if (isTakingPhoto) {
            return
        }
        isTakingPhoto = true
        val mDateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
        val file = File(getBatchDirectoryName(), mDateFormat.format(Date()).toString() + ".jpg")
        val outputFileOptions: ImageCapture.OutputFileOptions =
            ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            outputFileOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(@NonNull outputFileResults: ImageCapture.OutputFileResults) {
                    photo = file.path
                    cropImage(file.path)
                    isTakingPhoto = false
                }

                override fun onError(@NonNull error: ImageCaptureException) {
                    error.printStackTrace()
                    isTakingPhoto = false
                }
            })
    }

    fun cropImage(imagePath: String) {
        val options: BitmapFactory.Options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val bitmap = BitmapFactory.decodeFile(imagePath, options)

        val outputBitmap = Bitmap.createBitmap(
            bitmap,
            (bitmap.width * 0.1).toInt(),
            (bitmap.height * 0.2).toInt(),
            (bitmap.width * 0.8).toInt(),
            (bitmap.height * 0.4).toInt()
        )

        try {
            takeImage.postValue(outputBitmap)

            FileOutputStream(imagePath).use({ out ->
                outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getBatchDirectoryName(): String? {
        var app_folder_path = ""
        app_folder_path =
            Environment.getExternalStorageDirectory().toString() + "/images"
        val dir = File(app_folder_path)
        if (!dir.exists() && !dir.mkdirs()) {

        }
        return app_folder_path
    }
}
