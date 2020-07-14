package com.android.master.kyc.net.model.request

import com.android.master.kyc.model.Features
import com.android.master.kyc.model.Image
import com.android.master.kyc.model.ImageUri
import com.google.gson.annotations.SerializedName

data class PhotoRequest(
    @SerializedName("features")
    var features: Features,
    @SerializedName("image")
    var image: Image
)