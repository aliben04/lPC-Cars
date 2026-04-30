package com.example.lpc_origin_app.model
import com.example.lpc_origin_app.model.*
import com.example.lpc_origin_app.repo.*
import com.example.lpc_origin_app.ui.view.*
import com.example.lpc_origin_app.ui.viewmodel.*
import com.example.lpc_origin_app.utils.*
import com.example.lpc_origin_app.R
import com.example.lpc_origin_app.databinding.*


data class Car(
    val id: String = "",
    val brand: String = "",
    val model: String = "",
    val registration: String = "",
    val pricePerDay: String = "",
    val imageUrls: List<String> = emptyList(),
    val description: String = "",
    val fuelType: String = "",
    val features: Map<String, String> = emptyMap(),
    val status: String = "Available", // "Available" or "Not Available"
    val favouriteCount: Int = 0,
    val createdAt: Long = 0
)


