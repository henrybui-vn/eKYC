package com.android.master.kyc.ui.dialog

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.DialogFragment
import com.android.master.kyc.R
import com.android.master.kyc.utils.*


class GuideDialogFragment(private val typeData: Int) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        retainInstance = true
        val context: Context = requireActivity()
        val builder =
            AlertDialog.Builder(context, getTheme())
        val dialogInflater = LayoutInflater.from(builder.context)
        val view: View =
            dialogInflater.inflate(R.layout.dialog_guide, null, false)

        val imgGuide = view.findViewById<AppCompatImageView>(R.id.img_guide)
        val videoGuide = view.findViewById<VideoView>(R.id.video_guide)
        val btClose = view.findViewById<AppCompatButton>(R.id.bt_close_guide)
        val layoutLoading = view.findViewById<RelativeLayout>(R.id.layout_loading)

        when (typeData) {
            TYPE_ID_CARD -> {
                imgGuide.visibility = View.VISIBLE
                imgGuide.setImageDrawable(resources.getDrawable(R.drawable.guide_cmt))
            }
            TYPE_PASSPORT -> {
                imgGuide.visibility = View.VISIBLE
                imgGuide.setImageDrawable(resources.getDrawable(R.drawable.guide_hochieu))
            }
            TYPE_DRIVER_LICENSE -> {
                imgGuide.visibility = View.VISIBLE
                imgGuide.setImageDrawable(resources.getDrawable(R.drawable.guide_bang_lai_xe))
            }
            TYPE_MILITARY -> {
                imgGuide.visibility = View.VISIBLE
                imgGuide.setImageDrawable(resources.getDrawable(R.drawable.guide_military))
            }
            TYPE_FACE_ID -> {
                videoGuide.visibility = View.VISIBLE
                val path =
                    "android.resource://" + requireActivity().getPackageName() + "/" + R.raw.support_capture_face_app
                videoGuide.setVideoURI(Uri.parse(path))
                videoGuide.setOnPreparedListener { mp ->
                    mp.isLooping = true
                    videoGuide.start()
                }
            }
            TYPE_LOADING -> {
                btClose.visibility = View.GONE
                imgGuide.visibility = View.GONE
                videoGuide.visibility = View.GONE
                layoutLoading.visibility = View.VISIBLE
                isCancelable = false
            }
        }

        btClose.setOnClickListener { dismiss() }
        builder.setView(view)
        builder.setCancelable(false)
        val dialog = builder.create()

        return dialog
    }

    override fun onPause() {
        super.onPause()
    }

    override fun getTheme(): Int {
        return R.style.WindowSlideUpDown
    }
}