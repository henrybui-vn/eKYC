package com.android.master.kyc.model

import com.google.gson.annotations.SerializedName

data class Image(
    @SerializedName("content")
    var image: String
)