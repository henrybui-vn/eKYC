package com.android.master.kyc.net.model.response

import com.google.gson.annotations.SerializedName

data class ScanFaceResponse(
    @SerializedName("livenessResults")
    var faceResponse: FaceResponse
)