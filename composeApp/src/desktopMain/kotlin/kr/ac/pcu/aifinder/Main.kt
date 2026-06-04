package kr.ac.pcu.aifinder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    // iPhone 13/14 screen proportions (390x844) + Bezel offsets
    val windowState = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(430.dp, 920.dp)
    )

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "AIFinder (iOS Simulated Mode on Windows)",
        resizable = false
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF1E1E24) // Dark steel outer background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // iPhone Bezel Frame
                Box(
                    modifier = Modifier
                        .size(390.dp, 844.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .border(10.dp, Color(0xFF0D0D11), RoundedCornerShape(40.dp))
                        .background(Color.White)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Launch the cross-platform App with null context (behaves like iOS/Mock Mode)
                        App(platformContext = null)

                        // Simulated Dynamic Island (iPhone Notch Mockup)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                                .size(110.dp, 28.dp)
                                .background(Color.Black, RoundedCornerShape(14.dp))
                        )

                        // Simulated iOS Home Indicator Bar
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                                .size(140.dp, 5.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }
    }
}
