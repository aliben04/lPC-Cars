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
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lpc_origin_app.databinding.ActivityHistoryBinding
import com.example.lpc_origin_app.databinding.ItemAwaitingReservationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.btnBack.setOnClickListener { finish() }

        checkUserRole()
        setupBottomNav()
    }

    private fun checkUserRole() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            isAdmin = doc.getString("type") == "Admin"
            fetchHistory()
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.navHistory.setImageResource(R.drawable.history_icon)
        binding.bottomNav.navHistory.setColorFilter(getColor(R.color.black))
        
        binding.bottomNav.navHome.setColorFilter(getColor(R.color.text_gray))
        
        binding.bottomNav.navHome.setOnClickListener {
            val intent = if (isAdmin) Intent(this, AdminHomeActivity::class.java)
                         else Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }
        binding.bottomNav.navFavorites.setOnClickListener {
            startActivity(Intent(this, FavouriteActivity::class.java))
        }
        binding.bottomNav.navNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        binding.bottomNav.navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun fetchHistory() {
        val userId = auth.currentUser?.uid ?: return
        
        val query = if (isAdmin) {
            db.collection("bookings").whereEqualTo("isPaid", true)
        } else {
            db.collection("bookings")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isPaid", true)
        }

        query.addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener
                val bookings = snapshots.toObjects(Booking::class.java).sortedByDescending { it.timestamp }
                binding.rvHistory.adapter = HistoryAdapter(bookings)
            }
    }

    inner class HistoryAdapter(private val bookings: List<Booking>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
        inner class ViewHolder(val binding: ItemAwaitingReservationBinding) : RecyclerView.ViewHolder(binding.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemAwaitingReservationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val booking = bookings[position]
            holder.binding.tvCarName.text = booking.carName
            holder.binding.tvAmount.text = "${booking.totalAmount} MAD"
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            holder.binding.tvDate.text = sdf.format(Date(booking.timestamp))


            if (booking.carImageUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context).load(booking.carImageUrl).into(holder.binding.ivCarImage)
            }


        }
        override fun getItemCount() = bookings.size
    }
}


