package com.android.master.kyc.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.android.master.kyc.R

class GetPhotoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_photo)

        configureAndShowFragment()
    }

    private fun configureAndShowFragment() {
        val bundle = intent.extras
        val navHost =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment?
        supportFragmentManager.beginTransaction().setPrimaryNavigationFragment(navHost).commit()

        val navController = findNavController(R.id.navHostFragment)
        val navGraph = navController.navInflater.inflate(R.navigation.get_photo_navigation)
        navController.setGraph(navGraph, bundle)
    }
}