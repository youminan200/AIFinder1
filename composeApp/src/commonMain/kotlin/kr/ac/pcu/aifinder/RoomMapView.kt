package kr.ac.pcu.aifinder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoomMapView(
    areas: List<RoomArea>,
    itemCounts: Map<Int, Int>,
    selectedAreaId: Int,
    onAreaClick: (RoomArea) -> Unit,
    modifier: Modifier = Modifier
) {
    val areaColors = listOf(
        Color(0xFFEFEAFE), // Purple-ish
        Color(0xFFE8F0FF), // Blue-ish
        Color(0xFFE7F5EF), // Green-ish
        Color(0xFFFFF4DA), // Amber-ish
        Color(0xFFDFF7F4), // Teal-ish
        Color(0xFFFFE8ED)  // Rose-ish
    )

    val areaBorderColors = listOf(
        Color(0xFF6D5BD0),
        Color(0xFF2563EB),
        Color(0xFF167A5A),
        Color(0xFFB7791F),
        Color(0xFF0F766E),
        Color(0xFFC2415B)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (row in 0 until 3) {
            Row(
                modifier = Modifier.fillMaxWidth().height(90.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (col in 0 until 2) {
                    val index = row * 2 + col
                    if (index >= areas.size) {
                        Spacer(modifier = Modifier.weight(1f))
                        continue
                    }
                    val area = areas[index]
                    val colorIndex = index % areaColors.size
                    val isSelected = area.id == selectedAreaId
                    val count = itemCounts[area.id] ?: 0

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onAreaClick(area) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFE6F0FA) else areaColors[colorIndex]
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color(0xFF2563EB) else areaBorderColors[colorIndex]
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = area.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C2633)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "보관 중: ${count}개",
                                fontSize = 12.sp,
                                color = Color(0xFF5E6B7A)
                            )
                        }
                    }
                }
            }
        }
    }
}
