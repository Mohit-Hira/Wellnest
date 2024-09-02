package com.example.wellnest.adapters

import android.app.ActivityManager
import android.app.TimePickerDialog
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

class ScheduleListAdapter(private val context: Context, private val apps: List<ApplicationInfo>) :
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

        appPlay.text="Schedule"


        appPlay.setOnClickListener {
            // Get current time
            val now = Calendar.getInstance()
            val timePicker = TimePickerDialog(context, { _, selectedHour, selectedMinute ->
                // Convert selected hour and minute to seconds
                val totalSeconds = selectedHour * 3600 + selectedMinute * 60

                // Display the formatted time in TextView or handle as needed
                updateFirestoreWithPlayTime(packageName, totalSeconds)

                // Here you can also update Firestore or handle the time value as required
                // updateFirestore(packageName, totalSeconds)

            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true)
            timePicker.show()
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

    private fun updateFirestoreWithPlayTime(packageName: String, totalSeconds: Int) {
        val db = FirebaseFirestore.getInstance()
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("email", "") ?: return

        db.collection("Users")
            .whereEqualTo("email", userEmail)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        val packages = document.get("packages") as? List<Map<String, Any>>

                        val updatedPackages = mutableListOf<Map<String, Any>>()

                        var found=false
                        packages?.forEach { packageData ->
                            val pkg = packageData["packageName"]?.toString() ?: ""
                            val time = packageData["time"]?.toString()?.toInt() ?: 0
//                            val scheduled =
//                                packageData["scheduled"] ?: "" // Default to false if not present

                            if (pkg == packageName) {
                                found=true
                                val updatedPackage = mapOf(
                                    "packageName" to pkg,
                                    "time" to time.toString(),
                                    "scheduled" to totalSeconds.toString()
                                )
                                updatedPackages.add(updatedPackage)
                            } else {
                                updatedPackages.add(packageData)
                            }
                        }
                        if(found==false)
                        {
                            val updatedPackage = mapOf(
                                "packageName" to packageName,
                                "time" to "0",
                                "scheduled" to totalSeconds.toString()
                            )
                            updatedPackages.add(updatedPackage)
                        }

                        val updateData = mapOf(
                            "packages" to updatedPackages
                        )

                        db.collection("Users")
                            .document(document.id)
                            .update(updateData)
                            .addOnSuccessListener {
//                                Toast.makeText(context, "Play time updated in Firestore", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
//                                Toast.makeText(context, "Failed to update Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Failed to fetch user document: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

