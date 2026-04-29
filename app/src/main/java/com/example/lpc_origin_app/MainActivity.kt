package com.example.lpc_origin_app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.content.res.ColorStateList
import android.graphics.Color
import com.bumptech.glide.Glide
import com.example.lpc_origin_app.databinding.ActivityMainBinding
import com.example.lpc_origin_app.databinding.ItemBrandBinding
import com.example.lpc_origin_app.databinding.ItemCarAvailableBinding
import com.example.lpc_origin_app.databinding.ItemCarUnavailableBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var favouriteCarDocs = mutableMapOf<String, String>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            startTrackingIfBookingActive()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        fetchBrands()
        fetchCars()
        observeNotifications()
        observeFavourites()
        setupBottomNav()
        checkLocationPermissions()
        binding.btnSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
    }

    private fun checkLocationPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            startTrackingIfBookingActive()
        }
    }

    private fun startTrackingIfBookingActive() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("bookings")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "Live")
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val carId = documents.documents[0].getString("carId")
                    if (carId != null) {
                        val serviceIntent = Intent(this, LocationService::class.java)
                        serviceIntent.putExtra("CAR_ID", carId)
                        ContextCompat.startForegroundService(this, serviceIntent)
                    }
                }
            }
    }

    private fun setupBottomNav() {
        binding.bottomNav.navHome.setImageResource(R.drawable.homepage_icon)
        binding.bottomNav.navHome.setColorFilter(getColor(R.color.black))

        binding.bottomNav.navHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
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

    override fun onResume() {
        super.onResume()
        loadUserProfile()
        autoUpdateExpiredBookings()
    }

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val imageUrl = document.getString("profileImageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(imageUrl)
                            .placeholder(android.R.drawable.ic_menu_myplaces)
                            .error(android.R.drawable.ic_menu_myplaces)
                            .circleCrop()
                            .into(binding.ivProfile)
                    }
                }
            }
    }

    private fun setupClickListeners() {
        binding.ivProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        binding.flNotification.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
    }

    private fun observeNotifications() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener
                
                val count = snapshots.size()
                if (count > 0) {
                    binding.tvNotificationBadge.visibility = View.VISIBLE
                    binding.tvNotificationBadge.text = count.toString()
                } else {
                    binding.tvNotificationBadge.visibility = View.GONE
                }
            }
    }

    private fun observeFavourites() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("favourites")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener
                favouriteCarDocs.clear()
                for (doc in snapshots.documents) {
                    val carId = doc.getString("carId")
                    if (carId != null) {
                        favouriteCarDocs[carId] = doc.id
                    }
                }
                // Notify adapters to refresh the heart icons
                binding.rvAvailableCars.adapter?.notifyDataSetChanged()
            }
    }

    private fun fetchBrands() {
        binding.pbBrands.visibility = View.VISIBLE
        binding.pbBrands.alpha = 1f
        binding.rvBrands.visibility = View.GONE
        binding.rvBrands.alpha = 0f

        db.collection("brands").addSnapshotListener { snapshots, e ->
            if (e != null) return@addSnapshotListener
            
            val brandList = snapshots?.documents?.mapNotNull { doc ->
                val name = doc.getString("name")
                val image = doc.getString("image")

                if (name != null && image != null) {
                    brands(name, image)
                } else null
            } ?: emptyList()

            binding.rvBrands.adapter = BrandAdapter(brandList)
            
            binding.pbBrands.animate().alpha(0f).setDuration(300).withEndAction {
                binding.pbBrands.visibility = View.GONE
            }
            binding.rvBrands.visibility = View.VISIBLE
            binding.rvBrands.animate().alpha(1f).setDuration(300).start()
        }
    }

    private fun autoUpdateExpiredBookings() {
        val now = System.currentTimeMillis()
        // Query both 'Live' and 'Pending' bookings that have expired
        db.collection("bookings")
            .whereLessThan("returnDate", now)
            .get()
            .addOnSuccessListener { snapshots ->
                for (doc in snapshots.documents) {
                    val status = doc.getString("status") ?: ""
                    if (status == "Live" || status == "Pending") {
                        val carId = doc.getString("carId")
                        val bookingId = doc.id
                        
                        if (carId != null) {
                            // Mark booking as completed (or cancelled if never paid)
                            val nextStatus = if (status == "Live") "Completed" else "Cancelled"
                            db.collection("bookings").document(bookingId).update("status", nextStatus)
                            
                            // Mark car as available
                            db.collection("cars").document(carId).update("status", "Available")
                        }
                    }
                }
            }
    }

    private fun fetchCars() {
        binding.pbAvailableCars.visibility = View.VISIBLE
        binding.pbAvailableCars.alpha = 1f
        binding.rvAvailableCars.visibility = View.GONE
        binding.rvAvailableCars.alpha = 0f

        db.collection("cars").addSnapshotListener { snapshots, e ->
            if (e != null) return@addSnapshotListener
            val allCars = snapshots?.toObjects(Car::class.java) ?: emptyList()
            
            val availableCars = allCars.filter { it.status == "Available" }
            val unavailableCars = allCars.filter { it.status != "Available" }

            binding.rvAvailableCars.adapter = AvailableCarAdapter(availableCars)
            binding.rvNotAvailableCars.adapter = UnavailableCarAdapter(unavailableCars)
            
            binding.pbAvailableCars.animate().alpha(0f).setDuration(300).withEndAction {
                binding.pbAvailableCars.visibility = View.GONE
            }
            binding.rvAvailableCars.visibility = View.VISIBLE
            binding.rvAvailableCars.animate().alpha(1f).setDuration(300).start()
        }
    }

    inner class BrandAdapter(private val brands: List<brands>) : RecyclerView.Adapter<BrandAdapter.ViewHolder>() {
        inner class ViewHolder(val binding: ItemBrandBinding) : RecyclerView.ViewHolder(binding.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemBrandBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.binding.tvBrandName.text = brands[position].name
            Glide.with(holder.itemView.context)
                .load(brands[position].image)
                .placeholder(android.R.color.white)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.binding.ivBrandLogo)
        }
        override fun getItemCount() = brands.size
    }

    inner class AvailableCarAdapter(private val cars: List<Car>) : RecyclerView.Adapter<AvailableCarAdapter.ViewHolder>() {
        inner class ViewHolder(val binding: ItemCarAvailableBinding) : RecyclerView.ViewHolder(binding.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemCarAvailableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val car = cars[position]
            holder.binding.tvCarName.text = "${car.brand} ${car.model}"
            holder.binding.tvPrice.text = "${car.pricePerDay} MAD/Day"
            holder.binding.tvFavouriteCount.text = car.favouriteCount.toString()
            
            if (car.imageUrls.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(car.imageUrls[0])
                    .placeholder(R.color.bg_light_gray)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.binding.ivCar)
            }

            holder.itemView.setOnClickListener {
                val intent = Intent(this@MainActivity, CarDetailsActivity::class.java)
                intent.putExtra("CAR_ID", car.id)
                startActivity(intent)
            }

            if (favouriteCarDocs.containsKey(car.id)) {
                holder.binding.ivFavourite.imageTintList = ColorStateList.valueOf(Color.RED)
            } else {
                holder.binding.ivFavourite.imageTintList = ColorStateList.valueOf(Color.GRAY)
            }

            holder.binding.ivFavourite.setOnClickListener {
                val userId = auth.currentUser?.uid ?: return@setOnClickListener
                val existingDocId = favouriteCarDocs[car.id]

                if (existingDocId != null) {
                    db.collection("favourites").document(existingDocId).delete()
                        .addOnSuccessListener {
                            db.collection("cars").document(car.id).update("favouriteCount", FieldValue.increment(-1))
                        }
                } else {
                    val data = hashMapOf(
                        "userId" to userId,
                        "carId" to car.id
                    )
                    db.collection("favourites").add(data)
                        .addOnSuccessListener {
                            db.collection("cars").document(car.id).update("favouriteCount", FieldValue.increment(1))
                        }
                }
            }
        }
        override fun getItemCount() = cars.size
    }

    inner class UnavailableCarAdapter(private val cars: List<Car>) : RecyclerView.Adapter<UnavailableCarAdapter.ViewHolder>() {
        inner class ViewHolder(val binding: ItemCarUnavailableBinding) : RecyclerView.ViewHolder(binding.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemCarUnavailableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val car = cars[position]
            holder.binding.tvCarNameLarge.text = "${car.brand} ${car.model}"
            holder.binding.tvPriceLarge.text = "${car.pricePerDay} MAD/Day"
            holder.binding.tvFavouriteCountLarge.text = car.favouriteCount.toString()

            if (favouriteCarDocs.containsKey(car.id)) {
                holder.binding.ivFavouriteLarge.imageTintList = ColorStateList.valueOf(Color.RED)
            } else {
                holder.binding.ivFavouriteLarge.imageTintList = ColorStateList.valueOf(Color.GRAY)
            }

            holder.binding.ivFavouriteLarge.setOnClickListener {
                val userId = auth.currentUser?.uid ?: return@setOnClickListener
                val existingDocId = favouriteCarDocs[car.id]

                if (existingDocId != null) {
                    db.collection("favourites").document(existingDocId).delete()
                        .addOnSuccessListener {
                            db.collection("cars").document(car.id).update("favouriteCount", FieldValue.increment(-1))
                        }
                } else {
                    val data = hashMapOf(
                        "userId" to userId,
                        "carId" to car.id
                    )
                    db.collection("favourites").add(data)
                        .addOnSuccessListener {
                            db.collection("cars").document(car.id).update("favouriteCount", FieldValue.increment(1))
                        }
                }
            }
            
            if (car.imageUrls.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(car.imageUrls[0])
                    .placeholder(R.color.bg_light_gray)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.binding.ivCarLarge)
            }
            holder.itemView.setOnClickListener {
                val intent = Intent(this@MainActivity, CarDetailsActivity::class.java)
                intent.putExtra("CAR_ID", car.id)
                startActivity(intent)
            }
        }
        override fun getItemCount() = cars.size
    }
}
