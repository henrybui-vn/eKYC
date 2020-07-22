package com.android.master.kyc.ui

import android.annotation.SuppressLint
import android.hardware.Camera
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
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
import kotlinx.android.synthetic.main.camera_fragment.*
import kotlinx.android.synthetic.main.camera_fragment.captureImg
import kotlinx.android.synthetic.main.camera_fragment.imgCaptured
import kotlinx.android.synthetic.main.camera_fragment.layoutConfirmPhoto
import kotlinx.android.synthetic.main.camera_fragment.layoutTakePhoto
import kotlinx.android.synthetic.main.face_scan_fragment.*
import kotlinx.android.synthetic.main.face_scan_fragment.nextStep
import kotlinx.android.synthetic.main.face_scan_fragment.title
import java.util.concurrent.ExecutionException


class FaceScanFragment : Fragment() {
    private val typeData: Int by lazy { arguments?.getInt(EXTRA_1) ?: TYPE_ID_CARD }
    private val photos by lazy { arguments?.getStringArrayList(EXTRA_2) }

    private lateinit var viewModel: GetPhotoViewModel

    private var mCamera: Camera? = null
    private var mPreview: CameraPreview? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.face_scan_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
//        val dialog = GuideDialogFragment(TYPE_FACE_ID)
//        dialog.show(requireFragmentManager(), "Guide")

        viewModel = createSharedViewModel(requireActivity(), GetPhotoViewModel::class.java)


        startCamera()

        initUI()
        observeChanges()
    }

    private fun initUI() {
        setTitle()

        //Spin border
        val rotation = AnimationUtils.loadAnimation(requireActivity(), R.anim.image_spin)
        rotation.setFillAfter(true)
        imgBorderCamera.startAnimation(rotation)

        nextStep.setOnClickListener {
            val bundle = bundleOf(
                EXTRA_1 to typeData,
                EXTRA_2 to photos,
                EXTRA_3 to viewModel.facePhoto
            )

            findNavController().navigate(R.id.verifyDetailsFragment, bundle)
        }

        captureImg.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (viewModel.recording) {
                    return
                }

                captureImg.visibility = View.GONE
                setTitle()
                val file = viewModel.getOutputMediaFile()
                println("Path: " + file.path)
                viewModel.recordVideo(file)
            }
        })
    }

    private fun observeChanges() {
        val dialog = GuideDialogFragment(TYPE_LOADING)

        viewModel.takePhotoImage.observe(viewLifecycleOwner, Observer {
            title.text = "Hoàn thành quét khuôn mặt"
            imgCaptured.setImageBitmap(it)

            layoutTakePhoto.visibility = View.GONE
            layoutConfirmPhoto.visibility = View.VISIBLE

            viewModel.getDetailsFromPhotos(2)
        })

        viewModel.scanWaitForRequest.observe(viewLifecycleOwner, Observer {
            if (it) {
                dialog.show(requireFragmentManager(), "Guide")
            } else {
                dialog.dismiss()
            }
        })

        viewModel.scanFaceResult.observe(viewLifecycleOwner, Observer {
            captureImg.visibility = View.VISIBLE
            if (it == 0f) {
                Toast.makeText(requireContext(), "Quét lỗi vui lòng quét lại!", Toast.LENGTH_LONG)
                    .show()
            } else {
                notifyUser()
                viewModel.faceStep++
                setTitle()
            }
        })
    }

    fun setTitle() {
        when (viewModel.faceStep) {
            FACE_SMILE -> title.text = "Vui lòng cười"
            FACE_CLOSE_EYE -> title.text = "Vui lòng nhắm mắt trái"
            FACE_NORMAL -> title.text = "Vui lòng nhìn thẳng"
        }
    }

    fun notifyUser() {
        var content = ""
        when (viewModel.faceStep) {
            FACE_SMILE -> content = "Quét thành công vui lòng quét bước tiếp theo!"
            FACE_CLOSE_EYE -> content = "Quét thành công vui lòng quét bước tiếp theo!"
            FACE_NORMAL -> content = "Quét hoàn thành vui lòng chọn tiếp tục để nhận kết quả!"
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
