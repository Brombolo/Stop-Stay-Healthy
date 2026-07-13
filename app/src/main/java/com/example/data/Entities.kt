package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fasting_sessions")
data class FastingSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val weight: Double? = null
)

@Entity(tableName = "weight_entries")
data class WeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val weight: Double
)

@Entity(tableName = "habit_state")
data class HabitState(
    @PrimaryKey val id: Int = 1,
    val onboardingCompleted: Boolean = false,
    val fastingEnabled: Boolean = true,
    val smokingEnabled: Boolean = false,
    val alcoholEnabled: Boolean = false,
    val sugarEnabled: Boolean = false,
    // Start dates for bad habits
    val smokingStartDate: Long = System.currentTimeMillis(),
    val alcoholStartDate: Long = System.currentTimeMillis(),
    val sugarStartDate: Long = System.currentTimeMillis(),
    // Smoking specifics
    val smokingDailyCost: Double = 5.0,
    // Language: "system", "en", "it", "es", "fr"
    val language: String = "system"
)
