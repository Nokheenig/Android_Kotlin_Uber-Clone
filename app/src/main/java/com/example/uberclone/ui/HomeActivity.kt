package com.example.uberclone.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import com.example.uberclone.R
import com.example.uberclone.SplashScreenActivity
import com.example.uberclone.databinding.ActivityHomeBinding
import com.example.uberclone.utils.Constants
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityHomeBinding
    private lateinit var navView: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarHome.toolbar)

        binding.appBarHome.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }
        drawerLayout = binding.drawerLayout
        navView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_content_home)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        init()
    }

    private fun init(){
        navView.setNavigationItemSelectedListener { it ->
            val builder = AlertDialog.Builder(this@HomeActivity)
            builder.setTitle("Sign out")
            builder.setMessage("Do you really want to sign out ?")
                .setNegativeButton("CANCEL"){dialog, _ ->
                    dialog.dismiss()
                }.setPositiveButton("SIGN OUT"){dialog, _ ->
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this@HomeActivity, SplashScreenActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                }.setCancelable(false)

            val dialog = builder.create()
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(resources.getColor(android.R.color.holo_red_dark))
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(resources.getColor(android.R.color.holo_green_dark))
            }
            dialog.show()
            true
        }

        val headerView = navView.getHeaderView(0)
        val textViewName = headerView.findViewById<TextView>(R.id.textViewUserName)
        val textViewStar = headerView.findViewById<TextView>(R.id.text_view_rating)
        val textViewPhone = headerView.findViewById<TextView>(R.id.text_view_phone)

        textViewName.text = Constants.buildWelcomeMessage()
        textViewPhone.text = Constants.currentUser?.phoneNumber
        textViewStar.text = java.lang.StringBuilder().append(Constants.currentUser?.rating)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_home)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}