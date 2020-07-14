package com.android.master.kyc.model

import com.google.gson.annotations.SerializedName

data class IdentityCard(
    @SerializedName("field")
    val field: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("confidence")
    val confidence: Double = 0.00
)