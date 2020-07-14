package com.android.master.kyc.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
    }

    private fun initUI() {
        val options: BitmapFactory.Options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

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
    }

}
