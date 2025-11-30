package com.example.spotme

import com.example.spotme.R
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    val homeFragment = HomeFragment()
    val presageFragment = PresageFragment()
    val chatbotFragment = ChatbotFragment()
    var preinterviewFragment = PreinterviewFragment()

    var activeFragment: Fragment = presageFragment
    var previousFragment: Fragment = presageFragment




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_main)

        val layoutParams = window.attributes
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE

        supportFragmentManager.beginTransaction().apply {
            add(R.id.frame_fragment_holder, presageFragment, "Presage").hide(presageFragment)
            add(R.id.frame_fragment_holder, chatbotFragment, "Chatbot").hide(chatbotFragment)
            add(R.id.frame_fragment_holder, homeFragment, "Home").hide(homeFragment)
            add(R.id.frame_fragment_holder, preinterviewFragment, "Preinterview").hide(preinterviewFragment)

        }.commit()

        switchFragment(homeFragment)
//        switchFragment(presageFragment)

        val chatbot_btn = findViewById<android.widget.Button>(R.id.button1)
        chatbot_btn.setOnClickListener {
            switchFragment(chatbotFragment)
        }
        val presage_btn = findViewById<android.widget.Button>(R.id.button2)
        presage_btn.setOnClickListener {
            switchFragment(presageFragment)
        }

        val home_btn = findViewById<android.widget.Button>(R.id.button3)
        home_btn.setOnClickListener {
            switchFragment(homeFragment)
        }

    }

    fun switchFragment(targetFragment: Fragment, navItemId: Int? = null) {
        if (targetFragment == activeFragment) return

        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .setMaxLifecycle(activeFragment, Lifecycle.State.STARTED) // Pause this fragment
            .show(targetFragment)
            .setMaxLifecycle(targetFragment, Lifecycle.State.RESUMED) // Resume this one
            .commit()

        previousFragment = activeFragment
//        previousLayout = when (activeFragment) {
//            homeFragment -> R.id.homeFragment_ID
//            stockFragment -> R.id.stockFragment_ID
//            genOptionsFragment -> R.id.genOptionsFragment_ID
//
//            else -> R.id.homeFragment_ID
//        }

        activeFragment = targetFragment

//        navItemId?.let {
//            val navView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
//            if (navView.selectedItemId != it) {
//                navView.selectedItemId = it
//            }
//        }
    }

}