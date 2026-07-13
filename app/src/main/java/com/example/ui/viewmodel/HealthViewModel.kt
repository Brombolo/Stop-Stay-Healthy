package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.widget.WidgetUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class Screen {
    Onboarding, Fasting, Smoking, Alcohol, Sugar, Statistics, Settings
}

class HealthViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    private val repository = HealthRepository(database)

    // Room flows
    val fastingSessions: StateFlow<List<FastingSession>> = repository.fastingSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weightEntries: StateFlow<List<WeightEntry>> = repository.weightEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habitState: StateFlow<HabitState> = repository.habitState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitState())

    // Active screen navigation state
    var currentScreen by mutableStateOf(Screen.Onboarding)
        private set

    // Shared Preference states for active trackers (so widget and app are in sync)
    private val prefs = context.getSharedPreferences("stop_stay_healthy_prefs", Context.MODE_PRIVATE)

    var fastingStartTime by mutableStateOf(-1L)
        private set

    var smokingStartTime by mutableStateOf(-1L)
        private set

    var alcoholStartTime by mutableStateOf(-1L)
        private set

    var sugarStartTime by mutableStateOf(-1L)
        private set

    init {
        // Load initial values from SharedPreferences
        fastingStartTime = prefs.getLong("fasting_start_time", -1L)
        smokingStartTime = prefs.getLong("smoking_start_time", -1L)
        alcoholStartTime = prefs.getLong("alcohol_start_time", -1L)
        sugarStartTime = prefs.getLong("sugar_start_time", -1L)

        // React to database habitState to set initial screen and synchronize preferences
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getHabitStateDirect()
            
            // Sync preferences
            prefs.edit().apply {
                putBoolean("smoking_enabled", state.smokingEnabled)
                putBoolean("alcohol_enabled", state.alcoholEnabled)
                putBoolean("sugar_enabled", state.sugarEnabled)
                putLong("smoking_start_time", state.smokingStartDate)
                putLong("alcohol_start_time", state.alcoholStartDate)
                putLong("sugar_start_time", state.sugarStartDate)
                putString("language", state.language)
                apply()
            }
            smokingStartTime = state.smokingStartDate
            alcoholStartTime = state.alcoholStartDate
            sugarStartTime = state.sugarStartDate

            viewModelScope.launch(Dispatchers.Main) {
                if (!state.onboardingCompleted) {
                    currentScreen = Screen.Onboarding
                } else {
                    // "se è stato selezionato il digiuno intermittente l'app dovrà essere focalizzata all'apertura su questo e tenere le altre voci accessibili nel menu laterale."
                    currentScreen = when {
                        state.fastingEnabled -> Screen.Fasting
                        state.smokingEnabled -> Screen.Smoking
                        state.alcoholEnabled -> Screen.Alcohol
                        state.sugarEnabled -> Screen.Sugar
                        else -> Screen.Settings
                    }
                }
            }
        }
    }

    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    fun completeOnboarding(
        fasting: Boolean,
        smoking: Boolean,
        alcohol: Boolean,
        sugar: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = HabitState(
                id = 1,
                onboardingCompleted = true,
                fastingEnabled = fasting,
                smokingEnabled = smoking,
                alcoholEnabled = alcohol,
                sugarEnabled = sugar,
                smokingStartDate = -1L,
                alcoholStartDate = -1L,
                sugarStartDate = -1L
            )
            repository.updateHabitState(state)

            // Update preferences
            prefs.edit().apply {
                putBoolean("smoking_enabled", smoking)
                putBoolean("alcohol_enabled", alcohol)
                putBoolean("sugar_enabled", sugar)
                putLong("smoking_start_time", -1L)
                putLong("alcohol_start_time", -1L)
                putLong("sugar_start_time", -1L)
                apply()
            }

            smokingStartTime = -1L
            alcoholStartTime = -1L
            sugarStartTime = -1L

            WidgetUtils.triggerUpdate(context)

            viewModelScope.launch(Dispatchers.Main) {
                currentScreen = if (fasting) Screen.Fasting else {
                    when {
                        smoking -> Screen.Smoking
                        alcohol -> Screen.Alcohol
                        sugar -> Screen.Sugar
                        else -> Screen.Settings
                    }
                }
            }
        }
    }

    fun startFasting(startTime: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit().putLong("fasting_start_time", startTime).apply()
            fastingStartTime = startTime
            WidgetUtils.triggerUpdate(context)
        }
    }

    fun stopFasting(endTime: Long, weight: Double?) {
        val start = fastingStartTime
        if (start == -1L) return

        viewModelScope.launch(Dispatchers.IO) {
            // Save to database
            val session = FastingSession(
                startTimestamp = start,
                endTimestamp = endTime,
                weight = weight
            )
            repository.insertFastingSession(session)

            // Save weight to history if entered
            if (weight != null) {
                val weightEntry = WeightEntry(
                    timestamp = endTime,
                    weight = weight
                )
                repository.insertWeight(weightEntry)
            }

            // Reset active fasting state
            prefs.edit().putLong("fasting_start_time", -1L).apply()
            fastingStartTime = -1L
            WidgetUtils.triggerUpdate(context)
        }
    }

    fun updateFastingStartTime(newTime: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit().putLong("fasting_start_time", newTime).apply()
            fastingStartTime = newTime
            WidgetUtils.triggerUpdate(context)
        }
    }

    fun updateFastingSession(session: FastingSession) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFastingSession(session)
        }
    }

    fun deleteFastingSession(session: FastingSession) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFastingSession(session)
        }
    }

    // Bad habits controls
    fun startSmoking(startTime: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getHabitStateDirect().copy(smokingStartDate = startTime)
            repository.updateHabitState(state)
            prefs.edit().putLong("smoking_start_time", startTime).apply()
            smokingStartTime = startTime
            WidgetUtils.triggerUpdate(context)
        }
    }

    fun stopSmoking() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getHabitStateDirect().copy(smokingStartDate = -1L)
            repository.updateHabitState(state)
            prefs.edit().putLong("smoking_start_time", -1L).apply()
            smokingStartTime = -1L
            WidgetUtils.triggerUpdate(context)
        }
    }

    fun startAlcohol(startTime: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getHabitStateDirect().copy(alcoholStartDate = startTime)
            repository.updateHabitState(state)
            prefs.edit().putLong("alcohol_start_time", startTime).apply()
            alcoholStartTime = startTime
            WidgetUtils.triggerUpdate(context)
        }
    }

    fun stopAlcohol() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getHabitStateDirect().copy(alcoholStartDate = -1L)
            repository.updateHabitState(state)
            prefs.edit().putLong("alcohol_start_time", -1L).apply()
            alcoholStartTime = -1L
            WidgetUtils.triggerUpdate(context)
        }
    }

    fun startSugar(startTime: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getHabitStateDirect().copy(sugarStartDate = startTime)
            repository.updateHabitState(state)
            prefs.edit().putLong("sugar_start_time", startTime).apply()
            sugarStartTime = startTime
            WidgetUtils.triggerUpdate(context)
        }
    }

    fun stopSugar() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getHabitStateDirect().copy(sugarStartDate = -1L)
            repository.updateHabitState(state)
            prefs.edit().putLong("sugar_start_time", -1L).apply()
            sugarStartTime = -1L
            WidgetUtils.triggerUpdate(context)
        }
    }

    fun resetSmoking() {
        val now = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getHabitStateDirect().copy(smokingStartDate = now)
            repository.updateHabitState(state)
            prefs.edit().putLong("smoking_start_time", now).apply()
            smokingStartTime = now
            WidgetUtils.triggerUpdate(context)
        }
    }

    fun resetAlcohol() {
        val now = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getHabitStateDirect().copy(alcoholStartDate = now)
            repository.updateHabitState(state)
            prefs.edit().putLong("alcohol_start_time", now).apply()
            alcoholStartTime = now
            WidgetUtils.triggerUpdate(context)
        }
    }

    fun resetSugar() {
        val now = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getHabitStateDirect().copy(sugarStartDate = now)
            repository.updateHabitState(state)
            prefs.edit().putLong("sugar_start_time", now).apply()
            sugarStartTime = now
            WidgetUtils.triggerUpdate(context)
        }
    }

    // Toggle modules dynamically in Settings
    fun toggleFastingEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = repository.getHabitStateDirect()
            val activeCount = (if (enabled) 1 else 0) +
                              (if (currentState.smokingEnabled) 1 else 0) +
                              (if (currentState.alcoholEnabled) 1 else 0) +
                              (if (currentState.sugarEnabled) 1 else 0)
            if (activeCount == 0) return@launch

            val state = currentState.copy(fastingEnabled = enabled)
            repository.updateHabitState(state)
            if (!enabled) {
                prefs.edit().putLong("fasting_start_time", -1L).apply()
                fastingStartTime = -1L
            }
            WidgetUtils.triggerUpdate(context)
        }
    }

    fun toggleSmokingEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = repository.getHabitStateDirect()
            val activeCount = (if (currentState.fastingEnabled) 1 else 0) +
                              (if (enabled) 1 else 0) +
                              (if (currentState.alcoholEnabled) 1 else 0) +
                              (if (currentState.sugarEnabled) 1 else 0)
            if (activeCount == 0) return@launch

            val state = currentState.copy(
                smokingEnabled = enabled,
                smokingStartDate = if (enabled) -1L else 0L
            )
            repository.updateHabitState(state)

            prefs.edit().apply {
                putBoolean("smoking_enabled", enabled)
                putLong("smoking_start_time", state.smokingStartDate)
                apply()
            }
            smokingStartTime = state.smokingStartDate
            WidgetUtils.triggerUpdate(context)
        }
    }

    fun toggleAlcoholEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = repository.getHabitStateDirect()
            val activeCount = (if (currentState.fastingEnabled) 1 else 0) +
                              (if (currentState.smokingEnabled) 1 else 0) +
                              (if (enabled) 1 else 0) +
                              (if (currentState.sugarEnabled) 1 else 0)
            if (activeCount == 0) return@launch

            val state = currentState.copy(
                alcoholEnabled = enabled,
                alcoholStartDate = if (enabled) -1L else 0L
            )
            repository.updateHabitState(state)

            prefs.edit().apply {
                putBoolean("alcohol_enabled", enabled)
                putLong("alcohol_start_time", state.alcoholStartDate)
                apply()
            }
            alcoholStartTime = state.alcoholStartDate
            WidgetUtils.triggerUpdate(context)
        }
    }

    fun toggleSugarEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = repository.getHabitStateDirect()
            val activeCount = (if (currentState.fastingEnabled) 1 else 0) +
                              (if (currentState.smokingEnabled) 1 else 0) +
                              (if (currentState.alcoholEnabled) 1 else 0) +
                              (if (enabled) 1 else 0)
            if (activeCount == 0) return@launch

            val state = currentState.copy(
                sugarEnabled = enabled,
                sugarStartDate = if (enabled) -1L else 0L
            )
            repository.updateHabitState(state)

            prefs.edit().apply {
                putBoolean("sugar_enabled", enabled)
                putLong("sugar_start_time", state.sugarStartDate)
                apply()
            }
            sugarStartTime = state.sugarStartDate
            WidgetUtils.triggerUpdate(context)
        }
    }

    fun updateSmokingCost(cost: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getHabitStateDirect().copy(smokingDailyCost = cost)
            repository.updateHabitState(state)
        }
    }

    // Weight management
    fun addWeightEntry(weight: Double, timestamp: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertWeight(WeightEntry(weight = weight, timestamp = timestamp))
        }
    }

    fun editWeightEntry(entry: WeightEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateWeight(entry)
        }
    }

    fun deleteWeightEntry(entry: WeightEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteWeight(entry)
        }
    }

    // Language setting
    fun setLanguage(langCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = repository.getHabitStateDirect().copy(language = langCode)
            repository.updateHabitState(state)
            prefs.edit().putString("language", langCode).apply()
            WidgetUtils.triggerUpdate(context)
        }
    }

    // Reset all data (deletes all tables, resets preferences)
    fun resetApplication() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetAllData()
            prefs.edit().clear().apply()
            fastingStartTime = -1L
            smokingStartTime = -1L
            alcoholStartTime = -1L
            sugarStartTime = -1L
            WidgetUtils.triggerUpdate(context)
            viewModelScope.launch(Dispatchers.Main) {
                currentScreen = Screen.Onboarding
            }
        }
    }
}
