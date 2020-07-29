package com.android.master.kyc.net.model.request

import com.google.gson.annotations.SerializedName

data class VerifyFaceRequest(
    @SerializedName("mode")
    var mode: String,
    @SerializedName("requests")
    var requests: List<ImagesRequest>
)