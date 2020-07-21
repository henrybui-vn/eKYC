package com.android.master.kyc.net.model.request

import com.google.gson.annotations.SerializedName

data class ScanFaceRequest(
    @SerializedName("requests")
    var requests: List<FaceRequest>
)