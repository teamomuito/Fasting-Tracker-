package app.fasting.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.fasting.domain.CravingOutcome
import app.fasting.domain.FastStatus

@Entity(tableName = "fasts")
data class FastEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val status: FastStatus,
    val startAt: Long,
    val durationMs: Long,
    val endedAt: Long?,
    val startNotified: Boolean,
    val endNotified: Boolean,
)

@Entity(
    tableName = "cravings",
    foreignKeys = [
        ForeignKey(
            entity = FastEntity::class,
            parentColumns = ["id"],
            childColumns = ["fastId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("fastId")],
)
data class CravingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fastId: Long,
    val createdAt: Long,
    val outcome: CravingOutcome,
)

/** A fast row together with how many cravings were resisted during it. */
data class FastWithCravings(
    val id: Long,
    val status: FastStatus,
    val startAt: Long,
    val durationMs: Long,
    val endedAt: Long?,
    val startNotified: Boolean,
    val endNotified: Boolean,
    val cravingsResisted: Int,
)
