package com.example.timebankingapp

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.*
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity : AppCompatActivity() {


    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navHostFragment: NavHostFragment
    private val vm by viewModels<MyViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController


        val navView: NavigationView = findViewById(R.id.nav_view)
        navView.setupWithNavController(navController)
        val navHeader = navView.getHeaderView(0)


        drawerLayout = findViewById(R.id.drawer_layout)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.home,
                R.id.myTimeslotList,
                R.id.chatList,
                R.id.showProfile,
                R.id.interested_timeslotsList,
                R.id.accepted_assigned_timeslotsList
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        vm.userAuth.observe(this) {
            if (it != null){
                navHeader.findViewById<TextView>(R.id.user_name).text = it.name.plus(" " + it.surname)
                vm.imageAuth.observe(this) {
                    val img = navHeader.findViewById<CircleImageView>(R.id.profile_image)
                    img.setImageBitmap(it)
                }
            }
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }


    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START)
        else
            super.onBackPressed()
    }


    fun logoutFun(item: MenuItem) {
        val auth = Firebase.auth
        auth.signOut()
        // Google sign out
        drawerLayout.close()
        when (navController.currentDestination?.id) {
            R.id.home -> navController.navigate(R.id.action_home_to_googleSignInFragment2)
            R.id.showProfile -> navController.navigate(R.id.action_showProfile_to_googleSignInFragment)
            R.id.myTimeslotList -> navController.navigate(R.id.action_myTimeslotList_to_googleSignInFragment3)
            R.id.accepted_assigned_timeslotsList -> navController.navigate(R.id.action_accepted_assigned_timeslotsList_to_googleSignInFragment)
            R.id.chatList -> navController.navigate(R.id.action_chatList_to_googleSignInFragment)
            R.id.interested_timeslotsList -> navController.navigate(R.id.action_interested_timeslotsList_to_googleSignInFragment)
        }

    }


}