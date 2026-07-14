package com.julieasoreng.touchgrass.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.julieasoreng.touchgrass.ui.onboarding.OnboardingAnswers

private val Context.dataStore by preferencesDataStore(name = "bloom_prefs")

class OnboardingPreferencesRepository(private val context: Context) {

    private object Keys {
        val CURRENT_USAGE = stringPreferencesKey("current_usage")
        val TARGET_USAGE = stringPreferencesKey("target_usage")
        val SCROLL_TIMES = stringSetPreferencesKey("scroll_times")
        val REPLACEMENT_ACTIVITIES = stringSetPreferencesKey("replacement_activities")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    suspend fun saveOnboardingAnswers(answers: OnboardingAnswers) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CURRENT_USAGE] = answers.currentUsage
            prefs[Keys.TARGET_USAGE] = answers.targetUsage
            prefs[Keys.SCROLL_TIMES] = answers.scrollTimes.toSet()
            prefs[Keys.REPLACEMENT_ACTIVITIES] = answers.replacementActivities.toSet()
            prefs[Keys.ONBOARDING_COMPLETE] = true
        }
    }
}
