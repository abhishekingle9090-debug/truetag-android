package com.example.data

import com.example.model.ReceiptScanResult
import com.example.model.ScannedItem
import com.example.model.TaxLocation
import java.util.regex.Pattern

object ReceiptParser {

    private val pricePattern = Pattern.compile("""(?:\$|\b)(\d{1,4}\.\d{2})\b""")

    fun parseReceiptText(text: String, taxLocation: TaxLocation): ReceiptScanResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        var detectedStore = "Store"
        val knownStores = listOf("Target", "Trader Joe's", "Costco", "Walmart", "Whole Foods", "Walgreens", "CVS", "Kroger", "Aldi", "Safeway")
        for (store in knownStores) {
            if (lines.take(6).any { it.contains(store, ignoreCase = true) }) {
                detectedStore = store
                break
            }
        }

        // Try extracting subtotal, tax, total
        var subtotal = 0.0
        var taxPaid = 0.0
        var total = 0.0

        val lineItems = mutableListOf<ScannedItem>()

        for (line in lines) {
            val lower = line.lowercase()
            val matcher = pricePattern.matcher(line)
            val pricesOnLine = mutableListOf<Double>()
            while (matcher.find()) {
                val p = matcher.group(1)?.toDoubleOrNull()
                if (p != null) pricesOnLine.add(p)
            }

            if (pricesOnLine.isNotEmpty()) {
                val linePrice = pricesOnLine.last()

                if (lower.contains("subtotal") || lower.contains("sub total")) {
                    subtotal = linePrice
                } else if (lower.contains("tax") || lower.contains("sales tax")) {
                    taxPaid = linePrice
                } else if (lower.contains("total") || lower.contains("balance") || lower.contains("amount due")) {
                    total = linePrice
                } else if (linePrice in 0.50..350.00 && lineItems.size < 12) {
                    // Extract item description by removing numbers and special chars
                    var itemName = line.replace(Regex("""\$?\d{1,4}\.\d{2}"""), "")
                        .replace(Regex("""[^a-zA-Z\s]"""), " ")
                        .trim()
                    if (itemName.length < 3) {
                        itemName = "Scanned Item #${lineItems.size + 1}"
                    }
                    lineItems.add(
                        ScannedItem(
                            name = itemName.take(24),
                            tagPrice = linePrice,
                            taxRate = taxLocation.combinedRate,
                            cityName = taxLocation.displayCityState,
                            insight = PriceInsightRepository.evaluate(itemName, linePrice)
                        )
                    )
                }
            }
        }

        // If total not found, sum line items
        if (total == 0.0) {
            if (subtotal > 0.0) {
                taxPaid = if (taxPaid > 0.0) taxPaid else (subtotal * taxLocation.combinedRate)
                total = subtotal + taxPaid
            } else if (lineItems.isNotEmpty()) {
                subtotal = lineItems.sumOf { it.tagPrice }
                taxPaid = subtotal * taxLocation.combinedRate
                total = subtotal + taxPaid
            }
        }

        if (taxPaid == 0.0 && total > 0.0) {
            // Auto split using local combined rate: total = subtotal * (1 + rate)
            subtotal = total / (1.0 + taxLocation.combinedRate)
            taxPaid = total - subtotal
        }

        return ReceiptScanResult(
            storeName = detectedStore,
            items = lineItems,
            subtotal = subtotal,
            taxPaid = taxPaid,
            total = total
        )
    }
}
