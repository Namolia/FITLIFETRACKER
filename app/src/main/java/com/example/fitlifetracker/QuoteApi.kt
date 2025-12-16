package com.example.fitlifetracker

import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class AdviceSlipResponse(
    val slip: Slip? = null
)

data class Slip(
    val advice: String? = null
)

interface AdviceService {
    @GET("advice")
    suspend fun getAdvice(): AdviceSlipResponse
}

object QuoteApi {
    private const val BASE_URL = "https://api.adviceslip.com/"

    // ✅ Cache sorununu kırmak için request'e no-cache ekliyoruz
    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val newReq: Request = original.newBuilder()
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .build()
                chain.proceed(newReq)
            }
            .build()
    }

    val service: AdviceService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AdviceService::class.java)
    }
}
