package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FastingDao {
    @Query("SELECT * FROM fasting_sessions ORDER BY endTimestamp DESC")
    fun getAllSessionsFlow(): Flow<List<FastingSession>>

    @Query("SELECT * FROM fasting_sessions ORDER BY endTimestamp DESC")
    suspend fun getAllSessions(): List<FastingSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FastingSession)

    @Update
    suspend fun updateSession(session: FastingSession)

    @Delete
    suspend fun deleteSession(session: FastingSession)

    @Query("DELETE FROM fasting_sessions")
    suspend fun clearAll()
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_entries ORDER BY timestamp DESC")
    fun getAllWeightsFlow(): Flow<List<WeightEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weightEntry: WeightEntry)

    @Update
    suspend fun updateWeight(weightEntry: WeightEntry)

    @Delete
    suspend fun deleteWeight(weightEntry: WeightEntry)

    @Query("DELETE FROM weight_entries")
    suspend fun clearAll()
}

@Dao
interface HabitStateDao {
    @Query("SELECT * FROM habit_state WHERE id = 1 LIMIT 1")
    fun getHabitStateFlow(): Flow<HabitState?>

    @Query("SELECT * FROM habit_state WHERE id = 1 LIMIT 1")
    suspend fun getHabitState(): HabitState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateState(habitState: HabitState)
}
