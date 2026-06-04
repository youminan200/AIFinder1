package kr.ac.pcu.aifinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {
    private val refreshTrigger = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App(platformContext = this, refreshTrigger = refreshTrigger.value)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTrigger.value++
    }
}

