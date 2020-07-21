package com.android.master.kyc.net.model.response

import com.google.gson.annotations.SerializedName

data class ScanResponse(
    @SerializedName("responses")
    val response: List<ScanFaceResponse>
)