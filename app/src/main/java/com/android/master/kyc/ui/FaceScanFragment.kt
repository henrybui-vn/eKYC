package com.android.master.kyc.ui

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.annotation.NonNull
import androidx.camera.core.*
import androidx.camera.extensions.HdrImageCaptureExtender
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider

import com.android.master.kyc.R
import com.android.master.kyc.ui.dialog.GuideDialogFragment
import com.android.master.kyc.utils.EXTRA
import com.android.master.kyc.utils.TYPE_FACE_ID
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.camera_fragment.*
import kotlinx.android.synthetic.main.camera_fragment.camera
import kotlinx.android.synthetic.main.camera_fragment.captureImg
import kotlinx.android.synthetic.main.camera_fragment.imgCaptured
import kotlinx.android.synthetic.main.camera_fragment.layoutConfirmPhoto
import kotlinx.android.synthetic.main.camera_fragment.layoutTakePhoto
import kotlinx.android.synthetic.main.face_scan_fragment.*
import java.util.concurrent.ExecutionException

class FaceScanFragment : Fragment() {
    private val typeData: Int by lazy { arguments?.getInt(EXTRA) ?: TYPE_FACE_ID }

    private lateinit var viewModel: FaceScanViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.face_scan_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val dialog = GuideDialogFragment(typeData)
        dialog.show(requireFragmentManager(), "Guide")

        viewModel = ViewModelProvider(this).get(FaceScanViewModel::class.java)

        startCamera()
        initUI()
        observeChanges()
    }

    private fun initUI() {
        //Spin border
        val rotation = AnimationUtils.loadAnimation(requireActivity(), R.anim.image_spin)
        rotation.setFillAfter(true)
        imgBorderCamera.startAnimation(rotation)
    }

    private fun observeChanges() {
        viewModel.takeImage.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            imgCaptured.setImageBitmap(it)

            layoutTakePhoto.visibility = View.GONE
            layoutConfirmPhoto.visibility = View.VISIBLE
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
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
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

                viewModel.takePhoto()
            }
        })
    }
}
