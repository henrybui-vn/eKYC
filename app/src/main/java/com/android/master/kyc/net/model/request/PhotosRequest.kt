package com.android.master.kyc.net.model.request

import com.google.gson.annotations.SerializedName

data class PhotosRequest(
    @SerializedName("requests")
    var requests: List<PhotoRequest>
)