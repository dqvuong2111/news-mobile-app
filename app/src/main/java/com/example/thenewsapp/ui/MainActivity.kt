package com.example.thenewsapp.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.thenewsapp.R
import com.example.thenewsapp.database.ArticleDatabase
import com.example.thenewsapp.databinding.ActivityMainBinding
import com.example.thenewsapp.repository.NewsRepository
import com.example.thenewsapp.utility.ConnectivityObserver
import com.example.thenewsapp.utility.NetworkConnectivityObserver
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainActivity : AppCompatActivity() {

    lateinit var newsViewModel: NewsViewModel
    lateinit var binding: ActivityMainBinding

    private lateinit var connectivityObserver: ConnectivityObserver
    private var isFirstStatusCheck = true
    private val handler = Handler(Looper.getMainLooper())
    private var hideRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val newsRepository = NewsRepository(ArticleDatabase(this))
        val viewModelProviderFactory = NewsViewModelProviderFactory(application, newsRepository)
        newsViewModel = ViewModelProvider(this, viewModelProviderFactory).get(NewsViewModel::class.java)

        val navHostFragment = supportFragmentManager.findFragmentById((R.id.nav_host_news)) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)

        // Handle destination changes for bottom nav visibility and state
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.article_fragment -> {
                    // Hide bottom nav when viewing article for cleaner experience
                    binding.bottomNav.visibility = View.GONE
                }
                R.id.headlines_fragment -> {
                    binding.bottomNav.visibility = View.VISIBLE
                    binding.bottomNav.menu.findItem(R.id.headlines_fragment)?.isChecked = true
                }
                R.id.favourites_fragment -> {
                    binding.bottomNav.visibility = View.VISIBLE
                    binding.bottomNav.menu.findItem(R.id.favourites_fragment)?.isChecked = true
                }
                R.id.search_fragment -> {
                    binding.bottomNav.visibility = View.VISIBLE
                    binding.bottomNav.menu.findItem(R.id.search_fragment)?.isChecked = true
                }
            }
        }

        // Handle bottom nav item reselection to pop back stack
        binding.bottomNav.setOnItemReselectedListener { item ->
            // Pop the back stack to the start destination of that tab
            navController.popBackStack(item.itemId, inclusive = false)
        }

        // Setup network connectivity observer
        setupNetworkConnectivityObserver()
    }

    private fun setupNetworkConnectivityObserver() {
        connectivityObserver = NetworkConnectivityObserver(applicationContext)

        // Check initial status but don't show banner on app start if online
        val initialStatus = connectivityObserver.getCurrentStatus()
        if (initialStatus == ConnectivityObserver.Status.Unavailable ||
            initialStatus == ConnectivityObserver.Status.Lost) {
            showNetworkBanner(false)
        }

        connectivityObserver.observe().onEach { status ->
            when (status) {
                ConnectivityObserver.Status.Available -> {
                    if (!isFirstStatusCheck) {
                        showNetworkBanner(true)
                        // Auto-hide after 3 seconds
                        hideRunnable?.let { handler.removeCallbacks(it) }
                        hideRunnable = Runnable { hideNetworkBanner() }
                        handler.postDelayed(hideRunnable!!, 3000)
                    }
                    isFirstStatusCheck = false
                }
                ConnectivityObserver.Status.Unavailable,
                ConnectivityObserver.Status.Lost -> {
                    isFirstStatusCheck = false
                    hideRunnable?.let { handler.removeCallbacks(it) }
                    showNetworkBanner(false)
                }
                ConnectivityObserver.Status.Losing -> {
                    // Optionally handle losing state
                }
            }
        }.launchIn(lifecycleScope)
    }

    private fun showNetworkBanner(isOnline: Boolean) {
        val banner = binding.networkBanner.root as LinearLayout
        val icon = banner.findViewById<ImageView>(R.id.network_status_icon)
        val text = banner.findViewById<TextView>(R.id.network_status_text)

        if (isOnline) {
            banner.setBackgroundColor(ContextCompat.getColor(this, R.color.success))
            icon.setImageResource(android.R.drawable.ic_dialog_info)
            text.text = getString(R.string.back_online)
        } else {
            banner.setBackgroundColor(ContextCompat.getColor(this, R.color.error))
            icon.setImageResource(android.R.drawable.ic_dialog_alert)
            text.text = getString(R.string.no_internet_connection)
        }

        if (banner.visibility != View.VISIBLE) {
            banner.visibility = View.VISIBLE
            val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)
            banner.startAnimation(slideDown)
        }
    }

    private fun hideNetworkBanner() {
        val banner = binding.networkBanner.root as LinearLayout
        if (banner.visibility == View.VISIBLE) {
            val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
            slideUp.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    banner.visibility = View.GONE
                }
                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            })
            banner.startAnimation(slideUp)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideRunnable?.let { handler.removeCallbacks(it) }
    }
}
