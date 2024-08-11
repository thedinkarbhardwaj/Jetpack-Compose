package com.example.receipeapp2

import com.google.gson.annotations.SerializedName

data class CategoryApiResponse(
    @SerializedName("categories") val categories: List<CategoryResponse>
)



data class CategoryResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("status") val status: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("restaurant") val restaurant: List<Restaurant>
)

data class Restaurant(
    @SerializedName("id") val id: Int,
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String,
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("path") val path: String,
    @SerializedName("status") val status: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("image_url") val imageUrl: String
)
