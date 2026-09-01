package kr.ac.pcu.aifinder

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kr.ac.pcu.aifinder.databinding.ActivityMainBinding
import kr.ac.pcu.aifinder.fragments.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var itemStorage: ItemStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        

        itemStorage = ItemStorage(PlatformStorage(this))

        setupNavigation()
        setupFab()

        showSplashFragment()
    }

    private fun setupFab() {
        binding.fabCamera.setOnClickListener {
            launchObjectDetectionCamera(this) {
                // Refresh current fragment if needed
                val currentFragment = supportFragmentManager.findFragmentById(binding.fragmentContainer.id)
                (currentFragment as? RefreshableFragment)?.refreshData()
            }
        }
    }

    private fun showSplashFragment() {
        binding.bottomNavigation.visibility = android.view.View.GONE
        binding.fabCamera.visibility = android.view.View.GONE
        
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, SplashFragment())
            .commit()
    }

    fun onSplashFinished() {
        // Check login status
        val currentUser = itemStorage.getCurrentUser()
        if (currentUser == null) {
            showLoginFragment()
        } else {
            // Check if auto-login is actually enabled
            if (itemStorage.isAutoLoginEnabled()) {
                onLoginSuccess()
            } else {
                itemStorage.setCurrentUser(null)
                showLoginFragment()
            }
        }
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                kr.ac.pcu.aifinder.R.id.nav_search -> SearchFragment()
                kr.ac.pcu.aifinder.R.id.nav_room_map -> RoomMapFragment()
                kr.ac.pcu.aifinder.R.id.nav_favorites -> FavoritesFragment()
                kr.ac.pcu.aifinder.R.id.nav_checklist -> ChecklistFragment()
                kr.ac.pcu.aifinder.R.id.nav_stats -> StatsFragment()
                else -> SearchFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(kr.ac.pcu.aifinder.R.anim.fade_in, kr.ac.pcu.aifinder.R.anim.fade_out)
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }

    fun showLoginFragment() {
        // Hide UI elements not needed for login
        binding.bottomNavigation.visibility = android.view.View.GONE
        binding.fabCamera.visibility = android.view.View.GONE
        
        loadFragment(LoginFragment())
    }
    
    fun showRegisterFragment() {
        binding.bottomNavigation.visibility = android.view.View.GONE
        binding.fabCamera.visibility = android.view.View.GONE
        
        loadFragment(RegisterFragment())
    }
    
    fun onLoginSuccess() {
        binding.bottomNavigation.visibility = android.view.View.VISIBLE
        binding.fabCamera.visibility = android.view.View.VISIBLE
        loadFragment(SearchFragment())
    }
}

interface RefreshableFragment {
    fun refreshData()
}
