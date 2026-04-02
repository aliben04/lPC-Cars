package com.example.lpc_origin_app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.lpc_origin_app.databinding.ActivitySearchBinding
import com.example.lpc_origin_app.databinding.ItemCarSearchBinding
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import android.content.res.ColorStateList
import android.graphics.Color
import com.google.firebase.firestore.FirebaseFirestore

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var favouriteCarDocs = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        // observe favourites
        observeFavourites()

        // load default cars
        fetchRecommendedCars()

        // search live
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    searchCars(query)
                } else {
                    fetchRecommendedCars()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
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
                // Notify adapter to refresh the heart icons
                binding.rvRecommend.adapter?.notifyDataSetChanged()
            }
    }

    private fun fetchRecommendedCars() {
        db.collection("cars")
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                val cars = documents.toObjects(Car::class.java)
                binding.rvRecommend.adapter = SearchCarAdapter(cars)
            }
    }

    private fun searchCars(query: String) {
        db.collection("cars")
            .get()
            .addOnSuccessListener { documents ->
                val cars = documents.toObjects(Car::class.java)
                    .filter {
                        it.brand.contains(query, true) ||
                                it.model.contains(query, true)
                    }

                binding.rvRecommend.adapter = SearchCarAdapter(cars)
            }
    }

    inner class SearchCarAdapter(private val cars: List<Car>) :
        RecyclerView.Adapter<SearchCarAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemCarSearchBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemCarSearchBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val car = cars[position]

            holder.binding.tvCarName.text = "${car.brand} ${car.model}"
            holder.binding.tvPrice.text = "${car.pricePerDay} MAD/Day"

            if (car.imageUrls.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(car.imageUrls[0])
                    .into(holder.binding.ivCar)
            }

            holder.itemView.setOnClickListener {
                val intent = Intent(this@SearchActivity, CarDetailsActivity::class.java)
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
                } else {
                    val data = hashMapOf(
                        "userId" to userId,
                        "carId" to car.id
                    )
                    db.collection("favourites").add(data)
                }
            }
        }

        override fun getItemCount() = cars.size
    }
}