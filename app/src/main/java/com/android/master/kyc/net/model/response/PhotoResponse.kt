package com.android.master.kyc.net.model.response

import com.android.master.kyc.model.Face
import com.android.master.kyc.model.IdentityCard
import com.google.gson.annotations.SerializedName

data class PhotoResponse(
    @SerializedName("identityCardAnnotations")
    var identityCard: List<IdentityCard>?,
    @SerializedName("faceAnnotations")
    var face: List<Face>?
)