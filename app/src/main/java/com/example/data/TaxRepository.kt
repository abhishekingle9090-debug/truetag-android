package com.example.data

import android.content.Context
import com.example.model.TaxLocation
import java.io.BufferedReader
import java.io.InputStreamReader

object TaxRepository {

    private val cachedLocations = mutableListOf<TaxLocation>()
    private var initialized = false

    // 50 US State Base Sales Tax statutory rates
    private val stateBaseRates = mapOf(
        "AL" to 0.0400, "AK" to 0.0000, "AZ" to 0.0560, "AR" to 0.0650, "CA" to 0.0725,
        "CO" to 0.0290, "CT" to 0.0635, "DE" to 0.0000, "FL" to 0.0600, "GA" to 0.0400,
        "HI" to 0.0400, "ID" to 0.0600, "IL" to 0.0625, "IN" to 0.0700, "IA" to 0.0600,
        "KS" to 0.0650, "KY" to 0.0600, "LA" to 0.0445, "ME" to 0.0550, "MD" to 0.0600,
        "MA" to 0.0625, "MI" to 0.0600, "MN" to 0.06875,"MS" to 0.0700, "MO" to 0.04225,
        "MT" to 0.0000, "NE" to 0.0550, "NV" to 0.0685, "NH" to 0.0000, "NJ" to 0.06625,
        "NM" to 0.0500, "NY" to 0.0400, "NC" to 0.0475, "ND" to 0.0500, "OH" to 0.0575,
        "OK" to 0.0450, "OR" to 0.0000, "PA" to 0.0600, "RI" to 0.0700, "SC" to 0.0600,
        "SD" to 0.0450, "TN" to 0.0700, "TX" to 0.0625, "UT" to 0.0610, "VT" to 0.0600,
        "VA" to 0.0530, "WA" to 0.0650, "WV" to 0.0600, "WI" to 0.0500, "WY" to 0.0400,
        "DC" to 0.0600
    )

    fun initialize(context: Context) {
        if (initialized && cachedLocations.isNotEmpty()) return

        try {
            context.assets.open("tax_rates.csv").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    // Read header line
                    reader.readLine()
                    var line = reader.readLine()
                    while (line != null) {
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty()) {
                            val parts = trimmed.split(",")
                            if (parts.size >= 5) {
                                val zip = parts[0].trim()
                                val state = parts[1].trim()
                                val county = parts[2].trim()
                                val city = parts[3].trim()
                                val rate = parts[4].trim().toDoubleOrNull() ?: 0.0
                                val stateRate = if (parts.size > 5) parts[5].trim().toDoubleOrNull() ?: (rate * 0.6) else (rate * 0.6)
                                val countyRate = if (parts.size > 6) parts[6].trim().toDoubleOrNull() ?: (rate * 0.2) else (rate * 0.2)
                                val cityRate = if (parts.size > 7) parts[7].trim().toDoubleOrNull() ?: (rate * 0.2) else (rate * 0.2)

                                cachedLocations.add(
                                    TaxLocation(
                                        zip = zip,
                                        state = state,
                                        county = county,
                                        city = city,
                                        combinedRate = rate,
                                        stateRate = stateRate,
                                        countyRate = countyRate,
                                        cityRate = cityRate,
                                        streetAddress = ""
                                    )
                                )
                            }
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

    fun getAllLocations(): List<TaxLocation> = cachedLocations.toList()

    fun findByZip(zip: String): TaxLocation? {
        val cleanZip = zip.take(5)
        return cachedLocations.firstOrNull { it.zip == cleanZip }
    }

    fun findByCity(city: String, state: String? = null): TaxLocation? {
        val match = cachedLocations.firstOrNull { loc ->
            loc.city.equals(city, ignoreCase = true) &&
                    (state == null || loc.state.equals(state, ignoreCase = true))
        }
        if (match != null) return match

        return cachedLocations.firstOrNull { loc ->
            loc.city.contains(city, ignoreCase = true) || city.contains(loc.city, ignoreCase = true)
        }
    }

    fun findByState(stateCode: String?): TaxLocation? {
        if (stateCode.isNullOrBlank()) return null
        return cachedLocations.firstOrNull { it.state.equals(stateCode, ignoreCase = true) }
    }

    /**
     * Resolves tax for a real US location without falling back to any default city.
     */
    fun resolveTaxForUsLocation(
        city: String,
        state: String,
        county: String,
        zip: String,
        street: String
    ): TaxLocation {
        // 1. Try exact ZIP lookup
        if (zip.isNotBlank()) {
            val byZip = findByZip(zip)
            if (byZip != null) {
                return byZip.copy(
                    city = if (city.isNotBlank()) city else byZip.city,
                    streetAddress = street
                )
            }
        }

        // 2. Try city and state lookup
        if (city.isNotBlank()) {
            val byCity = findByCity(city, state)
            if (byCity != null) {
                return byCity.copy(
                    zip = if (zip.isNotBlank()) zip else byCity.zip,
                    streetAddress = street
                )
            }
        }

        // 3. Fallback to state statutory rates with real detected city & county
        val stateNormalized = state.trim().uppercase()
        val stateRate = stateBaseRates[stateNormalized] ?: (findByState(state)?.stateRate ?: 0.05)
        // Average local add-on (county + city) in the state, zero for tax-free states
        val localAddon = when (stateNormalized) {
            "OR", "NH", "DE", "MT", "AK" -> 0.0
            else -> 0.0225 // ~2.25% average US municipal/county add-on
        }
        val combined = stateRate + localAddon

        return TaxLocation(
            zip = zip,
            state = state,
            county = county.ifBlank { "County" },
            city = city.ifBlank { "Local Area" },
            combinedRate = combined,
            stateRate = stateRate,
            countyRate = localAddon * 0.5,
            cityRate = localAddon * 0.5,
            streetAddress = street
        )
    }
}
