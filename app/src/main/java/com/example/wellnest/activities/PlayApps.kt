package com.example.wellnest.activities

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.AppOpsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleObserver
import com.example.wellnest.adapters.GameListAdapter
import com.example.wellnest.services.GameTimeTrackerService
import com.example.wellnest.R
import com.example.wellnest.helpers.MyPackage
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class PlayApps : AppCompatActivity(), LifecycleObserver {

    var gamesList = ArrayList<ApplicationInfo>()
    val allPackages: MutableList<String> = arrayListOf()

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }
    @SuppressLint("SuspiciousIndentation", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_apps)
        val animFadein: Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_in)

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
        readFireStoreData()
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (packageInfo in packages) {
            if (packageInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0 && packageInfo.packageName != packageName) {
                allPackages.add(packageInfo.packageName)
                if(!checkAppIsGame(this, packageInfo.packageName))
                {
                    gamesList.add(packageInfo)
                }
            }
        }


        // Sort the list of applications by their names
        gamesList.sortBy { it.loadLabel(pm).toString() }

//        for (packageInfo in packages) {
////            val b: Boolean = checkAppIsGame(this, packageInfo.packageName)
////            if (b) {
//                gamesList.add(packageInfo)
////            }
//        }
        val listView = findViewById<ListView>(R.id.gamesListView)
        val adapter = GameListAdapter(this, gamesList)
        listView.adapter = adapter



        val history_btn: Button = findViewById(R.id.history_btn)
        history_btn.setOnClickListener {
            history_btn.startAnimation(animFadein)
            startActivity(Intent(this, HistoryActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
        val app_txt: TextView = findViewById(R.id.apps)
        app_txt.setOnClickListener {
            startActivity(Intent(this, PlayGames::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }
        val schedule: TextView = findViewById(R.id.schedule)
        schedule.setOnClickListener {
            schedule.startAnimation(animFadein)
            startActivity(Intent(this, Schedule::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val logout_btn: Button = findViewById(R.id.logout_btn)
        logout_btn.setOnClickListener{
            logout_btn.startAnimation(animFadein)
            sharedPreferences.edit().remove("isLoggedIn").commit()
            val intent = Intent(this, SplashScreen::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

    }

    fun checkAppIsGame(context: Context, packageName: String): Boolean {
        try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return info.category == ApplicationInfo.CATEGORY_GAME
            } else {
                return info.flags and ApplicationInfo.FLAG_IS_GAME == ApplicationInfo.FLAG_IS_GAME
            }
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
    }
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun hasNotificationPermission(): Boolean {
        return NotificationManagerCompat.from(this).areNotificationsEnabled()
    }
    private fun requestNotificationPermission() {
        if (!hasNotificationPermission()) {
            AlertDialog.Builder(this)
                .setTitle("Notification Permission Required")
                .setMessage("This app requires access to notifications to function properly. Please enable them.")
                .setPositiveButton("Go to Settings") { dialog, which ->
                    val intent = Intent().apply {
                        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    }
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        requestNotificationPermission()

        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val tempMail=sharedPreferences.getString("email", "")
        val packagesString = allPackages.joinToString(",")
        if (isServiceRunning(GameTimeTrackerService::class.java)) {
            stopService(Intent(this, GameTimeTrackerService::class.java))
        }
        val serviceIntent = Intent(this, GameTimeTrackerService::class.java)
        serviceIntent.putExtra("packageNames", packagesString)
        serviceIntent.putExtra("email", tempMail)

        startService(serviceIntent)

        readFireStoreData()
        val listView = findViewById<ListView>(R.id.gamesListView)
        val adapter = GameListAdapter(this, gamesList)
        listView.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
//        stopService(Intent(this, GameTimeTrackerService::class.java))
    }

    private fun readFireStoreData() {
        val totalTimeTextView = findViewById<TextView>(R.id.total_time)
        val db = FirebaseFirestore.getInstance()
        db.collection("Users")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    var calculatedTotalTime = 0  // Use a local variable to sum the time
                    val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    val tempMail = sharedPreferences.getString("email", "")
                    for (document in task.result!!) {
                        if (document.data["email"].toString() == tempMail) {
                            val packagesData = document.get("packages") as? List<Map<String, Any>>
                            if (packagesData != null) {
                                for (packageData in packagesData) {
                                    val pkg = packageData["packageName"]?.toString() ?: ""
                                    val time = packageData["time"]?.toString() ?: "0"
                                    if(!checkAppIsGame(this, pkg)) {
                                        calculatedTotalTime += time.toInt()
                                    }
                                }
                            }
                        }
                    }
                    // Update the TextView only once here
                    val hours = calculatedTotalTime / 3600
                    val minutes = (calculatedTotalTime % 3600) / 60
                    val seconds = calculatedTotalTime % 60

                    totalTimeTextView.text = when {
                        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
                        minutes > 0 -> String.format("%d m:%02d s", minutes, seconds)
                        else -> String.format("%d s", seconds)
                    }
                    //totalTimeTextView.text = "${calculatedTotalTime} seconds"
                }
            }
    }

}
