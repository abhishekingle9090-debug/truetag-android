package com.example.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

data class TaxLocation(
    val zip: String,
    val state: String,
    val county: String,
    val city: String,
    val combinedRate: Double,
    val stateRate: Double = 0.0625,
    val countyRate: Double = 0.0175,
    val cityRate: Double = 0.0225,
    val streetAddress: String = ""
) {
    val ratePercentageFormatted: String
        get() = String.format("%.2f%%", combinedRate * 100.0)

    val stateRateFormatted: String
        get() = String.format("%.2f%%", stateRate * 100.0)

    val countyRateFormatted: String
        get() = String.format("%.2f%%", countyRate * 100.0)

    val cityRateFormatted: String
        get() = String.format("%.2f%%", cityRate * 100.0)

    val displayCityState: String
        get() = "$city, $state"

    val displayChip: String
        get() = "📍 $city, $state · $county County · $ratePercentageFormatted"

    val fullFormattedAddress: String
        get() = if (streetAddress.isNotBlank()) {
            "$streetAddress, $city, $state $zip"
        } else {
            "$city, $state $zip ($county County)"
        }

    val whyExplainer: String
        get() = if (combinedRate == 0.0) {
            "$state has 0% statewide and local sales tax under state law."
        } else {
            "In $city, the total sales tax of $ratePercentageFormatted is composed of: $state State tax ($stateRateFormatted) + $county County tax ($countyRateFormatted) + $city Municipal tax ($cityRateFormatted). Sales tax is mandated by state and municipal statutes and added at checkout."
        }
}

enum class InsightTier {
    GOOD_DEAL,
    FAIR_PRICE,
    HIGH_PRICE
}

data class PriceInsight(
    val tier: InsightTier,
    val summary: String,
    val detail: String,
    val benchmarkStore: String = ""
)

data class ScannedItem(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    val tagPrice: Double,
    val taxRate: Double,
    val cityName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "General",
    val insight: PriceInsight? = null
) {
    val taxAmount: Double
        get() = BigDecimal.valueOf(tagPrice)
            .multiply(BigDecimal.valueOf(taxRate))
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()

    val truePrice: Double
        get() = BigDecimal.valueOf(tagPrice)
            .add(BigDecimal.valueOf(taxAmount))
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()

    val formattedTagPrice: String
        get() = String.format("$%.2f", tagPrice)

    val formattedTaxAmount: String
        get() = String.format("$%.2f", taxAmount)

    val formattedTruePrice: String
        get() = String.format("$%.2f", truePrice)
}

data class ShoppingTrip(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var storeName: String = "General Store",
    val date: Long = System.currentTimeMillis(),
    val items: List<ScannedItem>
) {
    val totalTagPrice: Double
        get() = items.map { BigDecimal.valueOf(it.tagPrice) }.fold(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP).toDouble()

    val totalTaxAmount: Double
        get() = items.map { BigDecimal.valueOf(it.taxAmount) }.fold(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP).toDouble()

    val totalTruePrice: Double
        get() = items.map { BigDecimal.valueOf(it.truePrice) }.fold(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP).toDouble()

    val formattedTotalTagPrice: String
        get() = String.format("$%.2f", totalTagPrice)

    val formattedTotalTaxAmount: String
        get() = String.format("$%.2f", totalTaxAmount)

    val formattedTotalTruePrice: String
        get() = String.format("$%.2f", totalTruePrice)
}

data class ReceiptScanResult(
    val storeName: String,
    val items: List<ScannedItem>,
    val subtotal: Double,
    val taxPaid: Double,
    val total: Double
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class UserBadge(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val unlocked: Boolean
)

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val createdAt: String = "",
    val profileImage: String = "",
    val preferredCurrency: String = "USD",
    val budgetPreference: Double = 100.0
)

data class UserSettings(
    val budgetAmount: Double = 100.0,
    val preferredCurrency: String = "USD",
    val themeMode: String = "SYSTEM",
    val autoSaveTrips: Boolean = true,
    val updatedAt: String = ""
)
