package com.android.master.kyc.net.model.request

import com.android.master.kyc.model.Image
import com.google.gson.annotations.SerializedName

data class ImageRequest(
    @SerializedName("image")
    var image: Image
)