package com.example.receipeapp2.Signup


import com.google.gson.annotations.SerializedName

data class SignupModel(
    @SerializedName("success") val success: Boolean,
    @SerializedName("statusCode") val statusCode: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: UserData
)

data class UserData(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("device_token") val deviceToken: String,
    @SerializedName("device_type") val deviceType: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("id") val id: Int
)
