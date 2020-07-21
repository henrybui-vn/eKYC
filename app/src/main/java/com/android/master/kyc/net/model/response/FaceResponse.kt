package com.android.master.kyc.net.model.response

import com.google.gson.annotations.SerializedName

data class FaceResponse(
    @SerializedName("liveness")
    var liveness: Float
)