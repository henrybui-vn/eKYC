package com.android.master.kyc.net.model.request

import com.android.master.kyc.model.Image
import com.google.gson.annotations.SerializedName

data class FaceRequest(
    @SerializedName("action")
    var action: String,
    @SerializedName("images")
    var image: List<Image>
)