package com.weatherapp.apiServices

import com.weatherapp.WeatherModel
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {

    @GET("/v1/current.json")
    suspend fun getWeather(
        @Query("Key") apiKey:String,
        @Query("q") city:String
    ): Response<WeatherModel>
}