package com.example.data

import com.example.model.ScannedItem
import com.example.model.ShoppingTrip
import com.example.model.TaxLocation
import kotlin.math.abs

object MoneyCoachAi {

    fun generateAnswer(
        question: String,
        cartItems: List<ScannedItem>,
        taxLocation: TaxLocation,
        budget: Double,
        recentTrips: List<ShoppingTrip>
    ): String {
        val q = question.trim().lowercase()

        val cartTagTotal = cartItems.sumOf { it.tagPrice }
        val cartTaxTotal = cartItems.sumOf { it.taxAmount }
        val cartTrueTotal = cartItems.sumOf { it.truePrice }

        return when {
            q.contains("over budget") || q.contains("am i over") || q.contains("budget") -> {
                val remaining = budget - cartTrueTotal
                if (cartTrueTotal > budget) {
                    val over = cartTrueTotal - budget
                    "⚠️ You are currently $${String.format("%.2f", over)} over your $${String.format("%.2f", budget)} budget! Your real checkout total is $${String.format("%.2f", cartTrueTotal)} (includes $${String.format("%.2f", cartTaxTotal)} tax). Consider removing non-essential items before heading to the register."
                } else {
                    "✅ Great news! You have $${String.format("%.2f", remaining)} left of your $${String.format("%.2f", budget)} budget. Your current cart is $${String.format("%.2f", cartTrueTotal)} with tax included."
                }
            }

            q.contains("good deal") || q.contains("deal") || q.contains("fair") -> {
                val lastItem = cartItems.firstOrNull()
                if (lastItem != null) {
                    val insight = PriceInsightRepository.evaluate(lastItem.name, lastItem.tagPrice)
                    "Looking at ${lastItem.name} at $${String.format("%.2f", lastItem.tagPrice)}:\n${insight.summary}\n${insight.detail}\nAt checkout in ${taxLocation.city}, you will pay $${String.format("%.2f", lastItem.truePrice)} total."
                } else {
                    "Scan an item tag or add something to your cart, and I will analyze if it is a good deal versus typical retail averages."
                }
            }

            q.contains("tax am i paying") || q.contains("tax today") || q.contains("how much tax") || q.contains("total tax") -> {
                if (cartItems.isEmpty()) {
                    "Your cart is currently empty, but in ${taxLocation.city}, ${taxLocation.state}, you pay ${taxLocation.ratePercentageFormatted} combined sales tax on taxable goods."
                } else {
                    "On your current ${cartItems.size} items:\n• Shelf price total: $${String.format("%.2f", cartTagTotal)}\n• Tax added at register: +$${String.format("%.2f", cartTaxTotal)} (${taxLocation.ratePercentageFormatted})\n• Total checkout out-of-pocket: $${String.format("%.2f", cartTrueTotal)}"
                }
            }

            q.contains("cheaper") || q.contains("compare") || (q.contains("or") && q.contains("$")) -> {
                val regex = Regex("""\$?(\d+(?:\.\d{1,2})?)""")
                val matches = regex.findAll(q).map { it.groupValues[1].toDoubleOrNull() }.filterNotNull().toList()
                if (matches.size >= 2) {
                    val p1 = matches[0]
                    val p2 = matches[1]
                    val rate = taxLocation.combinedRate
                    val t1 = p1 * (1 + rate)
                    val t2 = p2 * (1 + rate)
                    val diff = abs(t1 - t2)
                    val cheaper = if (t1 < t2) p1 else p2
                    "With ${taxLocation.ratePercentageFormatted} tax in ${taxLocation.city}:\n• $${String.format("%.2f", p1)} becomes $${String.format("%.2f", t1)}\n• $${String.format("%.2f", p2)} becomes $${String.format("%.2f", t2)}\n\nThe $${String.format("%.2f", cheaper)} option saves you $${String.format("%.2f", diff)} real dollars at checkout."
                } else {
                    "Enter two prices in your question to compare their register checkout totals with local tax."
                }
            }

            q.contains("oregon") || q.contains("save in oregon") || q.contains("tax free") -> {
                if (cartItems.isEmpty()) {
                    "Oregon has 0% sales tax. Shopping in ${taxLocation.city} adds ${taxLocation.ratePercentageFormatted} to taxable purchases."
                } else {
                    "If you bought your current cart in Oregon (0% sales tax):\n• In ${taxLocation.city}: $${String.format("%.2f", cartTrueTotal)}\n• In Oregon: $${String.format("%.2f", cartTagTotal)}\n\nYou would pocket $${String.format("%.2f", cartTaxTotal)} in instant tax savings."
                }
            }

            q.contains("yesterday") || q.contains("past") || q.contains("history") || q.contains("remind me") -> {
                val lastTrip = recentTrips.firstOrNull()
                if (lastTrip != null) {
                    val itemsList = lastTrip.items.take(3).joinToString(", ") { "${it.name} ($${String.format("%.2f", it.tagPrice)})" }
                    "In your recent ${lastTrip.name} at ${lastTrip.storeName}:\nYou bought ${lastTrip.items.size} items including: $itemsList.\nTotal was $${String.format("%.2f", lastTrip.totalTruePrice)} (including $${String.format("%.2f", lastTrip.totalTaxAmount)} sales tax)."
                } else {
                    "You do not have any saved shopping trips in your history yet! Once you save a cart or scan a receipt, I will remember it here."
                }
            }

            q.contains("why") && (q.contains("high") || q.contains("tax")) -> {
                "Here in ${taxLocation.city}, ${taxLocation.county} County:\n• State of ${taxLocation.state}: ${taxLocation.stateRateFormatted}\n• ${taxLocation.county} County: ${taxLocation.countyRateFormatted}\n• ${taxLocation.city} Municipal: ${taxLocation.cityRateFormatted}\n= ${taxLocation.ratePercentageFormatted} Total.\nUnlike Europe or Asia, US prices exclude sales tax by law because rates vary across 13,000+ local jurisdictions."
            }

            q.contains("hello") || q.contains("hi") || q.contains("hey") -> {
                "Hey! I am your TrueTag Shopping Money Coach. Ask me: 'Is this a good deal?', 'How much tax today?', or 'Am I over budget?'"
            }

            else -> {
                "In ${taxLocation.city}, combined sales tax is ${taxLocation.ratePercentageFormatted}. Your live cart has ${cartItems.size} items totaling $${String.format("%.2f", cartTrueTotal)} ($${String.format("%.2f", cartTaxTotal)} tax). Ask me for price comparisons, budget checks, or deal ratings."
            }
        }
    }
}
