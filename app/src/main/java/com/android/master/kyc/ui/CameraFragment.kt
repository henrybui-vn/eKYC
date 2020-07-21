package com.android.master.kyc.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.NonNull
import androidx.camera.core.*
import androidx.camera.extensions.HdrImageCaptureExtender
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.android.master.kyc.R
import com.android.master.kyc.extension.createSharedViewModel
import com.android.master.kyc.extension.setCameraDisplayOrientation
import com.android.master.kyc.ui.dialog.GuideDialogFragment
import com.android.master.kyc.utils.*
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.camera_fragment.*
import java.util.concurrent.ExecutionException

class CameraFragment : Fragment() {
    private lateinit var viewModel: GetPhotoViewModel

    private val typeData: Int by lazy { arguments?.getInt(EXTRA_1) ?: 0 }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.camera_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val dialog = GuideDialogFragment(typeData)
        dialog.show(requireFragmentManager(), "Guide")
        viewModel = createSharedViewModel(requireActivity(), GetPhotoViewModel::class.java)

        startCamera()
        initUI()
        observeChanges()

        val callback = object : OnBackPressedCallback(
            true
            /** true means that the callback is enabled */
        ) {
            override fun handleOnBackPressed() {
                requireActivity().finish()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun initUI() {
        nextStep.setOnClickListener {
            if (viewModel.takingPhotoFinished) {
                if (viewModel.isTakingFrontPhoto && typeData != TYPE_PASSPORT) {
                    viewModel.isTakingFrontPhoto = false

                    title.text = "Mặt sau"
                    viewModel.photos.add(viewModel.photo)
                    layoutTakePhoto.visibility = View.VISIBLE
                    layoutConfirmPhoto.visibility = View.GONE
                    progressTakeImage.visibility = View.GONE
                    viewModel.getDetailsFromPhotos(0)
                } else {
                    viewModel.photos.add(viewModel.photo)
                    viewModel.getDetailsFromPhotos(1)
                    val bundle = bundleOf(
                        EXTRA_1 to typeData,
                        EXTRA_2 to viewModel.photos
                    )
                    camera.releasePointerCapture()
                    findNavController().navigate(R.id.faceScanFragment, bundle)
                }
            }
        }
    }

    private fun observeChanges() {
        viewModel.takeImage.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            Log.d("QH", "Init photo");
            progressTakeImage.visibility = View.GONE
            viewModel.takingPhotoFinished = true
            imgCaptured.setImageBitmap(it)
        })
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
        viewModel.imageCapture = builder
            .setTargetRotation(requireActivity().windowManager.defaultDisplay.rotation)
            .build()
        preview.setSurfaceProvider(camera.createSurfaceProvider())
        val camera: Camera = cameraProvider.bindToLifecycle(
            (this as LifecycleOwner),
            cameraSelector,
            preview,
            imageAnalysis,
            viewModel.imageCapture
        )

        captureImg.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                imgCaptured.setImageBitmap(null)
                layoutTakePhoto.visibility = View.GONE
                layoutConfirmPhoto.visibility = View.VISIBLE
                progressTakeImage.visibility = View.VISIBLE
                viewModel.takePhoto()
            }
        })
    }
}
