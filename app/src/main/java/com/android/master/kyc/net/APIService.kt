package com.android.master.kyc.net

import com.android.master.kyc.net.model.response.PhotosResponse
import com.android.master.kyc.net.model.request.PhotosRequest
import io.reactivex.Observable
import retrofit2.http.*

interface APIService {
    @POST("/ekyc/v1/images:annotate")
    fun getDetailsFromPhotos(
        @HeaderMap header: Map<String, String>,
        @Body photosRequest: PhotosRequest
    ): Observable<PhotosResponse>
}