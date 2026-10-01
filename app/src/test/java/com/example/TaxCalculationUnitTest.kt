package com.example

import com.example.model.ScannedItem
import org.junit.Assert.assertEquals
import org.junit.Test

class TaxCalculationUnitTest {

    @Test
    fun testChicagoTaxCalculation() {
        // Shelf price $10.00 in Chicago (10.25% tax) -> Tax $1.03 (1.025 rounds to 1.03), True Price $11.03
        val item = ScannedItem(
            name = "Test Item",
            tagPrice = 10.00,
            taxRate = 0.1025,
            cityName = "Chicago, IL"
        )

        assertEquals(1.03, item.taxAmount, 0.001)
        assertEquals(11.03, item.truePrice, 0.001)
    }

    @Test
    fun testZeroTaxState() {
        // Portland, Oregon (0% tax) -> Tax $0.00, True Price equals tag price
        val item = ScannedItem(
            name = "Coffee",
            tagPrice = 4.50,
            taxRate = 0.0,
            cityName = "Portland, OR"
        )

        assertEquals(0.00, item.taxAmount, 0.001)
        assertEquals(4.50, item.truePrice, 0.001)
    }
}
