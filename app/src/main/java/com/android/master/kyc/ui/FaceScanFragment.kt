package com.android.master.kyc.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.impl.VideoCaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.android.master.kyc.R
import com.android.master.kyc.extension.createSharedViewModel
import com.android.master.kyc.ui.dialog.GuideDialogFragment
import com.android.master.kyc.utils.*
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.camera_fragment.captureImg
import kotlinx.android.synthetic.main.face_scan_fragment.*
import kotlinx.android.synthetic.main.face_scan_fragment.title
import java.util.concurrent.ExecutionException


class FaceScanFragment : Fragment() {
    private val typeData: Int by lazy { arguments?.getInt(EXTRA_1) ?: TYPE_ID_CARD }
    private val photos by lazy { arguments?.getStringArrayList(EXTRA_2) }

    private lateinit var viewModel: GetPhotoViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.face_scan_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val dialog = GuideDialogFragment(TYPE_FACE_ID)
        dialog.show(requireFragmentManager(), DIALOG_GUIDE)

        viewModel = createSharedViewModel(requireActivity(), GetPhotoViewModel::class.java)


        startCamera()

        initUI()
        observeChanges()
    }

    private fun initUI() {
        setTitle()

        captureImg.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (viewModel.isScanning) {
                    return
                }

                captureImg.visibility = View.GONE
                setTitle()
                viewModel.scanFace()
            }
        })
    }

    private fun observeChanges() {
        val dialog = GuideDialogFragment(TYPE_LOADING, getString(R.string.label_require_wait_for_scan))

        viewModel.takePhotoImage.observe(viewLifecycleOwner, Observer {
            notifyUser()
            viewModel.getDetailsFromPhotos(2)

            val bundle = bundleOf(
                EXTRA_1 to typeData,
                EXTRA_2 to photos,
                EXTRA_3 to viewModel.facePhoto
            )

            findNavController().navigate(R.id.verifyDetailsFragment, bundle)
        })

        viewModel.scanFaceWaitForRequest.observe(viewLifecycleOwner, Observer {
            if (it) {
                dialog.show(requireFragmentManager(), DIALOG_GUIDE)
            } else {
                dialog.dismiss()
            }
        })

        viewModel.scanFaceResult.observe(viewLifecycleOwner, Observer {
            if (it < 0f) {
                captureImg.visibility = View.VISIBLE
                Toast.makeText(
                    requireContext(),
                    getString(R.string.label_require_rescan),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                notifyUser()
                viewModel.faceStep++
                setTitle()
                viewModel.scanFace()
            }
        })

        viewModel.progress.observe(viewLifecycleOwner, Observer {
            progressCircular.progress = it
        })

        viewModel.scanFaceAction.observe(viewLifecycleOwner, Observer {
            if (it) {
                viewModel.scanFaceBitmap = cameraView.bitmap
            }
        })
    }

    fun setTitle() {
        when (viewModel.faceStep) {
            FACE_SMILE -> title.text = getString(R.string.label_require_smile)
            FACE_CLOSE_EYE -> title.text = getString(R.string.label_require_close_eye)
            FACE_NORMAL -> title.text = getString(R.string.label_require_straight)
        }
    }

    private fun notifyUser() {
        var content = ""
        when (viewModel.faceStep) {
            FACE_SMILE -> {
                content = getString(R.string.label_require_scan_next_step)
                viewModel.updateProgress(33)
            }
            FACE_CLOSE_EYE -> {
                content = getString(R.string.label_require_scan_next_step)
                viewModel.updateProgress(33)
            }
            FACE_NORMAL -> {
                content = getString(R.string.label_finish_scan)
                viewModel.updateProgress(34)
//                imgBorderCamera.visibility = View.VISIBLE
//
//                //Spin border
//                val rotation = AnimationUtils.loadAnimation(requireActivity(), R.anim.image_spin)
//                rotation.setFillAfter(true)
//                imgBorderCamera.startAnimation(rotation)
            }
        }
        Toast.makeText(requireContext(), content, Toast.LENGTH_SHORT).show()
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

    @SuppressLint("RestrictedApi")
    fun bindPreview(@NonNull cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder()
            .build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        val builder = VideoCaptureConfig.Builder()
        builder.setVideoFrameRate(30)
        builder.setAudioBitRate(0)

        viewModel.videoCapture = builder
            .setTargetRotation(requireActivity().windowManager.defaultDisplay.rotation)
            .build()
        preview.setSurfaceProvider(cameraView.createSurfaceProvider())
        val camera: androidx.camera.core.Camera = cameraProvider.bindToLifecycle(
            (this as LifecycleOwner),
            cameraSelector,
            preview,
            viewModel.videoCapture
        )
    }
}
