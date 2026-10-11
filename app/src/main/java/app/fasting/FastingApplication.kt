package app.fasting

import android.app.Application
import android.content.Context
import androidx.room.Room
import app.fasting.data.AppDatabase
import app.fasting.data.FastRepository
import app.fasting.notify.FastAlarms
import app.fasting.notify.FastNotifier

class FastingApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        container.notifier.createChannel()
    }
}

/** Hand-wired dependencies. There are few enough that a DI framework would only add noise. */
class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(context, AppDatabase::class.java, "fasting.db").build()
    val notifier = FastNotifier(context)
    val repository = FastRepository(database.fastDao(), FastAlarms(context), notifier)
}
