package com.julieasoreng.touchgrass.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.julieasoreng.touchgrass.data.usage.ScrollTimePattern
import com.julieasoreng.touchgrass.ui.onboarding.OnboardingAnswers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "bloom_prefs")

class OnboardingPreferencesRepository(private val context: Context) {

    private object Keys {
        val DAILY_AVERAGE_SCREEN_TIME_MILLIS = longPreferencesKey("daily_average_screen_time_millis")
        val SCREEN_TIME_DAYS_OF_DATA = intPreferencesKey("screen_time_days_of_data")
        val TARGET_SCREEN_TIME_MILLIS = longPreferencesKey("target_screen_time_millis")
        val SCROLL_TIME_PATTERN = stringPreferencesKey("scroll_time_pattern")
        val REPLACEMENT_ACTIVITIES = stringSetPreferencesKey("replacement_activities")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    /** The user's dominant scrolling time-of-day, available app-wide (e.g. to time kit nudges). */
    val scrollTimePattern: Flow<ScrollTimePattern?> = context.dataStore.data.map { prefs ->
        prefs[Keys.SCROLL_TIME_PATTERN]?.let { name -> runCatching { ScrollTimePattern.valueOf(name) }.getOrNull() }
    }

    suspend fun saveOnboardingAnswers(answers: OnboardingAnswers) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAILY_AVERAGE_SCREEN_TIME_MILLIS] = answers.dailyAverageScreenTimeMillis
            prefs[Keys.SCREEN_TIME_DAYS_OF_DATA] = answers.screenTimeDaysOfData
            answers.targetScreenTimeMillis?.let { prefs[Keys.TARGET_SCREEN_TIME_MILLIS] = it }
            answers.scrollTimePattern?.let { prefs[Keys.SCROLL_TIME_PATTERN] = it.name }
            prefs[Keys.REPLACEMENT_ACTIVITIES] = answers.replacementActivities.toSet()
            prefs[Keys.ONBOARDING_COMPLETE] = true
        }
    }
}
