package com.example.data

import android.content.Context
import com.example.model.InsightTier
import com.example.model.PriceInsight
import java.io.BufferedReader
import java.io.InputStreamReader

data class PriceBenchmark(
    val keyword: String,
    val category: String,
    val minPrice: Double,
    val avgPrice: Double,
    val maxPrice: Double,
    val storeBenchmark: String
)

object PriceInsightRepository {

    private val benchmarks = mutableListOf<PriceBenchmark>()
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized && benchmarks.isNotEmpty()) return
        try {
            context.assets.open("price_reference.csv").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLine() // skip header
                    var line = reader.readLine()
                    while (line != null) {
                        val parts = line.split(",")
                        if (parts.size >= 6) {
                            benchmarks.add(
                                PriceBenchmark(
                                    keyword = parts[0].trim().lowercase(),
                                    category = parts[1].trim(),
                                    minPrice = parts[2].trim().toDoubleOrNull() ?: 1.0,
                                    avgPrice = parts[3].trim().toDoubleOrNull() ?: 5.0,
                                    maxPrice = parts[4].trim().toDoubleOrNull() ?: 10.0,
                                    storeBenchmark = parts[5].trim()
                                )
                            )
                        }
                        line = reader.readLine()
                    }
                }
            }
            initialized = true
        } catch (e: Exception) {
            e.printStackTrace()
            initialized = true
        }
    }

    fun evaluate(itemName: String, tagPrice: Double): PriceInsight {
        val lower = itemName.lowercase()
        val match = benchmarks.firstOrNull { lower.contains(it.keyword) }
            ?: benchmarks.firstOrNull { it.keyword == "item" }
            ?: PriceBenchmark("item", "General", tagPrice * 0.8, tagPrice * 1.05, tagPrice * 1.4, "Target: $${String.format("%.2f", tagPrice * 0.95)}")

        val avg = match.avgPrice
        val diffRatio = (tagPrice - avg) / avg

        return when {
            diffRatio <= -0.08 -> {
                val pct = (-diffRatio * 100).toInt()
                PriceInsight(
                    tier = InsightTier.GOOD_DEAL,
                    summary = "🟢 Good deal — $pct% below typical average",
                    detail = "Typical average is $${String.format("%.2f", avg)} for ${match.category}.",
                    benchmarkStore = match.storeBenchmark
                )
            }
            diffRatio >= 0.10 -> {
                val diffAmount = tagPrice - avg
                PriceInsight(
                    tier = InsightTier.HIGH_PRICE,
                    summary = "🔴 High — you'd pay $${String.format("%.2f", diffAmount)} less at typical retail",
                    detail = "Benchmark: ${match.storeBenchmark}",
                    benchmarkStore = match.storeBenchmark
                )
            }
            else -> {
                PriceInsight(
                    tier = InsightTier.FAIR_PRICE,
                    summary = "🟡 Fair price — in line with typical retail",
                    detail = "Average market price is $${String.format("%.2f", avg)}.",
                    benchmarkStore = match.storeBenchmark
                )
            }
        }
    }
}
