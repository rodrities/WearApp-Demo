package com.plcoding.wearosstopwatch.presentation

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("aquamarine")
    suspend fun enviarTiempos(): Response<ApiResponse>
    //suspend fun enviarTiempos(@Body tiempos: List<Long>): Response<Void>
}
