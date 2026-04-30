package com.example.lpc_origin_app.model
import com.example.lpc_origin_app.model.*
import com.example.lpc_origin_app.repo.*
import com.example.lpc_origin_app.ui.view.*
import com.example.lpc_origin_app.ui.viewmodel.*
import com.example.lpc_origin_app.utils.*
import com.example.lpc_origin_app.R
import com.example.lpc_origin_app.databinding.*


data class User(
    val uid: String = "",
    val full_name: String = "",
    val email: String = "",
    val type: String = "User", // "User" or "Admin"
    val cin: String = "",
    val phone_number: String = "",
    val permis_number: String = "",
    val profileImageUrl: String = "",
    val pass: String =""
)


