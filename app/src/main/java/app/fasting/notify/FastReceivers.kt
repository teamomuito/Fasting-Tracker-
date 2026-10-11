package app.fasting.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.fasting.FastingApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Fires at a fast transition. Re-syncs, which sends any due alert and re-arms the next one. */
class FastAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        syncInBackground(context, goAsync())
    }

    companion object {
        const val ACTION_SYNC = "app.fasting.action.SYNC"
    }
}

/** After a reboot or an app update the alarm is gone, so re-arm it from the stored fasts. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        syncInBackground(context, goAsync())
    }
}

private fun syncInBackground(context: Context, result: BroadcastReceiver.PendingResult) {
    val repository = (context.applicationContext as FastingApplication).container.repository
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            repository.sync()
        } finally {
            result.finish()
        }
    }
}
