package com.android.master.kyc.net.model.response

import com.google.gson.annotations.SerializedName

data class VerifyFaceResponse(
    @SerializedName("responses")
    var responses: List<FaceVerificationResultsResponse> = emptyList()
)