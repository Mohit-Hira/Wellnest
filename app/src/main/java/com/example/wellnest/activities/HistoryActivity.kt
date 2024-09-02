
package com.example.wellnest.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.example.wellnest.adapters.HistoryListAdapter
import com.example.wellnest.helpers.History
import com.example.wellnest.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class HistoryActivity : AppCompatActivity() {
    val history = mutableListOf<History>()

    override fun onStart() {
        super.onStart()
    }
    override fun onStop() {
        super.onStop()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        val animFadein: Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_in)

        val decorView = window.decorView
        decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        readFireStoreData()

            val back_btn: Button = findViewById(R.id.back_btn)
            back_btn.setOnClickListener {
                back_btn.startAnimation(animFadein)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()


    }
}
    private fun readFireStoreData() {
        val db = FirebaseFirestore.getInstance()
        db.collection("Users")
            .get()
            .addOnCompleteListener {
                if(it.isSuccessful) {
                    for(document in it.result!!) {
                        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                        val tempMail=sharedPreferences.getString("email", "")
                        if(document.data.getValue("email").toString()==tempMail)
                        {
                            val activities = document.get("activities") as? List<Map<Any, Any>>?
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
                            history.reverse()
                            val listView = findViewById<ListView>(R.id.gamesListView)
                            val adapter = HistoryListAdapter(this, history)
                            listView.adapter = adapter

                        }
                    }
                }
            }
    }
}