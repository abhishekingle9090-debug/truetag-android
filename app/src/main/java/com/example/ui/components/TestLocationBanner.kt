package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TaxLocation

// Amber color (#E8A33D) with dark text as specified in requirements
val TestBannerAmber = Color(0xFFE8A33D)
val TestBannerDarkText = Color(0xFF1A1A1D)

@Composable
fun TestLocationBanner(
    location: TaxLocation?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = location != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        if (location == null) return@AnimatedVisibility

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TestBannerAmber)
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .testTag("test_location_amber_banner"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = TestBannerDarkText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Test Location: ${location.city}, ${location.state} ${location.zip} - ${location.ratePercentageFormatted} - Not your real location",
                    color = TestBannerDarkText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("close_test_location_banner")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Turn off test location",
                    tint = TestBannerDarkText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
