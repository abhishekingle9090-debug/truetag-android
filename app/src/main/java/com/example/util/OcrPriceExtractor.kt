package com.example.util

import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.math.abs

/**
 * Result of image validation before extraction.
 */
sealed class ValidationResult {
    data class Valid(val visionText: Text? = null) : ValidationResult()
    data class NotAPriceTag(val reason: String) : ValidationResult()
}

/**
 * Result of price extraction.
 */
sealed class ExtractionResult {
    data class Success(val extracted: ExtractedPrice) : ExtractionResult()
    data class Failure(val reason: String) : ExtractionResult()
}

/**
 * Result data class for an extracted price and its detected item name.
 */
data class ExtractedPrice(
    val price: Double,
    val itemName: String,
    val score: Double = 0.0
)

/**
 * Internal candidate representation with scoring attributes.
 */
data class PriceCandidate(
    val price: Double,
    val isDecimal: Boolean,
    val originalLine: String,
    val hasDollarSign: Boolean,
    val lineCenterY: Double,
    val fontHeight: Double,
    var score: Double = 0.0
)

/**
 * On-device price and item description extraction pipeline for ML Kit Text Recognition v2.
 * Adheres strictly to the 3-pass validation and 4 extraction rules specified in the requirements.
 *
 * CRITICAL REQUIREMENTS:
 * - Operates strictly on real recognized text from bitmap pixels.
 * - Zero fallback or default values. If no valid price passes the pipeline, returns failure.
 * - Both gallery upload and camera still-capture paths call this exact function.
 */
object OcrPriceExtractor {

    private const val TAG = "TrueTagOCR"

    // Step 3 Patterns:
    // Decimal price pattern: 1 to 4 digits, dot, exactly two digits after decimal (e.g. 4.99, 12.50, 1250.00).
    // Using lookaround (?<!\d) and (?!\d) to ensure boundary is not part of a larger number.
    private val decimalPriceRegex = Regex("""(?<!\d)(\d{1,4}\.\d{2})(?!\d)""")

    // Integer / whole number pattern: 1 to 4 digits with no decimals.
    private val wholeNumberRegex = Regex("""(?<!\d)(\d{1,4})(?!\d)""")

    // PART 2 Rule 3 Document Indicators:
    // chapter, page, figure, table, copyright, isbn, equation, theorem, definition, exercise, question, answer, student, university, lecture, slide, syllabus
    private val documentIndicatorRegex = Regex(
        """(?i)\b(chapter|page|figure|table|copyright|isbn|equation|theorem|definition|exercise|question|answer|student|university|lecture|slide|syllabus)\b"""
    )

    // PART 1 Pass 3 Retail Keywords:
    // price, total, each, ea, sale, reg, was, now, offer, deal, save, tax, item, store, sku, upc, savings, promo, markdown, clear
    private val retailKeywords = listOf(
        "price", "total", "each", "ea", "sale", "reg", "was", "now", "offer", "deal",
        "save", "tax", "item", "store", "sku", "upc", "savings", "promo", "markdown", "clear"
    )

    private fun logD(tag: String, msg: String) {
        try {
            Log.d(tag, msg)
        } catch (_: Throwable) {
            // Safe fallback in JVM unit tests where android.util.Log is unmocked
        }
    }

    /**
     * PART 1: Strict image validation before extraction.
     * Takes a Bitmap and returns a ValidationResult of either Valid or NotAPriceTag with a reason.
     * Must be called on a background thread or coroutine (e.g. Dispatchers.IO).
     */
    fun validatePriceTagImage(bitmap: Bitmap, rotationDegrees: Int = 0): ValidationResult {
        return try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
            val visionText = Tasks.await(recognizer.process(inputImage))
            validateVisionText(visionText, bitmap.height)
        } catch (e: Exception) {
            Log.e(TAG, "validatePriceTagImage failed: ${e.message}", e)
            ValidationResult.NotAPriceTag("Could not process image. Try a clearer photo.")
        }
    }

    /**
     * Runs the 3-pass validation and document checks on ML Kit recognized Text.
     */
    fun validateVisionText(visionText: Text, imageHeight: Int = 0): ValidationResult {
        val rawText = visionText.text
        val allLines = visionText.textBlocks.flatMap { it.lines }.map { it.text }
        val result = validateRawTextAndLines(rawText, allLines, imageHeight)
        return if (result is ValidationResult.Valid) {
            ValidationResult.Valid(visionText)
        } else {
            result
        }
    }

    /**
     * Core 3-pass validation logic operating on raw text and line strings.
     */
    fun validateRawTextAndLines(
        rawText: String,
        lines: List<String>,
        imageHeight: Int = 0
    ): ValidationResult {
        // Requirement 7: Log the raw recognized text to Logcat with tag TrueTagOCR
        logD(TAG, "========== ML Kit Raw Recognized Text (Validation) ==========\n$rawText\n===========================================================")

        // Pass 1. Text presence.
        // Run ML Kit Text Recognition on the image first. If the recognized text is empty
        // or shorter than two characters, return NotAPriceTag with reason "No text found in image."
        if (rawText.trim().length < 2) {
            return ValidationResult.NotAPriceTag("No text found in image.")
        }

        // Rule 3. Document indicators check (reject textbook / lecture / document photos immediately)
        if (documentIndicatorRegex.containsMatchIn(rawText)) {
            logD(TAG, "Validation rejected: Document indicator found in text.")
            return ValidationResult.NotAPriceTag("This looks like a document, not a price tag.")
        }

        // Rule 2. Never return a value if the image contains more than 15 recognized lines of dense text.
        if (lines.size > 15) {
            logD(TAG, "Validation rejected: Image contains ${lines.size} lines (>15 lines).")
            return ValidationResult.NotAPriceTag("This looks like a document, not a price tag.")
        }

        // Pass 2. Price pattern presence.
        // Scan the recognized lines for at least one candidate matching the price patterns from the extraction stage.
        // If no candidate exists anywhere in the image, return NotAPriceTag with reason "No price found in this image. Make sure the price tag is clearly visible."
        val candidates = findValidPriceCandidatesFromLines(lines, imageHeight = imageHeight)
        if (candidates.isEmpty()) {
            return ValidationResult.NotAPriceTag("No price found in this image. Make sure the price tag is clearly visible.")
        }

        // Pass 3. Currency or retail context.
        // Even if a number is found, require at least one of these signals to be present:
        // At least one currency symbol: dollar, USD, or the dollar sign ($)
        // OR at least two of these retail keywords (case-insensitive):
        // price, total, each, ea, sale, reg, was, now, offer, deal, save, tax, item, store, sku, upc, savings, promo, markdown, clear
        val hasCurrencySymbol = rawText.contains("$") ||
                Regex("""(?i)\b(dollar|dollars|usd)\b""").containsMatchIn(rawText)

        var retailKeywordMatches = 0
        for (kw in retailKeywords) {
            val count = Regex("""(?i)\b${Regex.escape(kw)}\b""").findAll(rawText).count()
            retailKeywordMatches += count
        }

        if (!hasCurrencySymbol && retailKeywordMatches < 2) {
            logD(TAG, "Validation rejected: Neither currency symbol nor 2 retail keywords found (matches=$retailKeywordMatches).")
            return ValidationResult.NotAPriceTag("This does not look like a price tag. Point the camera at a shelf price or upload a photo of one.")
        }

        // All three passes succeed!
        return ValidationResult.Valid()
    }

    /**
     * Normalizes a line of text by stripping currency symbols, trailing units, and formatting commas.
     */
    fun normalizeLineText(originalLineText: String): String {
        var normalized = originalLineText
        // Remove currency symbols: $, ¢, €, £, ¥
        normalized = normalized.replace(Regex("""[$¢€£¥]"""), " ")
        // Remove trailing letters like EA, ea, each, EACH, /ea, /lb, lb, pk, ct, oz
        normalized = normalized.replace(Regex("""(?i)(?<=\d)[/]?\s*(ea|each|per\s*ea|per\s*lb|lb|pk|ct|oz)\b"""), " ")
        normalized = normalized.replace(Regex("""(?i)\b(ea|each|per\s*ea|per\s*lb|lb|pk|ct|oz)\b"""), " ")
        // Remove commas used as thousand separators (e.g. 1,250.00 -> 1250.00)
        normalized = normalized.replace(Regex("""(?<=\d),(?=\d{3})"""), "")
        // Normalize European comma decimals if preceded by 1-4 digits and followed by 2 digits
        normalized = normalized.replace(Regex("""(?<=\d),(?=\d{2}(?!\d))"""), ".")
        return normalized.trim()
    }

    /**
     * Finds candidates from line strings.
     */
    fun findValidPriceCandidatesFromLines(
        lines: List<String>,
        lineHeights: List<Double> = emptyList(),
        lineCenterYs: List<Double> = emptyList(),
        imageHeight: Int = 0
    ): List<PriceCandidate> {
        val imgCenterY = if (imageHeight > 0) imageHeight / 2.0 else 0.0
        val rawCandidates = mutableListOf<PriceCandidate>()

        lines.forEachIndexed { index, originalLineText ->
            val lineCenterY = lineCenterYs.getOrNull(index) ?: imgCenterY
            val fontHeight = lineHeights.getOrNull(index) ?: 24.0
            val hasDollarSign = originalLineText.contains("$")

            val normalized = normalizeLineText(originalLineText)

            // Decimal pattern match
            val decimalMatches = decimalPriceRegex.findAll(normalized).toList()
            var foundDecimalOnLine = false

            if (decimalMatches.isNotEmpty()) {
                for (match in decimalMatches) {
                    val numStr = match.groupValues[1]
                    val p = numStr.toDoubleOrNull()
                    if (p != null) {
                        foundDecimalOnLine = true
                        rawCandidates.add(
                            PriceCandidate(
                                price = p,
                                isDecimal = true,
                                originalLine = originalLineText,
                                hasDollarSign = hasDollarSign,
                                lineCenterY = lineCenterY,
                                fontHeight = fontHeight
                            )
                        )
                    }
                }
            }

            // Also accept a whole number with no decimal if it is the only numeric candidate on the line
            if (!foundDecimalOnLine) {
                val allNumberTokens = Regex("""(?<!\d)\d+(?!\d)""").findAll(normalized).map { it.value }.toList()
                if (allNumberTokens.size == 1) {
                    val singleToken = allNumberTokens.first()
                    val wholeMatch = wholeNumberRegex.matchEntire(singleToken)
                    if (wholeMatch != null) {
                        val p = singleToken.toDoubleOrNull()
                        if (p != null) {
                            rawCandidates.add(
                                PriceCandidate(
                                    price = p,
                                    isDecimal = false,
                                    originalLine = originalLineText,
                                    hasDollarSign = hasDollarSign,
                                    lineCenterY = lineCenterY,
                                    fontHeight = fontHeight
                                )
                            )
                        }
                    }
                }
            }
        }

        // Apply Step 5 Rejection Rules
        val validCandidates = mutableListOf<PriceCandidate>()
        for (cand in rawCandidates) {
            val price = cand.price
            val rawLineLower = cand.originalLine.lowercase()

            // 1. Reject any candidate that is a four digit number in 1900..2100 (year)
            if (price >= 1900.0 && price <= 2100.0) continue
            // 2. Reject numbers longer than eight digits
            if (price >= 100_000_000.0) continue
            // Reject lines that contain more than eight consecutive digits (serial/barcode)
            if (Regex("""\d{9,}""").containsMatchIn(cand.originalLine)) continue
            // 3. Reject non-positive prices
            if (price <= 0.0) continue
            // 4. Reject numbers that appear inside a barcode region if detected
            if (rawLineLower.contains("upc") || rawLineLower.contains("barcode") ||
                rawLineLower.contains("sku") || rawLineLower.contains("ean")) continue

            validCandidates.add(cand)
        }

        return validCandidates
    }

    /**
     * PART 2: Extraction only after validation on ML Kit Text.
     */
    fun extractBestPriceResult(
        visionText: Text,
        imageWidth: Int = 0,
        imageHeight: Int = 0
    ): ExtractionResult {
        val allLines = visionText.textBlocks.flatMap { it.lines }
        val lineTexts = allLines.map { it.text }
        val lineHeights = allLines.map { it.boundingBox?.height()?.toDouble() ?: 24.0 }
        val lineCenterYs = allLines.map { it.boundingBox?.centerY()?.toDouble() ?: (imageHeight / 2.0) }

        return extractFromRawTextAndLines(
            rawText = visionText.text,
            lines = lineTexts,
            lineHeights = lineHeights,
            lineCenterYs = lineCenterYs,
            imageWidth = imageWidth,
            imageHeight = imageHeight
        )
    }

    /**
     * Core extraction algorithm with Rule 1, 2, 3, 4.
     */
    fun extractFromRawTextAndLines(
        rawText: String,
        lines: List<String>,
        lineHeights: List<Double> = emptyList(),
        lineCenterYs: List<Double> = emptyList(),
        imageWidth: Int = 0,
        imageHeight: Int = 0
    ): ExtractionResult {
        logD(TAG, "========== ML Kit Raw Recognized Text (Extraction) ==========\n$rawText\n===========================================================")

        if (rawText.isBlank()) {
            return ExtractionResult.Failure("No price detected. Try a clearer photo of the tag.")
        }

        // Rule 3. Document indicators check before scoring
        if (documentIndicatorRegex.containsMatchIn(rawText)) {
            logD(TAG, "Extraction rejected: Document indicator found.")
            return ExtractionResult.Failure("This looks like a document, not a price tag.")
        }

        // Rule 2. Never return a value if the image contains more than 15 recognized lines of dense text
        if (lines.size > 15) {
            logD(TAG, "Extraction rejected: ${lines.size} lines (>15 lines).")
            return ExtractionResult.Failure("This looks like a document, not a price tag.")
        }

        // Find candidates
        val candidates = findValidPriceCandidatesFromLines(
            lines = lines,
            lineHeights = lineHeights,
            lineCenterYs = lineCenterYs,
            imageHeight = imageHeight
        )

        if (candidates.isEmpty()) {
            logD(TAG, "Extraction failed: No valid price candidates.")
            return ExtractionResult.Failure("No price detected. Try a clearer photo of the tag.")
        }

        val imgCenterY = if (imageHeight > 0) imageHeight / 2.0 else 0.0

        // Score candidates
        for (cand in candidates) {
            // Position: Lines closer to vertical center score higher
            val posScore = if (imageHeight > 0 && imgCenterY > 0) {
                val distCenterY = abs(cand.lineCenterY - imgCenterY)
                val normalizedDist = (distCenterY / imgCenterY).coerceIn(0.0, 1.0)
                (1.0 - normalizedDist) * 100.0
            } else {
                50.0
            }

            // Font size: Larger bounding box height scores higher
            val fontScore = cand.fontHeight.coerceIn(8.0, 200.0) * 3.0

            // Currency symbol presence: Dollar sign adds a strong bonus
            val currencyBonus = if (cand.hasDollarSign) 250.0 else 0.0

            // Preference bonus for standard retail decimal formatting (.xx)
            val decimalBonus = if (cand.isDecimal) 60.0 else 0.0

            cand.score = posScore + fontScore + currencyBonus + decimalBonus
            logD(TAG, "Candidate ${cand.price} -> Score: ${cand.score} (pos: $posScore, font: $fontScore, currency: $currencyBonus)")
        }

        val sorted = candidates.sortedByDescending { it.score }
        val topCandidate = sorted.first()

        // Rule 1. Never return a value if the top candidate score is below 45
        if (topCandidate.score < 45.0) {
            logD(TAG, "Extraction rejected: Top candidate score ${topCandidate.score} < 45.0")
            return ExtractionResult.Failure("Price too unclear. Try a sharper photo.")
        }

        // Rule 4. If two or more price candidates exist and their scores are within 8 points,
        // and neither has a dollar sign, return a failure with reason "Multiple possible prices found. Crop the image to a single tag."
        if (sorted.size >= 2) {
            val top1 = sorted[0]
            val top2 = sorted[1]
            if (abs(top1.score - top2.score) <= 8.0 && !top1.hasDollarSign && !top2.hasDollarSign) {
                logD(TAG, "Extraction rejected: Ambiguous candidates within 8 points without dollar sign (${top1.price} vs ${top2.price})")
                return ExtractionResult.Failure("Multiple possible prices found. Crop the image to a single tag.")
            }
        }

        // Extract real product name from image text around selected price
        val bestLine = topCandidate.originalLine
        val sameLineLetters = bestLine
            .replace(Regex("""[$¢€£¥]"""), " ")
            .replace(Regex("""(?<!\d)\d+(?:\.\d+)?(?!\d)"""), " ")
            .replace(Regex("""[^a-zA-Z0-9\s-]"""), " ")
            .trim()

        val itemName: String
        if (sameLineLetters.length >= 3 && sameLineLetters.any { it.isLetter() }) {
            itemName = sameLineLetters.take(35)
        } else {
            val neighboringLines = lines
                .filter { line ->
                    val text = line.trim()
                    text != bestLine &&
                            !text.contains("$") &&
                            text.length >= 2 &&
                            text.any { it.isLetter() } &&
                            !text.contains("UPC", ignoreCase = true) &&
                            !text.contains("BARCODE", ignoreCase = true) &&
                            !text.contains("SKU", ignoreCase = true)
                }
                .map { it.trim().replace(Regex("""[^a-zA-Z0-9\s-]"""), "").trim() }
                .filter { it.isNotBlank() }

            itemName = if (neighboringLines.isNotEmpty()) {
                neighboringLines.take(2).joinToString(" ").take(35)
            } else {
                "Item"
            }
        }

        logD(TAG, "SUCCESS: Selected price: $${topCandidate.price} for '$itemName' (score=${topCandidate.score})")
        return ExtractionResult.Success(
            ExtractedPrice(
                price = topCandidate.price,
                itemName = itemName,
                score = topCandidate.score
            )
        )
    }

    /**
     * Backward-compatible convenience method returning ExtractedPrice? or null on failure.
     */
    fun extractBestPrice(
        visionText: Text,
        imageWidth: Int = 0,
        imageHeight: Int = 0
    ): ExtractedPrice? {
        return when (val res = extractBestPriceResult(visionText, imageWidth, imageHeight)) {
            is ExtractionResult.Success -> res.extracted
            is ExtractionResult.Failure -> null
        }
    }
}
