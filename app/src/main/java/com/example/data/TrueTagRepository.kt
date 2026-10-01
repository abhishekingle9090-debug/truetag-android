package com.example.data

import android.content.Context
import com.example.db.AppDatabase
import com.example.db.CartItemEntity
import com.example.db.ChatMessageEntity
import com.example.db.ShoppingTripEntity
import com.example.db.UserStatsEntity
import com.example.model.ChatMessage
import com.example.model.InsightTier
import com.example.model.PriceInsight
import com.example.model.ScannedItem
import com.example.model.ShoppingTrip
import com.example.model.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class TrueTagRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val cartDao = db.cartItemDao()
    private val tripDao = db.shoppingTripDao()
    private val chatDao = db.chatMessageDao()
    private val statsDao = db.userStatsDao()
    private val firestoreService = FirestoreService.getInstance(context)

    suspend fun syncUserDataFromFirestore(uid: String) {
        try {
            val remoteCart = firestoreService.getActiveCartOnce(uid)
            if (remoteCart.isNotEmpty()) {
                cartDao.clearCart()
                for (item in remoteCart) {
                    val insightJson = item.insight?.let { insight ->
                        JSONObject().apply {
                            put("tier", insight.tier.name)
                            put("summary", insight.summary)
                            put("detail", insight.detail)
                            put("benchmark", insight.benchmarkStore)
                        }.toString()
                    }
                    cartDao.insertItem(
                        CartItemEntity(
                            id = item.id,
                            name = item.name,
                            tagPrice = item.tagPrice,
                            taxRate = item.taxRate,
                            cityName = item.cityName,
                            timestamp = item.timestamp,
                            category = item.category,
                            insightJson = insightJson
                        )
                    )
                }
            }

            val remoteSettings = firestoreService.getUserSettingsOnce(uid)
            if (remoteSettings != null) {
                val currentStats = statsDao.getUserStats().firstOrNull() ?: UserStatsEntity()
                statsDao.insertOrUpdate(
                    currentStats.copy(
                        budgetAmount = remoteSettings.budgetAmount,
                        themeMode = remoteSettings.themeMode
                    )
                )
            }
        } catch (e: Exception) {
            // Non-blocking sync error
        }
    }

    val cartItems: Flow<List<ScannedItem>> = cartDao.getAllCartItems().map { list ->
        list.map { entityToScannedItem(it) }
    }

    val savedTrips: Flow<List<ShoppingTrip>> = tripDao.getAllTrips().map { list ->
        list.map { entityToShoppingTrip(it) }
    }

    val chatMessages: Flow<List<ChatMessage>> = chatDao.getAllMessages().map { list ->
        list.map { ChatMessage(id = it.id, text = it.text, isUser = it.isUser, timestamp = it.timestamp) }
    }

    val userStats: Flow<UserStatsEntity> = statsDao.getUserStats().map {
        it ?: UserStatsEntity()
    }

    suspend fun addCartItem(item: ScannedItem) {
        val insightJson = item.insight?.let { insight ->
            JSONObject().apply {
                put("tier", insight.tier.name)
                put("summary", insight.summary)
                put("detail", insight.detail)
                put("benchmark", insight.benchmarkStore)
            }.toString()
        }

        cartDao.insertItem(
            CartItemEntity(
                id = item.id,
                name = item.name,
                tagPrice = item.tagPrice,
                taxRate = item.taxRate,
                cityName = item.cityName,
                timestamp = item.timestamp,
                category = item.category,
                insightJson = insightJson
            )
        )

        // Increment scan count in stats
        val currentStats = statsDao.getUserStats().firstOrNull() ?: UserStatsEntity()
        statsDao.insertOrUpdate(
            currentStats.copy(
                totalScans = currentStats.totalScans + 1,
                totalTaxSavedOrTracked = currentStats.totalTaxSavedOrTracked + item.taxAmount
            )
        )

        // Sync active cart to Firestore
        AuthHelper.currentUser.value?.uid?.let { uid ->
            CoroutineScope(Dispatchers.IO).launch {
                val current = cartDao.getAllCartItems().firstOrNull()?.map { entityToScannedItem(it) } ?: emptyList()
                firestoreService.saveActiveCart(uid, current)
            }
        }
    }

    suspend fun updateCartItemName(id: String, newName: String) {
        val current = cartDao.getAllCartItems().firstOrNull()?.find { it.id == id }
        if (current != null) {
            cartDao.updateItem(current.copy(name = newName))
            AuthHelper.currentUser.value?.uid?.let { uid ->
                CoroutineScope(Dispatchers.IO).launch {
                    val items = cartDao.getAllCartItems().firstOrNull()?.map { entityToScannedItem(it) } ?: emptyList()
                    firestoreService.saveActiveCart(uid, items)
                }
            }
        }
    }

    suspend fun removeCartItem(id: String) {
        cartDao.deleteById(id)
        AuthHelper.currentUser.value?.uid?.let { uid ->
            CoroutineScope(Dispatchers.IO).launch {
                val items = cartDao.getAllCartItems().firstOrNull()?.map { entityToScannedItem(it) } ?: emptyList()
                firestoreService.saveActiveCart(uid, items)
            }
        }
    }

    suspend fun clearCart() {
        cartDao.clearCart()
        AuthHelper.currentUser.value?.uid?.let { uid ->
            CoroutineScope(Dispatchers.IO).launch {
                firestoreService.saveActiveCart(uid, emptyList())
            }
        }
    }

    suspend fun saveCurrentCartAsTrip(tripName: String, storeName: String = "General Store"): ShoppingTrip? {
        val items = cartDao.getAllCartItems().firstOrNull()?.map { entityToScannedItem(it) } ?: emptyList()
        if (items.isEmpty()) return null

        val trip = ShoppingTrip(
            name = tripName,
            storeName = storeName,
            date = System.currentTimeMillis(),
            items = items
        )

        val itemsArray = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("tagPrice", item.tagPrice)
                put("taxRate", item.taxRate)
                put("cityName", item.cityName)
                put("timestamp", item.timestamp)
                put("category", item.category)
            }
            itemsArray.put(obj)
        }

        tripDao.insertTrip(
            ShoppingTripEntity(
                id = trip.id,
                name = trip.name,
                storeName = trip.storeName,
                date = trip.date,
                totalTagPrice = trip.totalTagPrice,
                totalTaxAmount = trip.totalTaxAmount,
                totalTruePrice = trip.totalTruePrice,
                itemsJson = itemsArray.toString()
            )
        )

        AuthHelper.currentUser.value?.uid?.let { uid ->
            CoroutineScope(Dispatchers.IO).launch {
                firestoreService.saveShoppingTrip(uid, trip)
            }
        }

        return trip
    }

    suspend fun saveReceiptAsTrip(
        storeName: String,
        lineItems: List<ScannedItem>,
        subtotal: Double,
        tax: Double,
        total: Double
    ): ShoppingTrip {
        val trip = ShoppingTrip(
            name = "Receipt · $storeName",
            storeName = storeName,
            date = System.currentTimeMillis(),
            items = lineItems
        )

        val itemsArray = JSONArray()
        for (item in lineItems) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("tagPrice", item.tagPrice)
                put("taxRate", item.taxRate)
                put("cityName", item.cityName)
                put("timestamp", item.timestamp)
                put("category", item.category)
            }
            itemsArray.put(obj)
        }

        tripDao.insertTrip(
            ShoppingTripEntity(
                id = trip.id,
                name = trip.name,
                storeName = trip.storeName,
                date = trip.date,
                totalTagPrice = subtotal,
                totalTaxAmount = tax,
                totalTruePrice = total,
                itemsJson = itemsArray.toString()
            )
        )

        AuthHelper.currentUser.value?.uid?.let { uid ->
            CoroutineScope(Dispatchers.IO).launch {
                firestoreService.saveShoppingTrip(uid, trip)
            }
        }

        return trip
    }

    suspend fun renameTrip(tripId: String, newName: String, newStore: String) {
        tripDao.updateTripName(tripId, newName, newStore)
    }

    suspend fun duplicateTrip(trip: ShoppingTrip) {
        val newTrip = trip.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${trip.name} (Copy)",
            date = System.currentTimeMillis()
        )
        val itemsArray = JSONArray()
        for (item in newTrip.items) {
            val obj = JSONObject().apply {
                put("id", java.util.UUID.randomUUID().toString())
                put("name", item.name)
                put("tagPrice", item.tagPrice)
                put("taxRate", item.taxRate)
                put("cityName", item.cityName)
                put("timestamp", System.currentTimeMillis())
                put("category", item.category)
            }
            itemsArray.put(obj)
        }
        tripDao.insertTrip(
            ShoppingTripEntity(
                id = newTrip.id,
                name = newTrip.name,
                storeName = newTrip.storeName,
                date = newTrip.date,
                totalTagPrice = newTrip.totalTagPrice,
                totalTaxAmount = newTrip.totalTaxAmount,
                totalTruePrice = newTrip.totalTruePrice,
                itemsJson = itemsArray.toString()
            )
        )
    }

    suspend fun deleteTrip(tripId: String) {
        tripDao.deleteById(tripId)
    }

    suspend fun addChatMessage(msg: ChatMessage) {
        chatDao.insertMessage(
            ChatMessageEntity(
                id = msg.id,
                text = msg.text,
                isUser = msg.isUser,
                timestamp = msg.timestamp
            )
        )
    }

    suspend fun clearChat() {
        chatDao.clearMessages()
    }

    suspend fun updateBudget(budget: Double) {
        val currentStats = statsDao.getUserStats().firstOrNull() ?: UserStatsEntity()
        statsDao.insertOrUpdate(currentStats.copy(budgetAmount = budget))
        AuthHelper.currentUser.value?.uid?.let { uid ->
            CoroutineScope(Dispatchers.IO).launch {
                firestoreService.saveUserSettings(
                    uid = uid,
                    settings = UserSettings(
                        budgetAmount = budget,
                        preferredCurrency = "USD",
                        themeMode = currentStats.themeMode,
                        autoSaveTrips = true
                    )
                )
            }
        }
    }

    suspend fun updateThemeMode(mode: String) {
        val currentStats = statsDao.getUserStats().firstOrNull() ?: UserStatsEntity()
        statsDao.insertOrUpdate(currentStats.copy(themeMode = mode))
    }

    suspend fun setDemoMode(active: Boolean) {
        val currentStats = statsDao.getUserStats().firstOrNull() ?: UserStatsEntity()
        statsDao.insertOrUpdate(currentStats.copy(isDemoModeActive = active))
    }

    suspend fun seedInitialDataIfEmpty() {
        val existingStats = statsDao.getUserStats().firstOrNull()
        if (existingStats == null) {
            val now = System.currentTimeMillis()
            // Clean initial state - strictly zero preloaded or invented values
            statsDao.insertOrUpdate(
                UserStatsEntity(
                    id = 1,
                    totalScans = 0,
                    totalTaxSavedOrTracked = 0.0,
                    streakDays = 0,
                    budgetAmount = 100.0
                )
            )

            // Seed initial welcome message from the Shopping Money Coach
            chatDao.insertMessage(
                ChatMessageEntity(
                    id = "welcome_bot_1",
                    text = "Welcome to TrueTag! I can help you understand sales tax and manage your shopping budget. Scan a price tag to begin.",
                    isUser = false,
                    timestamp = now
                )
            )
        }
    }

    suspend fun resetAllDemoData(context: Context) {
        clearCart()
        tripDao.clearTrips()
        clearChat()
        statsDao.insertOrUpdate(
            UserStatsEntity(
                id = 1,
                totalScans = 0,
                totalTaxSavedOrTracked = 0.0,
                streakDays = 0,
                budgetAmount = 100.0
            )
        )
        LocationHelper.disableTestLocation(context)
        CartRepository.clearCart(context)
        CartRepository.clearTrips(context)
    }

    private fun entityToScannedItem(entity: CartItemEntity): ScannedItem {
        var insight: PriceInsight? = null
        if (!entity.insightJson.isNullOrBlank()) {
            try {
                val obj = JSONObject(entity.insightJson)
                insight = PriceInsight(
                    tier = InsightTier.valueOf(obj.optString("tier", InsightTier.FAIR_PRICE.name)),
                    summary = obj.optString("summary", ""),
                    detail = obj.optString("detail", ""),
                    benchmarkStore = obj.optString("benchmark", "")
                )
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }

        return ScannedItem(
            id = entity.id,
            name = entity.name,
            tagPrice = entity.tagPrice,
            taxRate = entity.taxRate,
            cityName = entity.cityName,
            timestamp = entity.timestamp,
            category = entity.category,
            insight = insight
        )
    }

    private fun entityToShoppingTrip(entity: ShoppingTripEntity): ShoppingTrip {
        val items = mutableListOf<ScannedItem>()
        if (entity.itemsJson.isNotBlank()) {
            try {
                val array = JSONArray(entity.itemsJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    items.add(
                        ScannedItem(
                            id = obj.optString("id"),
                            name = obj.optString("name", "Item"),
                            tagPrice = obj.optDouble("tagPrice", 0.0),
                            taxRate = obj.optDouble("taxRate", 0.1025),
                            cityName = obj.optString("cityName", "Store Location"),
                            timestamp = obj.optLong("timestamp", entity.date),
                            category = obj.optString("category", "General")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return ShoppingTrip(
            id = entity.id,
            name = entity.name,
            storeName = entity.storeName,
            date = entity.date,
            items = items
        )
    }
}
