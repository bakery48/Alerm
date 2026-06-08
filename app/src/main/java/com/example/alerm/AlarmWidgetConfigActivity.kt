package com.example.alerm

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alerm.data.Alarm
import com.example.alerm.data.AlarmDatabase
import com.example.alerm.data.AlarmRepository
import com.example.alerm.databinding.ActivityWidgetConfigBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmWidgetConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWidgetConfigBinding
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWidgetConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cancel result by default (user dismisses without selecting)
        setResult(RESULT_CANCELED)

        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setSupportActionBar(binding.toolbarWidgetConfig)

        val repo = AlarmRepository(AlarmDatabase.getDatabase(this).alarmDao())
        lifecycleScope.launch {
            val alarms = repo.allAlarms.first()
            if (alarms.isEmpty()) {
                finish()
                return@launch
            }
            binding.rvWidgetAlarms.layoutManager = LinearLayoutManager(this@AlarmWidgetConfigActivity)
            binding.rvWidgetAlarms.adapter = AlarmPickerAdapter(alarms) { alarm ->
                onAlarmSelected(alarm)
            }
        }
    }

    private fun onAlarmSelected(alarm: Alarm) {
        WidgetPrefs.setAlarmId(this, appWidgetId, alarm.id)

        val manager = AppWidgetManager.getInstance(this)
        AlarmSingleWidgetProvider.updateWidget(this, manager, appWidgetId, alarm)

        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
    }

    private inner class AlarmPickerAdapter(
        private val alarms: List<Alarm>,
        private val onPick: (Alarm) -> Unit
    ) : RecyclerView.Adapter<AlarmPickerAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvTime: TextView = view.findViewById(R.id.tvConfigTime)
            val tvLabel: TextView = view.findViewById(R.id.tvConfigLabel)
            val tvStatus: TextView = view.findViewById(R.id.tvConfigStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_widget_config_alarm, parent, false)
        )

        override fun getItemCount() = alarms.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val alarm = alarms[position]
            holder.tvTime.text = alarm.formattedTime()
            holder.tvLabel.text = alarm.label.ifBlank { "Alarm" }
            holder.tvStatus.text = if (alarm.isEnabled) "ON" else "OFF"
            holder.itemView.setOnClickListener { onPick(alarm) }
        }
    }
}
