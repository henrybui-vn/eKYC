package com.android.master.kyc.net.model.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class PhotosResponse(
    @SerializedName("responses")
    val response: List<PhotoResponse>
)