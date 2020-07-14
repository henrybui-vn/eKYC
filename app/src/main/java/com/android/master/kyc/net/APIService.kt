package com.android.master.kyc.net

import com.android.master.kyc.net.model.AppResponse
import io.reactivex.Observable
import retrofit2.http.GET

interface APIService {
    @GET("/api/v1/device/themes/theme-widget")
    fun checkImage(): Observable<AppResponse>
}