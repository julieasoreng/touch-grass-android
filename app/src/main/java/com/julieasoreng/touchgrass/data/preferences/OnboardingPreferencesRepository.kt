package com.julieasoreng.touchgrass.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.julieasoreng.touchgrass.ui.onboarding.OnboardingAnswers

private val Context.dataStore by preferencesDataStore(name = "bloom_prefs")

class OnboardingPreferencesRepository(private val context: Context) {

    private object Keys {
        val DAILY_AVERAGE_SCREEN_TIME_MILLIS = longPreferencesKey("daily_average_screen_time_millis")
        val SCREEN_TIME_DAYS_OF_DATA = intPreferencesKey("screen_time_days_of_data")
        val TARGET_USAGE = stringPreferencesKey("target_usage")
        val SCROLL_TIMES = stringSetPreferencesKey("scroll_times")
        val REPLACEMENT_ACTIVITIES = stringSetPreferencesKey("replacement_activities")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    suspend fun saveOnboardingAnswers(answers: OnboardingAnswers) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAILY_AVERAGE_SCREEN_TIME_MILLIS] = answers.dailyAverageScreenTimeMillis
            prefs[Keys.SCREEN_TIME_DAYS_OF_DATA] = answers.screenTimeDaysOfData
            prefs[Keys.TARGET_USAGE] = answers.targetUsage
            prefs[Keys.SCROLL_TIMES] = answers.scrollTimes.toSet()
            prefs[Keys.REPLACEMENT_ACTIVITIES] = answers.replacementActivities.toSet()
            prefs[Keys.ONBOARDING_COMPLETE] = true
        }
    }
}
