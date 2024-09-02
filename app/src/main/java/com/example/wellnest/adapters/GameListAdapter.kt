package com.example.wellnest.adapters

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.wellnest.helpers.History
import com.example.wellnest.services.GameTimeTrackerService
import com.example.wellnest.R
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class GameListAdapter(private val context: Context, private val apps: List<ApplicationInfo>) :
    ArrayAdapter<ApplicationInfo>(context, R.layout.list_item_layout_play, apps) {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = inflater.inflate(R.layout.list_item_layout_play, parent, false)
        val appLogo = rowView.findViewById<ImageView>(R.id.app_logo)
        val appName = rowView.findViewById<TextView>(R.id.app_name)
        val appTime = rowView.findViewById<TextView>(R.id.app_time)
        val appPlay = rowView.findViewById<TextView>(R.id.app_playBtn)
        val appInfo = apps[position]
        val packageName = appInfo.packageName
        appLogo.setImageDrawable(context.packageManager.getApplicationIcon(packageName))
        appName.text = context.packageManager.getApplicationLabel(appInfo)
        readPackageFireStoreData(packageName) { timeInSec ->
            val hours = timeInSec.toInt() / 3600
            val minutes = (timeInSec.toInt() % 3600) / 60
            val seconds = timeInSec.toInt() % 60

            appTime.text = when {
                hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
                minutes > 0 -> String.format("%d m:%02d s", minutes, seconds)
                else -> String.format("%d s", seconds)
            }
        }
       val b: Boolean = checkAppIsGame(context, appInfo.packageName)
        if (b) {
            appPlay.text="Play"
        }
        else
        {
            appPlay.text="Use"
        }

        appPlay.setOnClickListener{
                launchGame(packageName)

        }
        return rowView
    }

    private fun readPackageFireStoreData(pkgName: String, callback: (String) -> Unit) {
        var pkgTime = "0"
        val db = FirebaseFirestore.getInstance()
        db.collection("Users")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    val tempMail = sharedPreferences.getString("email", "")
                    for (document in task.result!!) {
                        if (document.data["email"].toString() == tempMail) {
                            val packagesData = document.get("packages") as? List<Map<String, Any>>
                            if (packagesData != null) {
                                for (packageData in packagesData) {
                                    val pkg = packageData["packageName"]?.toString() ?: ""
                                    val time = packageData["time"]?.toString() ?: "0"
                                    if (pkgName == pkg) {
                                        pkgTime = time
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
                callback(pkgTime)
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
    private fun launchGame(game: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(game)
        if (intent != null) {
            val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val tempMail=sharedPreferences.getString("email", "")
            context.startActivity(intent)
//            if (isServiceRunning(GameTimeTrackerService::class.java)) {
//                context.stopService(Intent(context, GameTimeTrackerService::class.java))
//            }
//            val serviceIntent = Intent(context, GameTimeTrackerService::class.java)
//            serviceIntent.putExtra("packageName", game)
//            serviceIntent.putExtra("email", tempMail)
//            context.startService(serviceIntent)
        } else {
            Toast.makeText(context, "Unable to launch ${game}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
