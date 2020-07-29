package com.android.master.kyc.net.model.response

import com.google.gson.annotations.SerializedName

data class FaceVerificationResultsResponse(
    @SerializedName("faceVerificationResults")
    var faceVerificationResults: ConfidenceResponse
)