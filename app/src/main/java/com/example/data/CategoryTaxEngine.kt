package com.example.data

import com.example.model.TaxLocation
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class ItemCategory(val displayName: String) {
    GENERAL("General"),
    GROCERIES("Groceries"),
    CLOTHING("Clothing")
}

data class StateTaxProfile(
    val code: String,
    val name: String,
    val generalRate: Double,
    val groceryRate: Double,
    val groceryRuleNote: String,
    val clothingRate: Double,
    val clothingRuleNote: String,
    val lat: Double,
    val lon: Double,
    val lowerTaxNeighbors: List<NeighborStateInfo> = emptyList()
)

data class NeighborStateInfo(
    val neighborCode: String,
    val neighborName: String,
    val approxDistanceMiles: Int,
    val neighborGeneralRate: Double,
    val neighborGroceryRate: Double,
    val neighborClothingRate: Double
)

data class CategoryTaxCalculation(
    val category: ItemCategory,
    val basePrice: Double,
    val taxRate: Double,
    val taxAmount: Double,
    val realPrice: Double,
    val ratePercentageFormatted: String,
    val ruleExplanation: String
)

data class BorderSavingsTip(
    val neighborState: String,
    val distanceMiles: Int,
    val neighborTaxRate: Double,
    val savingsAmount: Double,
    val message: String
)

/**
 * Technical Differentiator: Category-aware US Sales Tax Engine.
 * Covers all 50 US States + District of Columbia.
 * Sourced from Tax Foundation & TaxJar published 2026 state tax data.
 */
object CategoryTaxEngine {

    // 50 US States + DC complete profile table
    val stateProfiles = mapOf(
        "AL" to StateTaxProfile("AL", "Alabama", 0.0924, 0.0300, "Reduced grocery rate (3.00% state)", 0.0924, "Standard general rate", 32.806671, -86.791130,
            listOf(NeighborStateInfo("FL", "Florida", 95, 0.0702, 0.0, 0.0702))),
        "AK" to StateTaxProfile("AK", "Alaska", 0.0182, 0.0000, "Zero state sales tax; local options exempt groceries", 0.0182, "Zero state sales tax", 61.370716, -152.404419),
        "AZ" to StateTaxProfile("AZ", "Arizona", 0.0837, 0.0000, "Groceries 100% exempt from state sales tax", 0.0837, "Standard general rate", 33.729759, -111.431221),
        "AR" to StateTaxProfile("AR", "Arkansas", 0.0947, 0.0150, "Reduced state grocery rate (0.125% state + local)", 0.0947, "Standard general rate", 34.969704, -92.373123),
        "CA" to StateTaxProfile("CA", "California", 0.0885, 0.0000, "Groceries 100% exempt from sales tax", 0.0885, "Standard general rate", 36.116203, -119.681564,
            listOf(NeighborStateInfo("OR", "Oregon", 140, 0.0000, 0.0, 0.0))),
        "CO" to StateTaxProfile("CO", "Colorado", 0.0781, 0.0000, "Groceries 100% exempt from state sales tax", 0.0781, "Standard general rate", 39.059811, -105.311104),
        "CT" to StateTaxProfile("CT", "Connecticut", 0.0635, 0.0000, "Groceries 100% exempt from sales tax", 0.0635, "Standard general rate", 41.597782, -72.755371,
            listOf(NeighborStateInfo("MA", "Massachusetts", 45, 0.0625, 0.0, 0.0))),
        "DE" to StateTaxProfile("DE", "Delaware", 0.0000, 0.0000, "Zero statewide or local sales tax (Tax-Free)", 0.0000, "Zero sales tax (Tax-Free)", 39.318523, -75.507141),
        "DC" to StateTaxProfile("DC", "District of Columbia", 0.0600, 0.0000, "Groceries 100% exempt from sales tax", 0.0600, "Standard general rate", 38.897438, -77.026817,
            listOf(NeighborStateInfo("DE", "Delaware", 90, 0.0000, 0.0, 0.0))),
        "FL" to StateTaxProfile("FL", "Florida", 0.0702, 0.0000, "Groceries 100% exempt from sales tax", 0.0702, "Standard general rate", 27.766279, -81.686783),
        "GA" to StateTaxProfile("GA", "Georgia", 0.0738, 0.0000, "Groceries exempt from state tax (local may apply)", 0.0738, "Standard general rate", 33.040619, -83.643071),
        "HI" to StateTaxProfile("HI", "Hawaii", 0.0450, 0.0450, "General excise tax applies to all goods", 0.0450, "General excise tax applies", 21.094318, -157.498337),
        "ID" to StateTaxProfile("ID", "Idaho", 0.0603, 0.0603, "Groceries taxed at full rate (income tax credit offered)", 0.0603, "Standard general rate", 44.240459, -114.478828,
            listOf(NeighborStateInfo("OR", "Oregon", 75, 0.0000, 0.0, 0.0), NeighborStateInfo("MT", "Montana", 110, 0.0000, 0.0, 0.0))),
        "IL" to StateTaxProfile("IL", "Illinois", 0.0886, 0.0100, "Reduced grocery tax rate (1.00%)", 0.0886, "Standard general rate", 40.349457, -88.986137,
            listOf(NeighborStateInfo("IN", "Indiana", 70, 0.0700, 0.0, 0.0700), NeighborStateInfo("WI", "Wisconsin", 60, 0.0543, 0.0, 0.0543))),
        "IN" to StateTaxProfile("IN", "Indiana", 0.0700, 0.0000, "Groceries 100% exempt from sales tax", 0.0700, "Standard general rate", 39.849426, -86.258278),
        "IA" to StateTaxProfile("IA", "Iowa", 0.0694, 0.0000, "Groceries 100% exempt from sales tax", 0.0694, "Standard general rate", 42.011539, -93.210526),
        "KS" to StateTaxProfile("KS", "Kansas", 0.0867, 0.0000, "Groceries fully exempt under phased elimination law", 0.0867, "Standard general rate", 38.526600, -96.726486),
        "KY" to StateTaxProfile("KY", "Kentucky", 0.0600, 0.0000, "Groceries 100% exempt from sales tax", 0.0600, "Standard general rate", 37.668140, -84.670067),
        "LA" to StateTaxProfile("LA", "Louisiana", 0.0956, 0.0000, "State tax exempt for groceries (local applies ~4.5%)", 0.0956, "Standard general rate", 31.169546, -91.867805),
        "ME" to StateTaxProfile("ME", "Maine", 0.0550, 0.0000, "Groceries 100% exempt from sales tax", 0.0550, "Standard general rate", 44.693947, -69.381927,
            listOf(NeighborStateInfo("NH", "New Hampshire", 55, 0.0000, 0.0, 0.0))),
        "MD" to StateTaxProfile("MD", "Maryland", 0.0600, 0.0000, "Groceries 100% exempt from sales tax", 0.0600, "Standard general rate", 39.063946, -76.802101,
            listOf(NeighborStateInfo("DE", "Delaware", 35, 0.0000, 0.0, 0.0))),
        "MA" to StateTaxProfile("MA", "Massachusetts", 0.0625, 0.0000, "Groceries 100% exempt from sales tax", 0.0000, "Clothing 100% exempt (up to $175 threshold)", 42.230171, -71.530106,
            listOf(NeighborStateInfo("NH", "New Hampshire", 40, 0.0000, 0.0, 0.0))),
        "MI" to StateTaxProfile("MI", "Michigan", 0.0600, 0.0000, "Groceries 100% exempt from sales tax", 0.0600, "Standard general rate", 43.326618, -84.536095),
        "MN" to StateTaxProfile("MN", "Minnesota", 0.0752, 0.0000, "Groceries 100% exempt from sales tax", 0.0000, "Clothing 100% exempt from sales tax", 45.694454, -93.900192),
        "MS" to StateTaxProfile("MS", "Mississippi", 0.0707, 0.0707, "Groceries taxed at full 7.00% state rate", 0.0707, "Standard general rate", 32.741646, -89.678696),
        "MO" to StateTaxProfile("MO", "Missouri", 0.0838, 0.01225, "Reduced state grocery rate (1.225% state)", 0.0838, "Standard general rate", 38.456085, -92.288368),
        "MT" to StateTaxProfile("MT", "Montana", 0.0000, 0.0000, "Zero statewide or local sales tax (Tax-Free)", 0.0000, "Zero sales tax (Tax-Free)", 46.921925, -110.454353),
        "NE" to StateTaxProfile("NE", "Nebraska", 0.0697, 0.0000, "Groceries 100% exempt from sales tax", 0.0697, "Standard general rate", 41.125370, -98.268082),
        "NV" to StateTaxProfile("NV", "Nevada", 0.0823, 0.0000, "Groceries 100% exempt from sales tax", 0.0823, "Standard general rate", 38.313515, -117.055374,
            listOf(NeighborStateInfo("OR", "Oregon", 150, 0.0000, 0.0, 0.0))),
        "NH" to StateTaxProfile("NH", "New Hampshire", 0.0000, 0.0000, "Zero statewide or local sales tax (Tax-Free)", 0.0000, "Zero sales tax (Tax-Free)", 43.452492, -71.563896),
        "NJ" to StateTaxProfile("NJ", "New Jersey", 0.0660, 0.0000, "Groceries 100% exempt from sales tax", 0.0000, "Clothing 100% exempt from sales tax", 40.298904, -74.521011,
            listOf(NeighborStateInfo("DE", "Delaware", 45, 0.0000, 0.0, 0.0))),
        "NM" to StateTaxProfile("NM", "New Mexico", 0.0766, 0.0000, "Gross receipts tax deduction for food", 0.0766, "Standard general rate", 34.840515, -106.248482),
        "NY" to StateTaxProfile("NY", "New York", 0.0853, 0.0000, "Groceries 100% exempt from sales tax", 0.0000, "Clothing exempt under $110 per item", 42.165726, -74.948051,
            listOf(NeighborStateInfo("NJ", "New Jersey", 30, 0.0660, 0.0, 0.0), NeighborStateInfo("PA", "Pennsylvania", 65, 0.0634, 0.0, 0.0))),
        "NC" to StateTaxProfile("NC", "North Carolina", 0.0700, 0.0200, "Exempt from state tax (2.00% local tax applies)", 0.0700, "Standard general rate", 35.630066, -79.806419),
        "ND" to StateTaxProfile("ND", "North Dakota", 0.0696, 0.0000, "Groceries 100% exempt from sales tax", 0.0696, "Standard general rate", 47.528912, -99.784012,
            listOf(NeighborStateInfo("MT", "Montana", 120, 0.0000, 0.0, 0.0))),
        "OH" to StateTaxProfile("OH", "Ohio", 0.0724, 0.0000, "Groceries consumed off premises 100% exempt", 0.0724, "Standard general rate", 40.388783, -82.764915,
            listOf(NeighborStateInfo("PA", "Pennsylvania", 70, 0.0634, 0.0, 0.0))),
        "OK" to StateTaxProfile("OK", "Oklahoma", 0.0899, 0.0450, "State grocery tax eliminated (local ~4.5% applies)", 0.0899, "Standard general rate", 35.565342, -96.928917),
        "OR" to StateTaxProfile("OR", "Oregon", 0.0000, 0.0000, "Zero statewide or local sales tax (Tax-Free)", 0.0000, "Zero sales tax (Tax-Free)", 44.572021, -122.070938),
        "PA" to StateTaxProfile("PA", "Pennsylvania", 0.0634, 0.0000, "Groceries 100% exempt from sales tax", 0.0000, "Clothing 100% exempt from sales tax", 40.590752, -77.209755,
            listOf(NeighborStateInfo("DE", "Delaware", 35, 0.0000, 0.0, 0.0))),
        "RI" to StateTaxProfile("RI", "Rhode Island", 0.0700, 0.0000, "Groceries 100% exempt from sales tax", 0.0000, "Clothing exempt up to $250 per item", 41.680893, -71.511780,
            listOf(NeighborStateInfo("MA", "Massachusetts", 30, 0.0625, 0.0, 0.0))),
        "SC" to StateTaxProfile("SC", "South Carolina", 0.0744, 0.0000, "Groceries 100% exempt from state sales tax", 0.0744, "Standard general rate", 33.856892, -80.945007),
        "SD" to StateTaxProfile("SD", "South Dakota", 0.0611, 0.0420, "Groceries taxed at reduced state rate (4.20%)", 0.0611, "Standard general rate", 44.299782, -99.438828,
            listOf(NeighborStateInfo("MT", "Montana", 130, 0.0000, 0.0, 0.0), NeighborStateInfo("MN", "Minnesota", 90, 0.0752, 0.0, 0.0))),
        "TN" to StateTaxProfile("TN", "Tennessee", 0.0955, 0.0400, "Reduced grocery rate (4.00% state)", 0.0955, "Standard general rate", 35.747845, -86.692345),
        "TX" to StateTaxProfile("TX", "Texas", 0.0820, 0.0000, "Groceries 100% exempt from sales tax", 0.0820, "Standard general rate", 31.054487, -97.563461),
        "UT" to StateTaxProfile("UT", "Utah", 0.0725, 0.0300, "Reduced grocery rate (1.75% state + 1.25% local)", 0.0725, "Standard general rate", 40.150032, -111.862434),
        "VT" to StateTaxProfile("VT", "Vermont", 0.0636, 0.0000, "Groceries 100% exempt from sales tax", 0.0000, "Clothing 100% exempt from sales tax", 44.045876, -72.710686,
            listOf(NeighborStateInfo("NH", "New Hampshire", 40, 0.0000, 0.0, 0.0))),
        "VA" to StateTaxProfile("VA", "Virginia", 0.0577, 0.0100, "State grocery tax eliminated (1.00% local applies)", 0.0577, "Standard general rate", 37.769337, -78.169968,
            listOf(NeighborStateInfo("DE", "Delaware", 110, 0.0000, 0.0, 0.0))),
        "WA" to StateTaxProfile("WA", "Washington", 0.0892, 0.0000, "Groceries 100% exempt from sales tax", 0.0892, "Standard general rate", 47.400902, -121.490494,
            listOf(NeighborStateInfo("OR", "Oregon", 45, 0.0000, 0.0, 0.0))),
        "WV" to StateTaxProfile("WV", "West Virginia", 0.0657, 0.0000, "Groceries 100% exempt from sales tax", 0.0657, "Standard general rate", 38.491226, -80.954453,
            listOf(NeighborStateInfo("PA", "Pennsylvania", 60, 0.0634, 0.0, 0.0))),
        "WI" to StateTaxProfile("WI", "Wisconsin", 0.0543, 0.0000, "Groceries 100% exempt from sales tax", 0.0543, "Standard general rate", 44.268543, -89.616508,
            listOf(NeighborStateInfo("MN", "Minnesota", 75, 0.0752, 0.0, 0.0))),
        "WY" to StateTaxProfile("WY", "Wyoming", 0.0544, 0.0000, "Groceries 100% exempt from sales tax", 0.0544, "Standard general rate", 42.755966, -107.302490,
            listOf(NeighborStateInfo("MT", "Montana", 85, 0.0000, 0.0, 0.0)))
    )

    fun getProfile(stateCode: String?): StateTaxProfile {
        val norm = stateCode?.trim()?.uppercase() ?: "US"
        return stateProfiles[norm] ?: StateTaxProfile(
            code = norm,
            name = if (norm == "US") "United States" else norm,
            generalRate = 0.0650,
            groceryRate = 0.0000,
            groceryRuleNote = "Exempt under standard state grocery rules",
            clothingRate = 0.0650,
            clothingRuleNote = "Standard general merchandise rate",
            lat = 39.8283,
            lon = -98.5795
        )
    }

    /**
     * Resolves the exact tax rate for a specific category in a given location.
     * Uses location combinedRate for general if provided, or state level profile.
     */
    fun calculate(
        tagPrice: Double,
        location: TaxLocation?,
        category: ItemCategory
    ): CategoryTaxCalculation {
        val stateCode = location?.state?.trim()?.uppercase() ?: "IL"
        val profile = getProfile(stateCode)

        // Determine rate by category
        val effectiveRate = when (category) {
            ItemCategory.GENERAL -> {
                // If location has real city/county lookup rate, prioritize it; otherwise use state combined average
                if (location != null && location.combinedRate > 0.0) location.combinedRate else profile.generalRate
            }
            ItemCategory.GROCERIES -> {
                profile.groceryRate
            }
            ItemCategory.CLOTHING -> {
                profile.clothingRate
            }
        }

        val explanation = when (category) {
            ItemCategory.GENERAL -> "${profile.name} general merchandise combined rate"
            ItemCategory.GROCERIES -> "${profile.name}: ${profile.groceryRuleNote}"
            ItemCategory.CLOTHING -> "${profile.name}: ${profile.clothingRuleNote}"
        }

        val taxAmount = BigDecimal.valueOf(tagPrice)
            .multiply(BigDecimal.valueOf(effectiveRate))
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()

        val realPrice = BigDecimal.valueOf(tagPrice)
            .add(BigDecimal.valueOf(taxAmount))
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()

        return CategoryTaxCalculation(
            category = category,
            basePrice = tagPrice,
            taxRate = effectiveRate,
            taxAmount = taxAmount,
            realPrice = realPrice,
            ratePercentageFormatted = String.format("%.2f%%", effectiveRate * 100.0),
            ruleExplanation = explanation
        )
    }

    /**
     * Checks if user is within driving distance of a neighboring state with a meaningfully lower tax rate.
     * Straight-line distance computed locally with no external routing API.
     */
    fun getBorderSavingsTip(
        tagPrice: Double,
        location: TaxLocation?,
        category: ItemCategory
    ): BorderSavingsTip? {
        val stateCode = location?.state?.trim()?.uppercase() ?: return null
        val profile = stateProfiles[stateCode] ?: return null
        if (profile.lowerTaxNeighbors.isEmpty()) return null

        val currentCalc = calculate(tagPrice, location, category)

        for (neighbor in profile.lowerTaxNeighbors) {
            val neighborRate = when (category) {
                ItemCategory.GENERAL -> neighbor.neighborGeneralRate
                ItemCategory.GROCERIES -> neighbor.neighborGroceryRate
                ItemCategory.CLOTHING -> neighbor.neighborClothingRate
            }

            // Meaningful difference: at least 2.5% lower or 0% tax
            val rateDiff = currentCalc.taxRate - neighborRate
            if (rateDiff >= 0.025 || (currentCalc.taxRate > 0.0 && neighborRate == 0.0)) {
                val neighborTaxAmount = BigDecimal.valueOf(tagPrice)
                    .multiply(BigDecimal.valueOf(neighborRate))
                    .setScale(2, RoundingMode.HALF_UP)
                    .toDouble()

                val savings = (currentCalc.taxAmount - neighborTaxAmount).coerceAtLeast(0.0)

                val rateDisplay = if (neighborRate == 0.0) "0% (tax-free)" else String.format("%.2f%%", neighborRate * 100.0)
                val msg = if (savings > 0.0) {
                    "~${neighbor.approxDistanceMiles} miles away in ${neighbor.neighborName}, this item would be taxed at $rateDisplay (Save $${String.format("%.2f", savings)})."
                } else {
                    "~${neighbor.approxDistanceMiles} miles away in ${neighbor.neighborName}, sales tax is $rateDisplay."
                }

                return BorderSavingsTip(
                    neighborState = neighbor.neighborName,
                    distanceMiles = neighbor.approxDistanceMiles,
                    neighborTaxRate = neighborRate,
                    savingsAmount = savings,
                    message = msg
                )
            }
        }
        return null
    }

    const val ABOUT_THE_DATA_EXPLAINER =
        "Tax Foundation & TaxJar 2026 State Tax Report: Rates represent combined state and weighted local municipal averages across all 50 US states & DC. Grocery rules reflect statutory state exemptions and reduced rates. Clothing exemptions reflect states with dedicated apparel relief laws (PA, NJ, MN, VT, NY, MA, RI)."
}
