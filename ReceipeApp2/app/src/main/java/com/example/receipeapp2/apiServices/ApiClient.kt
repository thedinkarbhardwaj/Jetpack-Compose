package com.throneride.apiServices

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object ApiClient {

    const val BASE_URL = " https://dev.bosselt.com/backend-assessment/"

    fun api(): ApiInterface {
        val logging = HttpLoggingInterceptor()
        logging.apply { logging.level = HttpLoggingInterceptor.Level.BODY}
        val client = OkHttpClient.Builder()
            .connectTimeout(100, TimeUnit.SECONDS)
            .writeTimeout(2.toLong() * 60, TimeUnit.SECONDS)
            .readTimeout(2.toLong() * 60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder().baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()).client(client)
            .build().create(ApiInterface::class.java)
    }
}