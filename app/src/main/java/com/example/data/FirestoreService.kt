package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.ScannedItem
import com.example.model.ShoppingTrip
import com.example.model.UserProfile
import com.example.model.UserSettings
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cloud Firestore service for isolated user data persistence:
 * - /users/{uid}
 * - /users/{uid}/shoppingTrips/{tripId}
 * - /users/{uid}/shoppingTrips/{tripId}/items/{itemId}
 * - /users/{uid}/savedCarts/{cartId}
 * - /users/{uid}/preferences/settings
 */
class FirestoreService(private val context: Context) {

    private val databaseId: String by lazy {
        try {
            context.getString(R.string.firestore_database_id)
        } catch (e: Exception) {
            "ai-studio-android-truetag-9ae6b438-1cc6-4f5e-af8c-455c801142b0"
        }
    }

    val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(databaseId)
    }

    private fun getCurrentIsoDate(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
    }

    // =========================================================================
    // USER PROFILE (/users/{uid})
    // =========================================================================

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            val userMap = hashMapOf<String, Any>(
                "name" to profile.name,
                "email" to profile.email,
                "createdAt" to (profile.createdAt.ifBlank { getCurrentIsoDate() }),
                "profileImage" to profile.profileImage,
                "preferredCurrency" to profile.preferredCurrency,
                "budgetPreference" to profile.budgetPreference
            )
            db.collection("users").document(profile.uid)
                .set(userMap, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveUserProfile error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getUserProfileOnce(uid: String): UserProfile? {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) {
                UserProfile(
                    uid = uid,
                    name = doc.getString("name") ?: "",
                    email = doc.getString("email") ?: "",
                    createdAt = doc.getString("createdAt") ?: "",
                    profileImage = doc.getString("profileImage") ?: "",
                    preferredCurrency = doc.getString("preferredCurrency") ?: "USD",
                    budgetPreference = doc.getDouble("budgetPreference") ?: 100.0
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "getUserProfileOnce error: ${e.message}")
            null
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val docRef = db.collection("users").document(uid)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "observeUserProfile error: ${error.message}")
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val profile = UserProfile(
                    uid = uid,
                    name = snapshot.getString("name") ?: "",
                    email = snapshot.getString("email") ?: "",
                    createdAt = snapshot.getString("createdAt") ?: "",
                    profileImage = snapshot.getString("profileImage") ?: "",
                    preferredCurrency = snapshot.getString("preferredCurrency") ?: "USD",
                    budgetPreference = snapshot.getDouble("budgetPreference") ?: 100.0
                )
                trySend(profile)
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun updateUserProfile(
        uid: String,
        name: String,
        preferredCurrency: String = "USD",
        budgetPreference: Double = 100.0
    ): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "name" to name,
                "preferredCurrency" to preferredCurrency,
                "budgetPreference" to budgetPreference
            )
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateUserProfile error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // SHOPPING TRIPS (/users/{uid}/shoppingTrips/{tripId})
    // =========================================================================

    suspend fun saveShoppingTrip(uid: String, trip: ShoppingTrip): Result<Unit> {
        return try {
            val tripRef = db.collection("users").document(uid)
                .collection("shoppingTrips").document(trip.id)

            val tripData = hashMapOf<String, Any>(
                "id" to trip.id,
                "storeName" to trip.storeName,
                "date" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(trip.date)),
                "total" to trip.totalTruePrice,
                "taxPaid" to trip.totalTaxAmount,
                "subtotal" to trip.totalTagPrice,
                "itemCount" to trip.items.size,
                "notes" to trip.name,
                "createdAt" to getCurrentIsoDate()
            )
            tripRef.set(tripData, SetOptions.merge()).await()

            // Save individual items into subcollection /items/{itemId}
            val itemsBatch = db.batch()
            for (item in trip.items) {
                val itemRef = tripRef.collection("items").document(item.id)
                val itemMap = hashMapOf<String, Any>(
                    "id" to item.id,
                    "name" to item.name,
                    "tagPrice" to item.tagPrice,
                    "taxRate" to item.taxRate,
                    "taxAmount" to item.taxAmount,
                    "truePrice" to item.truePrice,
                    "category" to item.category,
                    "createdAt" to getCurrentIsoDate()
                )
                itemsBatch.set(itemRef, itemMap, SetOptions.merge())
            }
            itemsBatch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveShoppingTrip error: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun observeShoppingTrips(uid: String): Flow<List<ShoppingTrip>> = callbackFlow {
        val tripsRef = db.collection("users").document(uid).collection("shoppingTrips")
        val registration = tripsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "observeShoppingTrips error: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = mutableListOf<ShoppingTrip>()
            if (snapshot != null) {
                for (doc in snapshot.documents) {
                    val id = doc.getString("id") ?: doc.id
                    val storeName = doc.getString("storeName") ?: "General Store"
                    val notes = doc.getString("notes") ?: "Trip"
                    list.add(
                        ShoppingTrip(
                            id = id,
                            name = notes,
                            storeName = storeName,
                            date = System.currentTimeMillis(),
                            items = emptyList()
                        )
                    )
                }
            }
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    suspend fun deleteShoppingTrip(uid: String, tripId: String): Result<Unit> {
        return try {
            val tripRef = db.collection("users").document(uid).collection("shoppingTrips").document(tripId)
            // Delete items in subcollection
            val itemsSnap = tripRef.collection("items").get().await()
            val batch = db.batch()
            for (itemDoc in itemsSnap.documents) {
                batch.delete(itemDoc.reference)
            }
            batch.delete(tripRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteShoppingTrip error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // SAVED CARTS (/users/{uid}/savedCarts/{cartId})
    // =========================================================================

    suspend fun saveActiveCart(uid: String, items: List<ScannedItem>): Result<Unit> {
        return try {
            val cartRef = db.collection("users").document(uid).collection("savedCarts").document("active")
            val itemsList = items.map { item ->
                hashMapOf(
                    "id" to item.id,
                    "name" to item.name,
                    "tagPrice" to item.tagPrice,
                    "taxRate" to item.taxRate,
                    "cityName" to item.cityName,
                    "category" to item.category,
                    "timestamp" to item.timestamp
                )
            }
            val totalTag = items.sumOf { it.tagPrice }
            val totalTax = items.sumOf { it.taxAmount }
            val totalReal = items.sumOf { it.truePrice }

            val data = hashMapOf<String, Any>(
                "id" to "active",
                "name" to "Active Shopping Cart",
                "items" to itemsList,
                "totalTagPrice" to totalTag,
                "totalTax" to totalTax,
                "totalRealPrice" to totalReal,
                "updatedAt" to getCurrentIsoDate()
            )
            cartRef.set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveActiveCart error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getActiveCartOnce(uid: String): List<ScannedItem> {
        return try {
            val doc = db.collection("users").document(uid).collection("savedCarts").document("active").get().await()
            if (doc.exists()) {
                val rawItems = doc.get("items") as? List<Map<String, Any?>> ?: emptyList()
                rawItems.mapNotNull { map ->
                    val id = map["id"] as? String ?: return@mapNotNull null
                    val name = map["name"] as? String ?: "Item"
                    val tagPrice = (map["tagPrice"] as? Number)?.toDouble() ?: 0.0
                    val taxRate = (map["taxRate"] as? Number)?.toDouble() ?: 0.0825
                    val cityName = map["cityName"] as? String ?: "Local"
                    val category = map["category"] as? String ?: "General"
                    val timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    ScannedItem(
                        id = id,
                        name = name,
                        tagPrice = tagPrice,
                        taxRate = taxRate,
                        cityName = cityName,
                        category = category,
                        timestamp = timestamp
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "getActiveCartOnce error: ${e.message}")
            emptyList()
        }
    }

    // =========================================================================
    // USER PREFERENCES / SETTINGS (/users/{uid}/preferences/settings)
    // =========================================================================

    suspend fun saveUserSettings(uid: String, settings: UserSettings): Result<Unit> {
        return try {
            val data = hashMapOf<String, Any>(
                "budgetAmount" to settings.budgetAmount,
                "preferredCurrency" to settings.preferredCurrency,
                "themeMode" to settings.themeMode,
                "autoSaveTrips" to settings.autoSaveTrips,
                "updatedAt" to getCurrentIsoDate()
            )
            db.collection("users").document(uid)
                .collection("preferences").document("settings")
                .set(data, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveUserSettings error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getUserSettingsOnce(uid: String): UserSettings? {
        return try {
            val doc = db.collection("users").document(uid)
                .collection("preferences").document("settings")
                .get().await()
            if (doc.exists()) {
                UserSettings(
                    budgetAmount = doc.getDouble("budgetAmount") ?: 100.0,
                    preferredCurrency = doc.getString("preferredCurrency") ?: "USD",
                    themeMode = doc.getString("themeMode") ?: "SYSTEM",
                    autoSaveTrips = doc.getBoolean("autoSaveTrips") ?: true,
                    updatedAt = doc.getString("updatedAt") ?: ""
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "getUserSettingsOnce error: ${e.message}")
            null
        }
    }

    // =========================================================================
    // DELETE ALL USER DATA
    // =========================================================================

    suspend fun deleteUserData(uid: String): Result<Unit> {
        return try {
            val userRef = db.collection("users").document(uid)

            // 1. Delete trips and items
            val trips = userRef.collection("shoppingTrips").get().await()
            val batch = db.batch()
            for (tripDoc in trips.documents) {
                val items = tripDoc.reference.collection("items").get().await()
                for (item in items.documents) {
                    batch.delete(item.reference)
                }
                batch.delete(tripDoc.reference)
            }

            // 2. Delete saved carts
            val carts = userRef.collection("savedCarts").get().await()
            for (cart in carts.documents) {
                batch.delete(cart.reference)
            }

            // 3. Delete preferences
            val prefs = userRef.collection("preferences").get().await()
            for (pref in prefs.documents) {
                batch.delete(pref.reference)
            }

            // 4. Delete root user doc
            batch.delete(userRef)
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteUserData error: ${e.message}", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "FirestoreService"

        @Volatile
        private var instance: FirestoreService? = null

        fun getInstance(context: Context): FirestoreService {
            return instance ?: synchronized(this) {
                instance ?: FirestoreService(context.applicationContext).also { instance = it }
            }
        }
    }
}
