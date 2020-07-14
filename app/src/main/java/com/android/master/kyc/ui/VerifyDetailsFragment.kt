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
import androidx.lifecycle.ViewModelProvider
import com.android.master.kyc.R
import com.android.master.kyc.utils.EXTRA_1
import com.android.master.kyc.utils.EXTRA_2
import com.android.master.kyc.utils.EXTRA_3
import com.android.master.kyc.utils.TYPE_ID_CARD
import kotlinx.android.synthetic.main.verify_details_fragment.*

class VerifyDetailsFragment : Fragment() {
    private lateinit var viewModel: VerifyDetailsViewModel

    private val typeData: Int by lazy { arguments?.getInt(EXTRA_1) ?: TYPE_ID_CARD }
    private val photos by lazy { arguments?.getStringArrayList(EXTRA_2) }
    private val facePhoto by lazy { arguments?.getString(EXTRA_3) ?: "" }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.verify_details_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(VerifyDetailsViewModel::class.java)

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

        if (photos?.size == 1) {
            val bitmap = BitmapFactory.decodeFile(photos?.get(0), options)
            imgFrontPhoto.setImageBitmap(bitmap)
            imgBackPhoto.visibility = View.GONE
        } else if (photos?.size == 2) {
            val bitmapFront = BitmapFactory.decodeFile(photos?.get(0), options)
            imgFrontPhoto.setImageBitmap(bitmapFront)
            val bitmapBack = BitmapFactory.decodeFile(photos?.get(1), options)
            imgBackPhoto.setImageBitmap(bitmapBack)
        } else {
            imgFrontPhoto.visibility = View.GONE
            imgBackPhoto.visibility = View.GONE
        }

        val bitmap = BitmapFactory.decodeFile(facePhoto, options)
        imgFacePhoto.setImageBitmap(bitmap)

        viewModel.getDetailsFromPhotos(photos!!.get(0), photos!!.get(1), facePhoto)
    }

    @SuppressLint("SetTextI18n")
    private fun observeChanges() {
        viewModel.responses.observe(viewLifecycleOwner, Observer { it ->
            try {
                pbPaper.visibility = View.GONE
                pbIdentityID.visibility = View.GONE
                pbName.visibility = View.GONE
                pbBirthday.visibility = View.GONE
                pbBirthplace.visibility = View.GONE
                pbIdentityAddress.visibility = View.GONE
                pbNgC.visibility = View.GONE
                pbNC.visibility = View.GONE
                pbFace.visibility = View.GONE

                it.response.forEach {
                    it.identityCard?.forEach {
                        val field = it.field
                        val description = it.description

                        when (field) {
                            "identityId" -> {
                                tvIdentityID.text = description
                            }
                            "identityName" -> {
                                tvName.text = description
                            }
                            "identityBirthday" -> {
                                tvBirthday.text = description
                            }
                            "identityBirthplace" -> {
                                tvBirthplace.text = description
                            }
                            "identityAddress" -> {
                                tvIdentityAddress.text = description
                            }
                            "identityIssueDate" -> {
                                tvNgC.text = description
                            }
                            "identityIssuePlace" -> {
                                tvNC.text = description
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
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        })
    }
}
