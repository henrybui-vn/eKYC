package com.android.master.kyc.ui

import android.hardware.Camera
import android.media.MediaRecorder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.android.master.kyc.R
import com.android.master.kyc.extension.createSharedViewModel
import com.android.master.kyc.extension.setCameraDisplayOrientation
import com.android.master.kyc.ui.dialog.GuideDialogFragment
import com.android.master.kyc.utils.*
import kotlinx.android.synthetic.main.camera_fragment.captureImg
import kotlinx.android.synthetic.main.camera_fragment.imgCaptured
import kotlinx.android.synthetic.main.camera_fragment.layoutConfirmPhoto
import kotlinx.android.synthetic.main.camera_fragment.layoutTakePhoto
import kotlinx.android.synthetic.main.face_scan_fragment.*
import java.io.File


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
        val dialog = GuideDialogFragment(TYPE_FACE_ID)
        dialog.show(requireFragmentManager(), "Guide")

        viewModel = createSharedViewModel(requireActivity(), GetPhotoViewModel::class.java)

        // Create an instance of Camera
        mCamera = getCameraInstance()

        mPreview = mCamera?.let {
            // Create our Preview view
            CameraPreview(requireActivity(), it)
        }

        // Set the Preview view as the content of our activity.
        mPreview?.also {
            cameraView.addView(it)
        }

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

                setTitle()
                val file = viewModel.getOutputMediaFile()
                println("Path: " + file.path)
                prepareVideoRecorder(file)
                viewModel.takeMultipleFacePhotos(file)
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


    private fun prepareVideoRecorder(file: File): Boolean {
        viewModel.videoRecorder = MediaRecorder()

        mCamera?.let { camera ->
            // Step 1: Unlock and set camera to MediaRecorder
            camera.unlock()

            viewModel.videoRecorder.run {
                setCamera(camera)

                // Step 2: Set sources
                setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
                setVideoSource(MediaRecorder.VideoSource.CAMERA)

                // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)

                // Step 4: Set output file
                setOutputFile(file)

                // Step 5: Set the preview output
                setPreviewDisplay(mPreview?.holder?.surface)

                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP)
                setMaxDuration(3000)

                // Step 6: Prepare configured MediaRecorder
                return try {
                    prepare()
                    true
                } catch (e: IllegalStateException) {
                    releaseMediaRecorder()
                    false
                }
            }

        }
        return false
    }

    private fun releaseMediaRecorder() {
        viewModel.videoRecorder.reset() // clear recorder configuration
        viewModel.videoRecorder.release() // release the recorder object
        mCamera?.lock() // lock camera for later use
    }

    private fun releaseCamera() {
        mCamera?.release() // release the camera for other applications
        mCamera = null
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

    /** A safe way to get an instance of the Camera object. */
    fun getCameraInstance(): Camera? {
        return try {
            Camera.open(1) // attempt to get a Camera instance
        } catch (e: Exception) {
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
    }
}
