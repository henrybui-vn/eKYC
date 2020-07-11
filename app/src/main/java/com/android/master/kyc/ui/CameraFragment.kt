package com.android.master.kyc.ui

import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.camera.core.*
import androidx.camera.extensions.HdrImageCaptureExtender
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.android.master.kyc.R
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.camera_fragment.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    companion object {
        fun newInstance() = CameraFragment()
    }

    private lateinit var viewModel: CameraViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        startCamera()
        return inflater.inflate(R.layout.camera_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CameraViewModel::class.java)
        // TODO: Use the ViewModel
    }

    private fun startCamera() {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                bindPreview(cameraProvider)
            } catch (e: ExecutionException) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            } catch (e: InterruptedException) {
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private val executor: Executor = Executors.newSingleThreadExecutor()

    fun bindPreview(@NonNull cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder()
            .build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        val imageAnalysis = ImageAnalysis.Builder()
            .build()
        val builder = ImageCapture.Builder()

        //Vendor-Extensions (The CameraX extensions dependency in build.gradle)
        val hdrImageCaptureExtender =
            HdrImageCaptureExtender.create(builder)

        // Query if extension is available (optional).
        if (hdrImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
            // Enable the extension if available.
            hdrImageCaptureExtender.enableExtension(cameraSelector)
        }
        val imageCapture = builder
            .setTargetRotation(requireActivity().windowManager.defaultDisplay.rotation)
            .build()
        preview.setSurfaceProvider(camera.createSurfaceProvider())
        val camera: Camera = cameraProvider.bindToLifecycle(
            (this as LifecycleOwner),
            cameraSelector,
            preview,
            imageAnalysis,
            imageCapture
        )

        captureImg.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val mDateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
                val file =
                    File(getBatchDirectoryName(), mDateFormat.format(Date()).toString() + ".jpg")
                val outputFileOptions: ImageCapture.OutputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                imageCapture.takePicture(
                    outputFileOptions,
                    executor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(@NonNull outputFileResults: ImageCapture.OutputFileResults) {
                            Handler().post {
                                Toast.makeText(
                                    requireContext(),
                                    "Image Saved successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onError(@NonNull error: ImageCaptureException) {
                            error.printStackTrace()
                        }
                    })
            }
        })
    }

    fun getBatchDirectoryName(): String? {
        var app_folder_path = ""
        app_folder_path =
            Environment.getExternalStorageDirectory().toString().toString() + "/images"
        val dir = File(app_folder_path)
        if (!dir.exists() && !dir.mkdirs()) {
        }
        return app_folder_path
    }
}
