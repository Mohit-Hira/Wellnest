package com.example.wellnest.adapters
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.example.wellnest.helpers.History
import com.example.wellnest.R


class HistoryListAdapter(private val context: Context, private val history: List<History>) :
    ArrayAdapter<History>(context, R.layout.list_item_layout_history, history) {
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = inflater.inflate(R.layout.list_item_layout_history, parent, false)
        val historyActivity = history[position]
        val activity_txt = rowView.findViewById<TextView>(R.id.activity_txt)
        val activity_date = rowView.findViewById<TextView>(R.id.activity_date)
        val activity_hrs = rowView.findViewById<TextView>(R.id.activity_hrs)
        activity_txt.text = historyActivity.activity
        activity_date.text = "On ${historyActivity.date}"
        activity_hrs.text = "At ${historyActivity.time}"
        return rowView
    }
}