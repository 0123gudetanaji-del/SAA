package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Firm
import com.example.data.model.PartEntity
import com.example.data.model.SyncStatus
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberGoldDark
import com.example.ui.theme.AutoNavy
import com.example.ui.theme.AutoNavyDark
import com.example.ui.theme.SaaBlue
import com.example.ui.theme.SaaBlueBg
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedBg
import com.example.ui.theme.StatusYellow
import com.example.ui.theme.StatusYellowBg
import com.example.ui.theme.TvsRed
import com.example.ui.theme.TvsRedBg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FirmBadge(firm: String, modifier: Modifier = Modifier) {
    when (firm) {
        Firm.SAA.code -> {
            Surface(
                color = SaaBlueBg,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SaaBlue.copy(alpha = 0.4f)),
                modifier = modifier
            ) {
                Text(
                    text = "SAA",
                    color = SaaBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Firm.TVS.code -> {
            Surface(
                color = TvsRedBg,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TvsRed.copy(alpha = 0.4f)),
                modifier = modifier
            ) {
                Text(
                    text = "TVS",
                    color = TvsRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        else -> {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Surface(
                    color = SaaBlueBg,
                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp),
                    modifier = Modifier
                ) {
                    Text(
                        text = "SAA",
                        color = SaaBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    color = TvsRedBg,
                    shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                    modifier = Modifier
                ) {
                    Text(
                        text = "TVS",
                        color = TvsRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StockStatusBadge(part: PartEntity, modifier: Modifier = Modifier) {
    val total = part.totalStock
    val (bgColor, textColor, label) = when {
        total <= 0 -> Triple(StatusRedBg, StatusRed, "OUT OF STOCK")
        total <= part.minStock -> Triple(StatusYellowBg, StatusYellow, "LOW STOCK (${total}/${part.minStock})")
        else -> Triple(StatusGreenBg, StatusGreen, "IN STOCK ($total)")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SyncStatusPill(
    status: SyncStatus,
    lastSyncTimestamp: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, contentColor, icon, statusText) = when (status) {
        SyncStatus.SYNCED -> Quadruple(
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32),
            Icons.Default.CheckCircle,
            "Synced"
        )
        SyncStatus.SYNCING -> Quadruple(
            Color(0xFFFFF3E0),
            Color(0xFFE65100),
            Icons.Default.Sync,
            "Syncing..."
        )
        SyncStatus.WAITING_FOR_DEVICE -> Quadruple(
            Color(0xFFEDE7F6),
            Color(0xFF512DA8),
            Icons.Default.Wifi,
            "Waiting for device"
        )
        SyncStatus.CONFLICT -> Quadruple(
            Color(0xFFFFF8E1),
            Color(0xFFF57F17),
            Icons.Default.SyncProblem,
            "Conflict Resolved"
        )
        SyncStatus.FAILED -> Quadruple(
            Color(0xFFFFEBEE),
            Color(0xFFC62828),
            Icons.Default.WifiOff,
            "Sync Offline"
        )
    }

    val timeStr = if (lastSyncTimestamp > 0) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(lastSyncTimestamp))
    } else {
        "None"
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, contentColor.copy(alpha = 0.3f)),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("sync_status_indicator")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (status == SyncStatus.SYNCING) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = contentColor,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = statusText,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$statusText • $timeStr",
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun PartItemCard(
    part: PartEntity,
    onClick: () -> Unit,
    onStockIn: () -> Unit,
    onStockOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("part_card_${part.partNumber}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = part.partName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = AutoNavyDark
                    )
                    Text(
                        text = "Part No: ${part.partNumber}",
                        fontSize = 12.sp,
                        color = Slate600,
                        fontWeight = FontWeight.Medium
                    )
                }
                FirmBadge(firm = part.firm)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stock breakdown row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // SAA Stock
                    Column {
                        Text("SAA Stock", fontSize = 10.sp, color = Slate600)
                        Text(
                            text = "${part.stockSaa}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SaaBlue
                        )
                    }

                    // Divider dot
                    Text("•", color = Slate600)

                    // TVS Stock
                    Column {
                        Text("TVS Stock", fontSize = 10.sp, color = Slate600)
                        Text(
                            text = "${part.stockTvs}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TvsRed
                        )
                    }

                    // Divider dot
                    Text("•", color = Slate600)

                    // Total
                    Column {
                        Text("Total", fontSize = 10.sp, color = Slate600)
                        Text(
                            text = "${part.totalStock}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AutoNavyDark
                        )
                    }
                }

                StockStatusBadge(part = part)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Location & actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Location",
                        tint = Slate600,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (part.location.isNotBlank()) part.location else "Rack ${part.rack}-${part.rackNumber} / ${part.shelf}",
                        fontSize = 11.sp,
                        color = Slate600
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = StatusGreenBg,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clickable(onClick = onStockIn)
                            .testTag("quick_stock_in_${part.partNumber}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddShoppingCart,
                                contentDescription = "Stock In",
                                tint = StatusGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ IN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusGreen
                            )
                        }
                    }

                    Surface(
                        color = StatusRedBg,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clickable(onClick = onStockOut)
                            .testTag("quick_stock_out_${part.partNumber}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RemoveShoppingCart,
                                contentDescription = "Stock Out",
                                tint = StatusRed,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "- OUT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusRed
                            )
                        }
                    }
                }
            }
        }
    }
}
