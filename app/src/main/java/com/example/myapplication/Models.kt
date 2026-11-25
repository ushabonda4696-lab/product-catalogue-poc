package com.example.myapplication

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: Int,
    val title: String,
    val price: Double,
    val description: String,
    val category: String,
    val image: String,
    val rating: Rating
)

@Serializable
data class Rating(
    val rate: Double,
    val count: Int
)

@Serializable
data class EnquiryRequest(
    val productId: Int,
    val productTitle: String,
    val name: String,
    val email: String,
    val message: String = ""
)

@Serializable
data class EnquiryResponse(
    val message: String
)