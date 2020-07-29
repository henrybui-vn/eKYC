package com.android.master.kyc.net.model.response

import com.google.gson.annotations.SerializedName

data class ConfidenceResponse (
    @SerializedName("confidence")
    var confidence: Float
)