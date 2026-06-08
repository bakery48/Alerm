package com.example.alerm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.alerm.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: AlarmViewModel by viewModels()
    private lateinit var adapter: AlarmAdapter

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 許可/拒否どちらでも続行 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupFab()
        setupStopBanner()
        observeAlarms()
        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        updateStopBanner()
    }

    private fun setupRecyclerView() {
        adapter = AlarmAdapter(
            onToggle = { alarm, enabled -> viewModel.toggleAlarm(alarm, enabled) },
            onItemClick = { alarm -> openEditActivity(alarm.id) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddAlarm.setOnClickListener {
            lifecycleScope.launch {
                val count = viewModel.getAlarmCount()
                if (count >= 5) {
                    Toast.makeText(this@MainActivity, getString(R.string.max_alarms_reached), Toast.LENGTH_SHORT).show()
                } else {
                    openEditActivity(alarmId = -1)
                }
            }
        }
    }

    private fun observeAlarms() {
        lifecycleScope.launch {
            viewModel.alarms.collect { alarms ->
                adapter.submitList(alarms.take(5))
                // Disable FAB when 5 alarms exist
                binding.fabAddAlarm.isEnabled = alarms.size < 5
                binding.fabAddAlarm.alpha = if (alarms.size < 5) 1f else 0.4f
            }
        }
    }

    private fun setupStopBanner() {
        binding.btnStopAlarm.setOnClickListener {
            val intent = Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_STOP
            }
            startService(intent)
            updateStopBanner()
        }
    }

    private fun updateStopBanner() {
        binding.stopAlarmBanner.visibility =
            if (AlarmService.isRunning) View.VISIBLE else View.GONE
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun openEditActivity(alarmId: Int) {
        val intent = Intent(this, AlarmEditActivity::class.java)
        if (alarmId != -1) {
            intent.putExtra(AlarmEditActivity.EXTRA_ALARM_ID, alarmId)
        }
        startActivity(intent)
    }
}
