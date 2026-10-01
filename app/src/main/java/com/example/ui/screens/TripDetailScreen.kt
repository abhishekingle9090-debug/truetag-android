package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.model.ShoppingTrip
import com.example.ui.components.DashedDivider
import com.example.ui.components.SwingTagCard
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.IbmpPlexMono
import com.example.ui.theme.InkNavy
import com.example.ui.theme.PaperCream
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.SpaceGrotesk
import com.example.ui.theme.TagRed
import com.example.ui.theme.WarmGray
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    trip: ShoppingTrip?,
    navController: NavController
) {
    val context = LocalContext.current

    BackHandler {
        navController.popBackStack()
    }

    if (trip == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text("Trip not found", fontFamily = SpaceGrotesk, color = WarmGray)
        }
        return
    }

    val dateFormatted = SimpleDateFormat("MMMM d, yyyy · h:mm a", Locale.US).format(Date(trip.date))

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("trip_detail_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "trip detail",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("trip_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaperCream)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.background,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Primary Action: Share as Receipt
                    Button(
                        onClick = {
                            shareTripAsReceiptImage(context, trip)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .border(1.dp, Color(0xFF9E332D), RoundedCornerShape(14.dp))
                            .testTag("share_receipt_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TagRed,
                            contentColor = PaperSurface
                        )
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "share as receipt",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // Secondary Action: Export CSV
                    Text(
                        text = "export CSV",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        textDecoration = TextDecoration.Underline,
                        color = InkNavy,
                        modifier = Modifier
                            .clickable {
                                exportTripCsv(context, trip)
                            }
                            .padding(vertical = 4.dp)
                            .testTag("export_csv_button")
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Card in Swing Tag Shape with Punched Hole
            item {
                Spacer(modifier = Modifier.height(6.dp))
                SwingTagCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trip_detail_header_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = trip.storeName.ifBlank { "Retail Store" },
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = trip.name,
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 14.sp,
                                    color = WarmGray
                                )
                                Text(
                                    text = dateFormatted,
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 12.sp,
                                    color = WarmGray.copy(alpha = 0.8f)
                                )
                            }
                        }

                        DashedDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL CHECKOUT",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp,
                                    color = WarmGray
                                )
                                Text(
                                    text = "total tax: ${trip.formattedTotalTaxAmount}",
                                    fontFamily = IbmpPlexMono,
                                    fontSize = 13.sp,
                                    color = WarmGray
                                )
                            }

                            // Display type treatment for real total price
                            val totalStr = String.format("%.2f", trip.totalTruePrice)
                            val parts = totalStr.split(".")
                            val intPart = parts.getOrNull(0) ?: "0"
                            val decPart = parts.getOrNull(1) ?: "00"

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$$intPart",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 36.sp,
                                    lineHeight = 38.sp,
                                    color = ForestGreen
                                )
                                Text(
                                    text = ".$decPart",
                                    fontFamily = IbmpPlexMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    lineHeight = 28.sp,
                                    color = ForestGreen,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "ITEMIZED BREAKDOWN (${trip.items.size} ITEMS)",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    color = WarmGray
                )
            }

            // Full item list in read-only swing tag style
            itemsIndexed(trip.items) { index, item ->
                SwingTagCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trip_item_row_$index"),
                    hasHoleCutout = false
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${item.formattedTagPrice} shelf · ${item.category} · +${item.formattedTaxAmount} tax",
                                fontFamily = IbmpPlexMono,
                                fontSize = 12.sp,
                                color = WarmGray
                            )
                        }

                        Text(
                            text = item.formattedTruePrice,
                            fontFamily = IbmpPlexMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = ForestGreen
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Renders the trip as a vertical receipt-style image on paper cream background with a torn edge at the bottom,
 * and opens the native Android share sheet.
 */
private fun shareTripAsReceiptImage(context: Context, trip: ShoppingTrip) {
    try {
        val width = 720
        val baseHeight = 400 + (trip.items.size * 55)
        val height = baseHeight.coerceAtLeast(600)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Paper Cream background #F7F3EA
        canvas.drawColor(android.graphics.Color.parseColor("#F7F3EA"))

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Header: Store Name
        paint.color = android.graphics.Color.parseColor("#1F2A44")
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(trip.storeName.ifBlank { "TrueTag Retail Store" }, width / 2f, 70f, paint)

        // Subheader: TrueTag Receipt
        paint.textSize = 20f
        paint.color = android.graphics.Color.parseColor("#6B6258")
        canvas.drawText("RECEIPT · TRUE PRICE BEFORE CHECKOUT", width / 2f, 105f, paint)

        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(trip.date))
        paint.textSize = 18f
        paint.typeface = Typeface.MONOSPACE
        canvas.drawText("DATE: $dateStr", width / 2f, 135f, paint)

        // Dashed divider line
        paint.color = android.graphics.Color.parseColor("#E4DCC8")
        paint.strokeWidth = 3f
        canvas.drawLine(40f, 160f, width - 40f, 160f, paint)

        // Items
        var y = 200f
        paint.textSize = 22f
        paint.typeface = Typeface.MONOSPACE

        for (item in trip.items) {
            paint.textAlign = Paint.Align.LEFT
            paint.color = android.graphics.Color.parseColor("#201C16")
            val nameTrunc = if (item.name.length > 20) item.name.take(19) + "…" else item.name
            canvas.drawText(nameTrunc, 50f, y, paint)

            paint.textAlign = Paint.Align.RIGHT
            paint.color = android.graphics.Color.parseColor("#2F5D50")
            canvas.drawText(item.formattedTruePrice, width - 50f, y, paint)

            y += 24f
            paint.textSize = 16f
            paint.color = android.graphics.Color.parseColor("#6B6258")
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("  tag ${item.formattedTagPrice} + tax ${item.formattedTaxAmount}", 50f, y, paint)
            y += 32f
            paint.textSize = 22f
        }

        // Summary Divider
        paint.color = android.graphics.Color.parseColor("#E4DCC8")
        canvas.drawLine(40f, y + 10f, width - 40f, y + 10f, paint)
        y += 45f

        // Subtotal
        paint.textSize = 20f
        paint.color = android.graphics.Color.parseColor("#6B6258")
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("SHELF SUBTOTAL", 50f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(trip.formattedTotalTagPrice, width - 50f, y, paint)

        y += 30f
        canvas.drawText("TOTAL SALES TAX", 50f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(trip.formattedTotalTaxAmount, width - 50f, y, paint)

        y += 45f
        // Total checkout price
        paint.textSize = 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = android.graphics.Color.parseColor("#2F5D50")
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("CHECKOUT TOTAL", 50f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.MONOSPACE
        canvas.drawText(trip.formattedTotalTruePrice, width - 50f, y, paint)

        // Torn edge zigzag at bottom
        paint.color = android.graphics.Color.WHITE
        val toothW = 24f
        val toothH = 18f
        val path = android.graphics.Path().apply {
            moveTo(0f, height.toFloat())
            var curX = 0f
            var up = true
            while (curX < width) {
                curX += toothW / 2f
                val py = if (up) height - toothH else height.toFloat()
                lineTo(curX.coerceAtMost(width.toFloat()), py)
                up = !up
            }
            lineTo(width.toFloat(), height.toFloat())
            close()
        }
        canvas.drawPath(path, paint)

        // Save image to cache
        val cachePath = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File(cachePath, "TrueTag_Receipt_${trip.id.take(8)}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "TrueTag Receipt - ${trip.name}")
            putExtra(Intent.EXTRA_TEXT, "Here is my shopping receipt from TrueTag: ${trip.formattedTotalTruePrice} total.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Receipt Image"))
    } catch (e: Exception) {
        // Fallback to text share
        val itemsSummary = trip.items.joinToString("\n") { "• ${it.name}: ${it.formattedTagPrice} (+${it.formattedTaxAmount} tax = ${it.formattedTruePrice})" }
        val text = "🧾 TrueTag Receipt: ${trip.name} (${trip.storeName})\nTotal: ${trip.formattedTotalTruePrice}\nTax: ${trip.formattedTotalTaxAmount}\n\n$itemsSummary"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Receipt"))
    }
}

private fun exportTripCsv(context: Context, trip: ShoppingTrip) {
    try {
        val csvHeader = "Item Name,Category,Shelf Tag Price,Tax Rate,Tax Amount,Checkout Price\n"
        val rows = trip.items.joinToString("\n") { item ->
            "\"${item.name.replace("\"", "\"\"")}\",\"${item.category}\",${item.tagPrice},${String.format("%.4f", item.taxRate)},${item.taxAmount},${item.truePrice}"
        }
        val csvSummary = "\n\"Total\",,\"${trip.totalTagPrice}\",,\"${trip.totalTaxAmount}\",\"${trip.totalTruePrice}\"\n"
        val fullCsv = csvHeader + rows + csvSummary

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "TrueTag_Trip_${trip.name.replace(" ", "_")}.csv")
            putExtra(Intent.EXTRA_TEXT, fullCsv)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Export Trip CSV"))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not export CSV", Toast.LENGTH_SHORT).show()
    }
}
