package app.fasting.notify

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.fasting.MainActivity
import app.fasting.R
import app.fasting.domain.Durations
import app.fasting.domain.Fast
import app.fasting.domain.formatDateTime

/** Start and end alerts. Notifications are silently skipped when the user has not allowed them. */
class FastNotifier(private val context: Context) {

    fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Fasting", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alerts when a scheduled fast starts and when a fast reaches its target"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun started(fast: Fast) {
        post(
            id = ID_START,
            title = "Fast started",
            text = "${Durations.short(fast.durationMs)} fast. Target ${formatDateTime(fast.targetEndAt)}.",
        )
    }

    fun ended(fast: Fast) {
        post(
            id = ID_END,
            title = "Fast complete",
            text = "You reached ${Durations.short(fast.durationMs)}. Time to eat, mindfully.",
        )
    }

    @SuppressLint("MissingPermission")
    private fun post(id: Int, title: String, text: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(id, notification)
    }

    private companion object {
        const val CHANNEL_ID = "fasting"
        const val ID_START = 1
        const val ID_END = 2
    }
}
