package kr.ac.pcu.aifinder

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // MainActivity로 리다이렉트
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
