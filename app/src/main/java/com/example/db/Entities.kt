package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val tagPrice: Double,
    val taxRate: Double,
    val cityName: String,
    val timestamp: Long,
    val category: String,
    val insightJson: String?
)

@Entity(tableName = "shopping_trips")
data class ShoppingTripEntity(
    @PrimaryKey val id: String,
    val name: String,
    val storeName: String,
    val date: Long,
    val totalTagPrice: Double,
    val totalTaxAmount: Double,
    val totalTruePrice: Double,
    val itemsJson: String
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long
)

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = 1,
    val totalScans: Int = 0,
    val totalTaxSavedOrTracked: Double = 0.0,
    val streakDays: Int = 1,
    val budgetAmount: Double = 50.0,
    val isDemoModeActive: Boolean = false,
    val themeMode: String = "SYSTEM" // SYSTEM, DARK, LIGHT
)
