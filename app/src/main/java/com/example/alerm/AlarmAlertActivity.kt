package com.example.alerm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class AlarmAlertActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LABEL = "alert_label"
        const val EXTRA_HOUR = "alert_hour"
        const val EXTRA_MINUTE = "alert_minute"

        fun createIntent(context: Context, label: String, hour: Int, minute: Int): Intent =
            Intent(context, AlarmAlertActivity::class.java).apply {
                putExtra(EXTRA_LABEL, label)
                putExtra(EXTRA_HOUR, hour)
                putExtra(EXTRA_MINUTE, minute)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Show over lock screen and turn on screen
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_alert)

        // Dismiss keyguard so the activity is fully visible
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        km.requestDismissKeyguard(this, null)

        val label = intent.getStringExtra(EXTRA_LABEL)?.takeIf { it.isNotBlank() } ?: "アラーム"
        val hour = intent.getIntExtra(EXTRA_HOUR, 8)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)

        findViewById<TextView>(R.id.tvAlertLabel).text = label
        findViewById<TextView>(R.id.tvAlertTime).text = formatTime(hour, minute)

        findViewById<Button>(R.id.btnStopAlert).setOnClickListener {
            stopAlarm()
        }
    }

    override fun onBackPressed() {
        // Prevent dismissing with back button — must tap Stop
    }

    private fun stopAlarm() {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP
        }
        startService(stopIntent)
        finish()
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val h = if (hour % 12 == 0) 12 else hour % 12
        val amPm = if (hour < 12) "AM" else "PM"
        return "%d:%02d %s".format(h, minute, amPm)
    }
}
