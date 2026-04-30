package com.example.lpc_origin_app.model
import com.example.lpc_origin_app.model.*
import com.example.lpc_origin_app.repo.*
import com.example.lpc_origin_app.ui.view.*
import com.example.lpc_origin_app.ui.viewmodel.*
import com.example.lpc_origin_app.utils.*
import com.example.lpc_origin_app.R
import com.example.lpc_origin_app.databinding.*


data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val isRead: Boolean = false,
    val userId: String = "", // Target user ID (empty for system-wide or multiple)
    val iconUrl: String = ""  // URL for car image or user profile image
)


