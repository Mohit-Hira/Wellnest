package com.example.wellnest.services

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.wellnest.R

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private var appName: String = ""

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        appName = intent.getStringExtra("appName") ?: "the app"
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Inflate the floating view layout
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_dialog, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        // Add the view to the window
        windowManager.addView(floatingView, params)

        floatingView.findViewById<Button>(R.id.dismiss_button).setOnClickListener {
            stopSelf()
        }

        floatingView.findViewById<ImageView>(R.id.imageView).visibility=View.VISIBLE
        floatingView.findViewById<Button>(R.id.close_app_button).setOnClickListener {

            goToHomeScreen()
            stopSelf()
        }
    }
    private fun goToHomeScreen() {
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }
    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}
