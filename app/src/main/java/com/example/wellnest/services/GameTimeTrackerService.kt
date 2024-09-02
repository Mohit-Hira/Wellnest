package com.example.wellnest.services


import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.wellnest.R
import com.example.wellnest.helpers.History
import com.example.wellnest.helpers.MyPackage
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar


class GameTimeTrackerService : Service() {

    private val handler = Handler()
    private lateinit var prefs: SharedPreferences
    private lateinit var usageStatsManager: UsageStatsManager
    private var trackingPackageName = listOf("game.maddex.jump")  // Example list
//    private var trackingPackageName = "game.maddex.jump"  // Example list
    private var isGameForeground = false
    private val CHANNEL_ID = "channel_id_example_01"
    private val PREFS_NAME = "MyPrefsFile"
    private val LAST_ACTIVE_TIME = "lastActiveTime"
    private var email=""
    private var currentForegroundApp: String? = null
    private val notifiedApps = mutableMapOf<String, Boolean>()


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notification Title"
            val descriptionText = "Notification Description"
            val importance: Int = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Game Time Tracker Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }    private fun getLastActiveTime(): Long {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(LAST_ACTIVE_TIME, 0)
    }
    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("GameTimePrefs", Context.MODE_PRIVATE)
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        createNotificationChannel()
        startForegroundService()
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Wellnest")
            .setContentText("Time Tracking through Wellnest Starts")
            .setSmallIcon(R.drawable.transparent_notification_logo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, notification.build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ServiceCheck", "Service Started")
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val currentDate = sharedPreferences.getString("currentDate", null)
        val today = LocalDate.now().toString()

        if (currentDate == null) {
            // If no date is set, get today's date, save it, and return it
            sharedPreferences.edit().putString("currentDate", today).apply()
        }
        else if(currentDate!=today){
            sharedPreferences.edit().putString("currentDate", today).apply()
            val packages = mutableListOf<MyPackage>()

            val db = FirebaseFirestore.getInstance()

            db.collection("Users")
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        for (document in task.result!!) {
                            if (document.data.getValue("email").toString() == email) {
                                val packagesData = document.get("packages") as? List<Map<String, Any>>
                                if (packagesData != null) {
                                    for (packagesdata in packagesData) {
                                        val pkg = packagesdata["packageName"]?.toString() ?: ""
                                        var scheduled = packagesdata["scheduled"]?.toString() ?: "0"
                                        var time="0"

                                        val package_ = MyPackage(
                                            pkg,
                                            scheduled,
                                            time
                                        )
                                        packages.add(package_)
                                    }
                                }

                                // Define a HashMap to hold the update
                                val add = HashMap<String, Any>()
                                add["packages"] = packages
                                val collection2 = db.collection("Users")
                                collection2.document(document.id)
                                    .update(add)
                                    .addOnSuccessListener {
                                    }
                                    .addOnFailureListener {
                                    }
                            }
                        }
                    } else {
                        Toast.makeText(this, "Failed to fetch documents: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        val packages = intent?.getStringExtra("packageNames")?.split(",") ?: return START_NOT_STICKY
        trackingPackageName = packages
        email = intent.getStringExtra("email")!!

        handler.post(trackingRunnable)
        return START_STICKY
    }
    fun getAppLabel(context: Context, packageName: String): String? {
        try {
            val packageManager: PackageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            return packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
    }
    private val trackingRunnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun run() {
            val foregroundApp = getForegroundApp()
            if (foregroundApp != null && trackingPackageName.contains(foregroundApp)) {
                if (!isGameForeground || currentForegroundApp != foregroundApp) {
                    // App has come to the foreground
                    isGameForeground = true
                    currentForegroundApp = foregroundApp
                    handleAppForeground(foregroundApp)
                }
                // Continue tracking time since the app is still in the foreground
                incrementGameTime(foregroundApp)
            } else {
                if (isGameForeground) {
                    // App has gone to the background
                    isGameForeground = false
                    currentForegroundApp = null
                    removeNotificationIfNeeded()
                }
            }
            // Schedule the next run
            handler.postDelayed(this, 1000)
        }
        private fun removeNotificationIfNeeded() {
            currentForegroundApp?.let {
                if (notifiedApps[it] == true) {
                    removeStickyNotification(it)
                    notifiedApps.remove(it)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleAppForeground(appName: String) {
        val db = FirebaseFirestore.getInstance()
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val tempMail=sharedPreferences.getString("email", "")

        val history = mutableListOf<History>()
        db.collection("Users")
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    for (documents in it.result!!) {
                        if (documents.data.getValue("email").toString() == tempMail) {
                            val activities = documents.get("activities") as? List<Map<Any, Any>>?
                            if (activities != null) {
                                for (activitiesMap in activities) {
                                    val activity = activitiesMap["activity"]?.toString() ?: ""
                                    val time = activitiesMap["time"]?.toString() ?: ""
                                    val date = activitiesMap["date"]?.toString() ?: ""
                                    val dateTime = activitiesMap["dateTime"]?.toString() ?: ""
                                    val activities_ = History(
                                        activity,
                                        time,
                                        date,
                                        dateTime
                                    )
                                    history.add(activities_)
                                }
                            }
                            val add = HashMap<String, Any>()
                            val formattedDate = LocalDateTime.now().format(
                                DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            val currentHour =  Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                            val currentMinute =  Calendar.getInstance().get(Calendar.MINUTE)
                            val appName_=getAppLabel(applicationContext,appName)
                            val activities_ = History(
                                "${appName_} Started",
                                "$currentHour:$currentMinute",
                                formattedDate.toString(),
                                System.currentTimeMillis().toString()
                            )
                            history.add(activities_)
                            add["activities"] = history
                            val collection2 = db.collection("Users")
                            collection2.document(documents.id)
                                .update(add)
                                .addOnSuccessListener {
                                }
                                .addOnFailureListener {
                                }
                        }
                    }
                }
            }
    }




    private fun getForegroundApp(): String? {
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - (2 * 60 * 1000) // Consider checking this time window
        val usageStats: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
        val foregroundApp = usageStats.maxByOrNull { it.lastTimeUsed }?.packageName
        Log.d("ServiceCheck", "Foreground app: $foregroundApp")
        return foregroundApp


       // return usageStats.maxByOrNull { it.lastTimeUsed }?.packageName
    }


    @SuppressLint("SuspiciousIndentation")
    private fun incrementGameTime(appName: String?) {
        Log.d("ServiceCheck","packages: $appName")

        if (appName == null) return
        val totalGameTime = prefs.getLong(appName, 0L) + 1
        prefs.edit().putLong(appName, totalGameTime).apply()


        val packages = mutableListOf<MyPackage>()

        val db = FirebaseFirestore.getInstance()

        db.collection("Users")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        if (document.data.getValue("email").toString() == email) {

                            val packagesData = document.get("packages") as? List<Map<String, Any>>

                            var found = false
                            if (packagesData != null) {
                                for (packagesdata in packagesData) {
                                    val pkg = packagesdata["packageName"]?.toString() ?: ""
                                    var time = packagesdata["time"]?.toString() ?: "0"
                                    var scheduled = packagesdata["scheduled"]?.toString() ?: "0"
                                    if (pkg == appName)
                                    {
                                        time= (time.toInt()+1).toString()
                                        found=true

                                        if(scheduled.toInt()!=0)
                                        {
                                            if (totalGameTime >= scheduled.toInt() && appName == currentForegroundApp && notifiedApps[appName] != true) {
                                                sendStickyNotification(appName)
                                                // Start the overlay service with the app name
                                                if(totalGameTime >= scheduled.toInt()) {
                                                    val intent = Intent(this, OverlayService::class.java)
                                                    intent.putExtra("appName", appName)
                                                    startService(intent)
                                                }
                                                notifiedApps[appName] = true  // Mark as notified
                                            } else if (totalGameTime < scheduled.toInt() && notifiedApps[appName] == true) {
                                                removeStickyNotification(appName)
                                                notifiedApps.remove(appName)  // Clear notified status
                                            }
                                        }

                                    }
                                    val package_ = MyPackage(
                                        pkg,
                                        scheduled,
                                        time
                                    )
                                    packages.add(package_)
                                }
                            }
                            if(found==false){
                                val package_ = MyPackage(
                                    appName,
                                    "0",
                                    "1",
                                )
                                packages.add(package_)
                            }


                        // Define a HashMap to hold the update
                        val add = HashMap<String, Any>()
                        add["packages"] = packages
                            val collection2 = db.collection("Users")
                            collection2.document(document.id)
                                .update(add)
                                .addOnSuccessListener {
                                }
                                .addOnFailureListener {
                                }
                    }
                }
                } else {
                    Toast.makeText(this, "Failed to fetch documents: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }

    }
    private fun sendStickyNotification(appName: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Time Limit Exceeded")
            .setContentText("You've been using this app for over your scheduled time!")
            .setSmallIcon(R.drawable.transparent_notification_logo)
            .setOngoing(true)  // Makes the notification sticky
            .build()
        notificationManager.notify(appName.hashCode(), notification)  // Use a unique ID for each app


    }
    private fun removeStickyNotification(appName: String?) {
        if (appName == null) return
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(appName.hashCode())  // Cancel notification using app's unique hash code
    }



    override fun onDestroy() {
        handler.removeCallbacks(trackingRunnable)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}