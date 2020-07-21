package com.android.master.kyc.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.android.master.kyc.R
import com.android.master.kyc.extension.createSharedViewModel
import com.android.master.kyc.net.model.response.PhotoResponse
import kotlinx.android.synthetic.main.verify_details_fragment.*

class VerifyDetailsFragment : Fragment() {
    private lateinit var viewModel: GetPhotoViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.verify_details_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = createSharedViewModel(requireActivity(), GetPhotoViewModel::class.java)

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
        val options: BitmapFactory.Options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        imgFrontPhoto.clipToOutline = true
        imgBackPhoto.clipToOutline = true

        if (viewModel.photos.size == 2) {
            imgFrontPhoto.setImageBitmap(viewModel.photos.get(0))
            imgBackPhoto.visibility = View.GONE
            imgFacePhoto.setImageBitmap(viewModel.photos.get(1))
        } else if (viewModel.photos.size == 3) {
            imgFrontPhoto.setImageBitmap(viewModel.photos.get(0))
            imgBackPhoto.setImageBitmap(viewModel.photos.get(1))
            imgFacePhoto.setImageBitmap(viewModel.photos.get(2))
        } else {
            imgFrontPhoto.visibility = View.GONE
            imgBackPhoto.visibility = View.GONE
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observeChanges() {
        viewModel.photosResponse.observe(viewLifecycleOwner, Observer {
            if (viewModel.responses.size == 3) {
                viewModel.isInitVerifyDetailsData = true
                fetchData(it.response)
            }
        })

        viewModel.isVerifyDetailsFragment = true

        if (viewModel.responses.size == 3 && !viewModel.isInitVerifyDetailsData) {
            fetchData(viewModel.responses)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun fetchData(datas: List<PhotoResponse>) {
        try {
            if (datas == null) {
                return
            }

            datas.forEach {
                it.identityCard?.forEach {
                    pbPaper.visibility = View.GONE

                    val field = it.field
                    val description = it.description

                    when (field) {
                        "identityId" -> {
                            tvIdentityID.text = description
                            pbIdentityID.visibility = View.GONE
                        }
                        "identityName" -> {
                            tvName.text = description
                            pbName.visibility = View.GONE
                        }
                        "identityBirthday" -> {
                            tvBirthday.text = description
                            pbBirthday.visibility = View.GONE
                        }
                        "identityBirthplace" -> {
                            tvBirthplace.text = description
                            pbBirthplace.visibility = View.GONE
                        }
                        "identityAddress" -> {
                            tvIdentityAddress.text = description
                            pbIdentityAddress.visibility = View.GONE
                        }
                        "identityIssueDate" -> {
                            tvNgC.text = description
                            pbNgC.visibility = View.GONE
                        }
                        "identityIssuePlace" -> {
                            tvNC.text = description
                            pbNC.visibility = View.GONE
                        }
                    }
                }


                it.face?.forEach {
                    val field = it.field

                    when (field) {
                        "faceBounds" -> {
                            tvFace.text = String.format("%.2f", (it.confidence * 100)) + "%"
                        }
                    }
                    pbFace.visibility = View.GONE
                }
            }

            pbIdentityID.visibility = View.GONE
            pbName.visibility = View.GONE
            pbBirthday.visibility = View.GONE
            pbBirthplace.visibility = View.GONE
            pbIdentityAddress.visibility = View.GONE
            pbNgC.visibility = View.GONE
            pbNC.visibility = View.GONE
            pbFace.visibility = View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
