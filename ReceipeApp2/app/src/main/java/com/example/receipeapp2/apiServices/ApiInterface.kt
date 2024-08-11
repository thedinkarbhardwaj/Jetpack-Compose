package com.throneride.apiServices

import com.example.receipeapp2.CategoryResponse
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.QueryMap
import retrofit2.http.Url

interface ApiInterface {


    @POST(ApiUrl.API_POST_REGISTER_USER)
    fun registerUser(@Body map: HashMap<String, String>,
                   //  @Part profile_pic: MultipartBody.Part?
    ): Call<JsonObject>

    @POST(ApiUrl.API_POST_LOGIN_USER)
    fun login(@Body map:HashMap<String,String>): Call<JsonObject>

    @GET(ApiUrl.API_GET_CATEGORY)
    fun category(): Call<List<CategoryResponse>>

}