package com.android.master.kyc.net.model

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class AppResponse(
    @Keep
    @Expose
    @SerializedName("id") var id: String,
    @Keep
    @Expose
    @SerializedName("name") var name: String,
    @Keep
    @Expose
    @SerializedName("summary") var summary: String,
    @Keep
    @Expose
    @SerializedName("order") var order: Int,
    @Keep
    @Expose
    @SerializedName("lang") var language: String,
    @Keep
    @Expose
    @SerializedName("store_package") var storePackage: String,
    @Keep
    @Expose
    @SerializedName("image_preview") var imgPreviewUrl: String,
    @Keep
    @Expose
    @SerializedName("image_background") var imgBgUrl: String,
    @Keep
    @Expose
    @SerializedName("big_icon") var bigIconUrl: String,
    @Keep
    @Expose
    @SerializedName("small_icon") var smallIconUrl: String,
    @Keep
    @Expose
    @SerializedName("version_code") var verCode: String,
    @Keep
    @Expose
    @SerializedName("color") var color: String
)