package app.fasting.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FastEntity::class, CravingEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fastDao(): FastDao
}
