package com.android.master.kyc.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.android.master.kyc.R


class MainActivity : AppCompatActivity() {
    lateinit var grantedPermission: (granted: Boolean) -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        askForPermission({
            if (it) {
                configureAndShowFragment()
            }
        })
    }

    private fun askForPermission(grantedPermission: (granted: Boolean) -> Unit) {
        this.grantedPermission = grantedPermission
        val isGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (isGranted) {
            grantedPermission(true)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
                1111
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        try {
            if (requestCode == 1111) {
                if (grantResults.size > 0) {
                    if (grantResults.get(0) == PackageManager.PERMISSION_GRANTED) {
                        grantedPermission(true)
                    } else {
                        askForPermission(grantedPermission)
                    }
                } else {
                    askForPermission(grantedPermission)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun configureAndShowFragment() {
        val bundle = intent.extras
        val navHost =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment?
        supportFragmentManager.beginTransaction().setPrimaryNavigationFragment(navHost).commit()

        val navController = findNavController(R.id.navHostFragment)
        val navGraph = navController.navInflater.inflate(R.navigation.main_navigation)
        navController.setGraph(navGraph, bundle)
    }
}
