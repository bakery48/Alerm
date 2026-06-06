package com.example.alerm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.alerm.data.Alarm

class AlarmAdapter(
    private val onToggle: (Alarm, Boolean) -> Unit,
    private val onItemClick: (Alarm) -> Unit
) : ListAdapter<Alarm, AlarmAdapter.AlarmViewHolder>(AlarmDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Enforce max 5 alarms at the adapter level too
    override fun getItemCount(): Int = minOf(super.getItemCount(), 5)

    inner class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTime: TextView = itemView.findViewById(R.id.tvAlarmTime)
        private val tvLabel: TextView = itemView.findViewById(R.id.tvAlarmLabel)
        private val tvDays: TextView = itemView.findViewById(R.id.tvAlarmDays)
        private val switchEnabled: Switch = itemView.findViewById(R.id.switchAlarmEnabled)

        fun bind(alarm: Alarm) {
            tvTime.text = alarm.formattedTime()
            tvLabel.text = if (alarm.label.isBlank()) "Alarm" else alarm.label
            tvDays.text = alarm.activeDaysLabel()

            // Set switch without triggering listener
            switchEnabled.setOnCheckedChangeListener(null)
            switchEnabled.isChecked = alarm.isEnabled
            switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                onToggle(alarm, isChecked)
            }

            itemView.setOnClickListener {
                onItemClick(alarm)
            }
        }
    }

    private class AlarmDiffCallback : DiffUtil.ItemCallback<Alarm>() {
        override fun areItemsTheSame(oldItem: Alarm, newItem: Alarm) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Alarm, newItem: Alarm) = oldItem == newItem
    }
}
