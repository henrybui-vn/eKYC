package com.android.master.kyc.net

import com.android.master.kyc.net.model.response.PhotosResponse
import com.android.master.kyc.net.model.request.PhotosRequest
import com.android.master.kyc.net.model.request.ScanFaceRequest
import com.android.master.kyc.net.model.response.ScanFaceResponse
import com.android.master.kyc.net.model.response.ScanResponse
import io.reactivex.Observable
import retrofit2.http.*

interface APIService {
    @POST("/ekyc/v1/images:annotate")
    fun getDetailsFromPhotos(
        @HeaderMap header: Map<String, String>,
        @Body photosRequest: PhotosRequest
    ): Observable<PhotosResponse>

    @POST("/ekyc/v1/images:liveness")
    fun scanFaces(
        @HeaderMap header: Map<String, String>,
        @Body scanFaceRequest: ScanFaceRequest
    ): Observable<ScanResponse>
}