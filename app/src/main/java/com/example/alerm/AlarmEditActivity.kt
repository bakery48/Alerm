package com.example.alerm

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.alerm.data.Alarm
import com.example.alerm.data.AlarmDatabase
import com.example.alerm.data.AlarmRepository
import com.example.alerm.databinding.ActivityAlarmEditBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }

    private lateinit var binding: ActivityAlarmEditBinding
    private lateinit var repository: AlarmRepository

    private var existingAlarm: Alarm? = null
    private var selectedMusicUri: String? = null

    private val musicPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            // Persist permission so we can access the file later
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // Some URIs don't support persistable permissions; we'll try anyway
            }
            selectedMusicUri = uri.toString()
            binding.btnSelectMusic.text = getFileName(uri) ?: uri.lastPathSegment ?: "Selected"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up Toolbar as ActionBar for back navigation
        setSupportActionBar(binding.toolbarEdit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarEdit.setNavigationOnClickListener { finish() }

        repository = AlarmRepository(AlarmDatabase.getDatabase(this).alarmDao())

        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)

        if (alarmId != -1) {
            supportActionBar?.title = "Edit Alarm"
            loadExistingAlarm(alarmId)
        } else {
            supportActionBar?.title = "New Alarm"
            setupNewAlarm()
        }

        setupButtons()
    }

    private fun loadExistingAlarm(alarmId: Int) {
        lifecycleScope.launch {
            val alarm = withContext(Dispatchers.IO) { repository.getAlarmById(alarmId) }
            if (alarm == null) {
                Toast.makeText(this@AlarmEditActivity, "Alarm not found", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            existingAlarm = alarm
            selectedMusicUri = alarm.musicUri
            populateFields(alarm)
            binding.btnDelete.isEnabled = true
        }
    }

    private fun setupNewAlarm() {
        binding.timePicker.setIs24HourView(true)
        binding.timePicker.hour = 8
        binding.timePicker.minute = 0
        binding.btnDelete.isEnabled = false
    }

    private fun populateFields(alarm: Alarm) {
        binding.timePicker.setIs24HourView(true)
        binding.timePicker.hour = alarm.hour
        binding.timePicker.minute = alarm.minute
        binding.etLabel.setText(alarm.label)

        // Set day checkboxes
        binding.cbMon.isChecked = (alarm.days and Alarm.MON) != 0
        binding.cbTue.isChecked = (alarm.days and Alarm.TUE) != 0
        binding.cbWed.isChecked = (alarm.days and Alarm.WED) != 0
        binding.cbThu.isChecked = (alarm.days and Alarm.THU) != 0
        binding.cbFri.isChecked = (alarm.days and Alarm.FRI) != 0
        binding.cbSat.isChecked = (alarm.days and Alarm.SAT) != 0
        binding.cbSun.isChecked = (alarm.days and Alarm.SUN) != 0
        binding.switchHeadphoneOnly.isChecked = alarm.headphoneOnly

        // Show selected music file name
        if (!alarm.musicUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(alarm.musicUri)
                val name = getFileName(uri) ?: uri.lastPathSegment ?: "Selected"
                binding.btnSelectMusic.text = name
            } catch (e: Exception) {
                binding.btnSelectMusic.text = getString(R.string.select_music)
            }
        }
    }

    private fun setupButtons() {
        binding.btnSelectMusic.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "audio/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            musicPickerLauncher.launch(intent)
        }

        binding.btnSave.setOnClickListener { saveAlarm() }

        binding.btnDelete.setOnClickListener {
            existingAlarm?.let { alarm ->
                AlertDialog.Builder(this)
                    .setTitle(R.string.delete_alarm)
                    .setMessage(R.string.delete_alarm_confirm)
                    .setPositiveButton(R.string.delete) { _, _ -> deleteAlarm(alarm) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
    }

    private fun buildDaysBitmask(): Int {
        var days = 0
        if (binding.cbMon.isChecked) days = days or Alarm.MON
        if (binding.cbTue.isChecked) days = days or Alarm.TUE
        if (binding.cbWed.isChecked) days = days or Alarm.WED
        if (binding.cbThu.isChecked) days = days or Alarm.THU
        if (binding.cbFri.isChecked) days = days or Alarm.FRI
        if (binding.cbSat.isChecked) days = days or Alarm.SAT
        if (binding.cbSun.isChecked) days = days or Alarm.SUN
        return days
    }

    private fun saveAlarm() {
        val hour = binding.timePicker.hour
        val minute = binding.timePicker.minute
        val label = binding.etLabel.text.toString().trim()
        val days = buildDaysBitmask()
        val headphoneOnly = binding.switchHeadphoneOnly.isChecked

        lifecycleScope.launch {
            val alarm = if (existingAlarm != null) {
                existingAlarm!!.copy(
                    label = label,
                    hour = hour,
                    minute = minute,
                    days = days,
                    musicUri = selectedMusicUri,
                    isEnabled = true,
                    headphoneOnly = headphoneOnly
                )
            } else {
                val count = withContext(Dispatchers.IO) { repository.getAlarmCount() }
                if (count >= 5) {
                    Toast.makeText(this@AlarmEditActivity, getString(R.string.max_alarms_reached), Toast.LENGTH_SHORT).show()
                    return@launch
                }
                Alarm(
                    label = label,
                    hour = hour,
                    minute = minute,
                    days = days,
                    musicUri = selectedMusicUri,
                    isEnabled = true,
                    headphoneOnly = headphoneOnly
                )
            }

            val alarmToSchedule = withContext(Dispatchers.IO) {
                if (existingAlarm != null) {
                    repository.updateAlarm(alarm)
                    alarm
                } else {
                    val newId = repository.insertAlarm(alarm).toInt()
                    alarm.copy(id = newId)
                }
            }

            // Schedule or reschedule alarm with correct id
            AlarmScheduler.scheduleAlarm(this@AlarmEditActivity, alarmToSchedule)
            AlarmWidgetProvider.updateAllWidgets(this@AlarmEditActivity)

            Toast.makeText(this@AlarmEditActivity, getString(R.string.alarm_saved), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun deleteAlarm(alarm: Alarm) {
        lifecycleScope.launch {
            AlarmScheduler.cancelAlarm(this@AlarmEditActivity, alarm)
            withContext(Dispatchers.IO) { repository.deleteAlarm(alarm) }
            AlarmWidgetProvider.updateAllWidgets(this@AlarmEditActivity)
            Toast.makeText(this@AlarmEditActivity, getString(R.string.alarm_deleted), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun getFileName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (nameIndex >= 0) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
