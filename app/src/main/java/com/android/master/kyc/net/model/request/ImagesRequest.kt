package com.android.master.kyc.net.model.request

import com.google.gson.annotations.SerializedName

data class ImagesRequest(
    @SerializedName("images")
    var images: List<ImageRequest>
)