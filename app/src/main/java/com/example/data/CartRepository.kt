package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ScannedItem
import com.example.model.ShoppingTrip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

object CartRepository {

    private const val PREFS_NAME = "truetag_cart_prefs"
    private const val KEY_CART_ITEMS = "cart_items_json"
    private const val KEY_SAVED_TRIPS = "saved_trips_json"

    private val _cartItems = MutableStateFlow<List<ScannedItem>>(emptyList())
    val cartItems: StateFlow<List<ScannedItem>> = _cartItems.asStateFlow()

    private val _savedTrips = MutableStateFlow<List<ShoppingTrip>>(emptyList())
    val savedTrips: StateFlow<List<ShoppingTrip>> = _savedTrips.asStateFlow()

    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Load cart
        val cartJson = prefs.getString(KEY_CART_ITEMS, null)
        if (!cartJson.isNullOrBlank()) {
            _cartItems.value = parseCartItems(cartJson)
        } else {
            // Cart starts clean and empty on initial install
            _cartItems.value = emptyList()
            saveCartToDisk(context)
        }

        // Load trips
        val tripsJson = prefs.getString(KEY_SAVED_TRIPS, null)
        if (!tripsJson.isNullOrBlank()) {
            _savedTrips.value = parseTrips(tripsJson)
        } else {
            _savedTrips.value = emptyList()
            saveTripsToDisk(context)
        }

        initialized = true
    }

    fun addItem(context: Context, item: ScannedItem) {
        val updated = _cartItems.value.toMutableList()
        updated.add(0, item) // add at top
        _cartItems.value = updated
        saveCartToDisk(context)
    }

    fun updateItemName(context: Context, itemId: String, newName: String) {
        val updated = _cartItems.value.map {
            if (it.id == itemId) it.copy(name = newName) else it
        }
        _cartItems.value = updated
        saveCartToDisk(context)
    }

    fun removeItem(context: Context, itemId: String) {
        val updated = _cartItems.value.filterNot { it.id == itemId }
        _cartItems.value = updated
        saveCartToDisk(context)
    }

    fun clearCart(context: Context) {
        _cartItems.value = emptyList()
        saveCartToDisk(context)
    }

    fun saveCurrentTrip(context: Context, tripName: String = "Shopping Trip"): ShoppingTrip? {
        val currentItems = _cartItems.value
        if (currentItems.isEmpty()) return null

        val trip = ShoppingTrip(
            name = tripName,
            date = System.currentTimeMillis(),
            items = currentItems
        )

        val updated = _savedTrips.value.toMutableList()
        updated.add(0, trip)
        _savedTrips.value = updated
        saveTripsToDisk(context)

        return trip
    }

    // Weekly tax paid in the current calendar month
    // Returns list of 4 weekly totals: [Week 1, Week 2, Week 3, Week 4]
    fun getMonthlyTaxData(): List<Double> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val weeklyTotals = DoubleArray(4) { 0.0 }

        for (trip in _savedTrips.value) {
            calendar.timeInMillis = trip.date
            if (calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear) {
                val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                val weekIndex = ((dayOfMonth - 1) / 7).coerceIn(0, 3)
                weeklyTotals[weekIndex] += trip.totalTaxAmount
            }
        }

        return weeklyTotals.toList()
    }

    fun getTotalTaxThisMonth(): Double {
        val data = getMonthlyTaxData()
        return data.sum()
    }

    fun clearTrips(context: Context) {
        _savedTrips.value = emptyList()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SAVED_TRIPS)
            .apply()
    }

    private fun saveCartToDisk(context: Context) {
        val array = JSONArray()
        for (item in _cartItems.value) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("tagPrice", item.tagPrice)
            obj.put("taxRate", item.taxRate)
            obj.put("cityName", item.cityName)
            obj.put("timestamp", item.timestamp)
            array.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CART_ITEMS, array.toString())
            .apply()
    }

    private fun saveTripsToDisk(context: Context) {
        val array = JSONArray()
        for (trip in _savedTrips.value) {
            val tripObj = JSONObject()
            tripObj.put("id", trip.id)
            tripObj.put("name", trip.name)
            tripObj.put("date", trip.date)

            val itemsArray = JSONArray()
            for (item in trip.items) {
                val itemObj = JSONObject()
                itemObj.put("id", item.id)
                itemObj.put("name", item.name)
                itemObj.put("tagPrice", item.tagPrice)
                itemObj.put("taxRate", item.taxRate)
                itemObj.put("cityName", item.cityName)
                itemObj.put("timestamp", item.timestamp)
                itemsArray.put(itemObj)
            }
            tripObj.put("items", itemsArray)
            array.put(tripObj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SAVED_TRIPS, array.toString())
            .apply()
    }

    private fun parseCartItems(jsonStr: String): List<ScannedItem> {
        val list = mutableListOf<ScannedItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ScannedItem(
                        id = obj.optString("id"),
                        name = obj.optString("name", "Scanned Item"),
                        tagPrice = obj.optDouble("tagPrice", 0.0),
                        taxRate = obj.optDouble("taxRate", 0.0825),
                        cityName = obj.optString("cityName", "Store Location"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseTrips(jsonStr: String): List<ShoppingTrip> {
        val list = mutableListOf<ShoppingTrip>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val tripObj = array.getJSONObject(i)
                val itemsJson = tripObj.optJSONArray("items")
                val items = mutableListOf<ScannedItem>()
                if (itemsJson != null) {
                    for (j in 0 until itemsJson.length()) {
                        val itemObj = itemsJson.getJSONObject(j)
                        items.add(
                            ScannedItem(
                                id = itemObj.optString("id"),
                                name = itemObj.optString("name", "Item"),
                                tagPrice = itemObj.optDouble("tagPrice", 0.0),
                                taxRate = itemObj.optDouble("taxRate", 0.0825),
                                cityName = itemObj.optString("cityName", "Store Location"),
                                timestamp = itemObj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                }
                list.add(
                    ShoppingTrip(
                        id = tripObj.optString("id"),
                        name = tripObj.optString("name", "Trip"),
                        date = tripObj.optLong("date", System.currentTimeMillis()),
                        items = items
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
