package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HealthRepository(private val database: AppDatabase) {
    private val fastingDao = database.fastingDao()
    private val weightDao = database.weightDao()
    private val habitStateDao = database.habitStateDao()

    val fastingSessions: Flow<List<FastingSession>> = fastingDao.getAllSessionsFlow()
    val weightEntries: Flow<List<WeightEntry>> = weightDao.getAllWeightsFlow()

    val habitState: Flow<HabitState> = habitStateDao.getHabitStateFlow().map { state ->
        state ?: HabitState()
    }

    suspend fun getHabitStateDirect(): HabitState {
        return habitStateDao.getHabitState() ?: HabitState().also {
            habitStateDao.insertOrUpdateState(it)
        }
    }

    suspend fun insertFastingSession(session: FastingSession) {
        fastingDao.insertSession(session)
    }

    suspend fun updateFastingSession(session: FastingSession) {
        fastingDao.updateSession(session)
    }

    suspend fun deleteFastingSession(session: FastingSession) {
        fastingDao.deleteSession(session)
    }

    suspend fun insertWeight(weightEntry: WeightEntry) {
        weightDao.insertWeight(weightEntry)
    }

    suspend fun updateWeight(weightEntry: WeightEntry) {
        weightDao.updateWeight(weightEntry)
    }

    suspend fun deleteWeight(weightEntry: WeightEntry) {
        weightDao.deleteWeight(weightEntry)
    }

    suspend fun updateHabitState(state: HabitState) {
        habitStateDao.insertOrUpdateState(state)
    }

    suspend fun resetAllData() {
        database.clearAllTables()
        // Re-insert standard state
        habitStateDao.insertOrUpdateState(HabitState(id = 1))
    }
}
