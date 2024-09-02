package com.example.wellnest.activities

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleObserver
import com.example.wellnest.adapters.GameListAdapter
import com.example.wellnest.services.GameTimeTrackerService
import com.example.wellnest.R
import com.example.wellnest.adapters.ScheduleListAdapter
import com.google.firebase.firestore.FirebaseFirestore

class Schedule : AppCompatActivity(), LifecycleObserver {
    var gamesList = ArrayList<ApplicationInfo>()


    override fun onStart() {
        super.onStart()    }
    override fun onStop() {
        super.onStop()
    }
    @SuppressLint("SuspiciousIndentation", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        val pkName = packageName
        val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            pkName
        )
        if (mode != AppOpsManager.MODE_ALLOWED) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.data = Uri.fromParts("package", pkName, null)
            startActivity(intent)
        }
        val decorView = window.decorView
        decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (packageInfo in packages) {
            // Check if the application is not a system app
            if (packageInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0 && packageInfo.packageName != packageName) {
                gamesList.add(packageInfo)
            }
        }
        // Sort the list of applications by their names
        gamesList.sortBy { it.loadLabel(pm).toString() }

        val listView = findViewById<ListView>(R.id.gamesListView)
        val adapter = ScheduleListAdapter(this, gamesList)
        listView.adapter = adapter



        val back: Button = findViewById(R.id.back)
        back.setOnClickListener {
            finish()
        }


    }

    override fun onResume() {
        super.onResume()

        val listView = findViewById<ListView>(R.id.gamesListView)
        val adapter = ScheduleListAdapter(this, gamesList)
        listView.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, GameTimeTrackerService::class.java))
    }


}
