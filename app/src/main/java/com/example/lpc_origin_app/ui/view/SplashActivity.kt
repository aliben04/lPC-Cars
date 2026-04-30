package com.example.lpc_origin_app.ui.view
import com.example.lpc_origin_app.model.*
import com.example.lpc_origin_app.repo.*
import com.example.lpc_origin_app.ui.view.*
import com.example.lpc_origin_app.ui.viewmodel.*
import com.example.lpc_origin_app.utils.*
import com.example.lpc_origin_app.R
import com.example.lpc_origin_app.databinding.*


import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Show splash for 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginStatus()
        }, 3000)
    }

    private fun checkLoginStatus() {
        val prefs = getSharedPreferences("LPC_PREFS", MODE_PRIVATE)
        val rememberMe = prefs.getBoolean("remember_me", false)

        if (rememberMe && auth.currentUser != null) {
            val role = prefs.getString("user_role", "User") ?: "User"
            navigateToHome(role)
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun navigateToHome(role: String) {
        if (role == "Admin") {
            startActivity(Intent(this, AdminHomeActivity::class.java))
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}


