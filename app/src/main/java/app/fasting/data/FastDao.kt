package app.fasting.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FastDao {

    @Query(
        """
        SELECT f.*,
            (SELECT COUNT(*) FROM cravings c WHERE c.fastId = f.id AND c.outcome = 'RESISTED') AS cravingsResisted
        FROM fasts f
        ORDER BY f.startAt DESC
        """,
    )
    fun observeAll(): Flow<List<FastWithCravings>>

    @Query("SELECT * FROM fasts WHERE status != 'COMPLETED'")
    suspend fun pending(): List<FastEntity>

    @Query("SELECT * FROM fasts WHERE id = :id")
    suspend fun byId(id: Long): FastEntity?

    @Insert
    suspend fun insertFast(fast: FastEntity): Long

    @Update
    suspend fun updateFast(fast: FastEntity)

    @Query("DELETE FROM fasts WHERE id = :id")
    suspend fun deleteFast(id: Long)

    @Insert
    suspend fun insertCraving(craving: CravingEntity)

    @Query("SELECT COUNT(*) FROM cravings")
    suspend fun cravingCount(): Int
}
