package kr.ac.pcu.aifinder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
        Color(0xFFF5F3FF), // Soft Purple
        Color(0xFFEFF6FF), // Soft Blue
        Color(0xFFECFDF5), // Soft Green
        Color(0xFFFFFBEB), // Soft Amber
        Color(0xFFF0FDFA), // Soft Teal
        Color(0xFFFFF1F2)  // Soft Rose
    )

    val areaBorderColors = listOf(
        Color(0xFFDDD6FE),
        Color(0xFFDBEAFE),
        Color(0xFFD1FAE5),
        Color(0xFFFEF3C7),
        Color(0xFFCCFBF1),
        Color(0xFFFFE4E6)
    )

    val areaTextColors = listOf(
        Color(0xFF6D28D9),
        Color(0xFF1D4ED8),
        Color(0xFF047857),
        Color(0xFFB45309),
        Color(0xFF0F766E),
        Color(0xFFBE123C)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (row in 0 until 3) {
            Row(
                modifier = Modifier.fillMaxWidth().height(92.dp),
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
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFEEF2FF) else areaColors[colorIndex]
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isSelected) 6.dp else 1.dp
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF4F46E5) else areaBorderColors[colorIndex]
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(14.dp).padding(end = 4.dp)
                                    )
                                }
                                Text(
                                    text = area.name,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF1E293B)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFC7D2FE) else Color(0xFFF1F5F9).copy(alpha = 0.8f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "보관 중: ${count}개",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF3730A3) else areaTextColors[colorIndex],
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
