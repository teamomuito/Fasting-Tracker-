package app.fasting.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Wakes the app at the next fast transition (a scheduled start or a target end). Only one fast can be
 * pending at a time, so one alarm slot is enough. Exact timing needs the user to allow exact alarms on
 * Android 12+; without it the alarm is inexact and may arrive a few minutes late.
 */
class FastAlarms(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** Points the alarm at [atMillis], or clears it when there is nothing pending. */
    fun arm(atMillis: Long?) {
        val pending = pendingIntent()
        alarmManager.cancel(pending)
        if (atMillis == null) return
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
        }
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, FastAlarmReceiver::class.java).setAction(FastAlarmReceiver.ACTION_SYNC)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val REQUEST_CODE = 1
    }
}
