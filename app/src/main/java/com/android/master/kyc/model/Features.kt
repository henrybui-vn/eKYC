package com.android.master.kyc.model

import com.google.gson.annotations.SerializedName

data class Features(
    @SerializedName("type")
    var type: String,
    @SerializedName("maxResults")
    var maxResults: Int = 1
)