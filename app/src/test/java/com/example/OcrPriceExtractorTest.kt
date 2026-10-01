package com.example

import com.example.util.ExtractionResult
import com.example.util.OcrPriceExtractor
import com.example.util.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OcrPriceExtractorTest {

    @Test
    fun testPass1_emptyText() {
        val res = OcrPriceExtractor.validateRawTextAndLines("", emptyList())
        assertTrue(res is ValidationResult.NotAPriceTag)
        assertEquals("No text found in image.", (res as ValidationResult.NotAPriceTag).reason)

        val resSingleChar = OcrPriceExtractor.validateRawTextAndLines("A", listOf("A"))
        assertTrue(resSingleChar is ValidationResult.NotAPriceTag)
        assertEquals("No text found in image.", (resSingleChar as ValidationResult.NotAPriceTag).reason)
    }

    @Test
    fun testPass2_noPriceFound() {
        val raw = "WELCOME TO THE STORE PLEASE ENJOY YOUR VISIT"
        val lines = listOf("WELCOME TO THE STORE", "PLEASE ENJOY YOUR VISIT")
        val res = OcrPriceExtractor.validateRawTextAndLines(raw, lines)
        assertTrue(res is ValidationResult.NotAPriceTag)
        assertEquals("No price found in this image. Make sure the price tag is clearly visible.", (res as ValidationResult.NotAPriceTag).reason)
    }

    @Test
    fun testPass2_rejectedYearOnly() {
        // 2024 is rejected as year
        val raw = "COPYRIGHT 2024 STORE"
        val lines = listOf("COPYRIGHT 2024 STORE")
        val res = OcrPriceExtractor.validateRawTextAndLines(raw, lines)
        assertTrue(res is ValidationResult.NotAPriceTag)
    }

    @Test
    fun testPass3_numberWithoutRetailContextOrCurrency() {
        // Just a random number with no currency and no retail keywords
        val raw = "BUILDING 42 ROOM 12.50"
        val lines = listOf("BUILDING 42", "ROOM 12.50")
        val res = OcrPriceExtractor.validateRawTextAndLines(raw, lines)
        assertTrue(res is ValidationResult.NotAPriceTag)
        assertEquals("This does not look like a price tag. Point the camera at a shelf price or upload a photo of one.", (res as ValidationResult.NotAPriceTag).reason)
    }

    @Test
    fun testRule3_documentIndicatorRejection_textbook() {
        val raw = "CHAPTER 4: THEOREM OF ECONOMIC PRICE $19.99 PAGE 42"
        val lines = listOf("CHAPTER 4", "THEOREM OF ECONOMIC PRICE $19.99", "PAGE 42")
        val res = OcrPriceExtractor.validateRawTextAndLines(raw, lines)
        assertTrue(res is ValidationResult.NotAPriceTag)
        assertEquals("This looks like a document, not a price tag.", (res as ValidationResult.NotAPriceTag).reason)

        // Extraction function also rejects document indicators
        val ext = OcrPriceExtractor.extractFromRawTextAndLines(raw, lines)
        assertTrue(ext is ExtractionResult.Failure)
        assertEquals("This looks like a document, not a price tag.", (ext as ExtractionResult.Failure).reason)
    }

    @Test
    fun testRule2_denseDocumentRejection_moreThan15Lines() {
        val lines = (1..16).map { "Line $it $1.99" }
        val raw = lines.joinToString("\n")
        val res = OcrPriceExtractor.validateRawTextAndLines(raw, lines)
        assertTrue(res is ValidationResult.NotAPriceTag)
        assertEquals("This looks like a document, not a price tag.", (res as ValidationResult.NotAPriceTag).reason)

        val ext = OcrPriceExtractor.extractFromRawTextAndLines(raw, lines)
        assertTrue(ext is ExtractionResult.Failure)
        assertEquals("This looks like a document, not a price tag.", (ext as ExtractionResult.Failure).reason)
    }

    @Test
    fun testSuccessfulValidationAndExtraction_withDollarSign() {
        val lines = listOf("ORGANIC WHOLE MILK", "$4.99", "GALLON")
        val raw = lines.joinToString("\n")
        val valResult = OcrPriceExtractor.validateRawTextAndLines(raw, lines)
        assertTrue(valResult is ValidationResult.Valid)

        val extResult = OcrPriceExtractor.extractFromRawTextAndLines(
            rawText = raw,
            lines = lines,
            lineHeights = listOf(20.0, 35.0, 18.0),
            lineCenterYs = listOf(100.0, 150.0, 200.0),
            imageHeight = 300
        )
        assertTrue(extResult is ExtractionResult.Success)
        val extracted = (extResult as ExtractionResult.Success).extracted
        assertEquals(4.99, extracted.price, 0.001)
        assertTrue(extracted.score >= 45.0)
    }

    @Test
    fun testSuccessfulValidationAndExtraction_withRetailKeywords() {
        // No dollar sign, but two retail keywords: "sale" and "ea"
        val lines = listOf("CEREAL BOX", "SALE 3.50 EA")
        val raw = lines.joinToString("\n")
        val valResult = OcrPriceExtractor.validateRawTextAndLines(raw, lines)
        assertTrue(valResult is ValidationResult.Valid)

        val extResult = OcrPriceExtractor.extractFromRawTextAndLines(
            rawText = raw,
            lines = lines,
            lineHeights = listOf(20.0, 30.0),
            lineCenterYs = listOf(100.0, 150.0),
            imageHeight = 300
        )
        assertTrue(extResult is ExtractionResult.Success)
        val extracted = (extResult as ExtractionResult.Success).extracted
        assertEquals(3.50, extracted.price, 0.001)
    }

    @Test
    fun testRule4_multipleAmbiguousPricesWithoutDollarSign() {
        // Two prices with similar positions and heights, neither having a dollar sign
        val lines = listOf("ITEM SALE", "10.00", "10.50")
        val raw = lines.joinToString("\n")
        val extResult = OcrPriceExtractor.extractFromRawTextAndLines(
            rawText = raw,
            lines = lines,
            lineHeights = listOf(20.0, 24.0, 24.0),
            lineCenterYs = listOf(100.0, 150.0, 152.0),
            imageHeight = 300
        )
        assertTrue(extResult is ExtractionResult.Failure)
        assertEquals("Multiple possible prices found. Crop the image to a single tag.", (extResult as ExtractionResult.Failure).reason)
    }

    @Test
    fun testRule1_scoreBelow45Rejected() {
        // Whole number without decimal (no decimal bonus 60), no dollar sign, and far off center (posScore 0)
        val lines = listOf("price each 5")
        val raw = lines.first()
        val extResult = OcrPriceExtractor.extractFromRawTextAndLines(
            rawText = raw,
            lines = lines,
            lineHeights = listOf(8.0),
            lineCenterYs = listOf(999.0),
            imageHeight = 100
        )
        assertTrue(extResult is ExtractionResult.Failure)
        // Score: posScore(0) + fontScore(24) + currency(0) + decimal(0) = 24.0 < 45.0
        assertEquals("Price too unclear. Try a sharper photo.", (extResult as ExtractionResult.Failure).reason)
    }
}
