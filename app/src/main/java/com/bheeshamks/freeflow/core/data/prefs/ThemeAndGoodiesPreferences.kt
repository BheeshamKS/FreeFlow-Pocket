package com.bheeshamks.freeflow.core.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bheeshamks.freeflow.core.data.model.BloomAnimationStyle
import com.bheeshamks.freeflow.core.data.model.BloomColorPreset
import com.bheeshamks.freeflow.core.data.model.DictationMode
import com.bheeshamks.freeflow.core.data.model.HapticStrength
import com.bheeshamks.freeflow.core.data.model.KeyboardSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "freeflow_preferences")

/**
 * DataStore preferences repository managing theme, Keys Cafe customizations, and accessibility settings.
 */
class ThemeAndGoodiesPreferences(private val context: Context) {

    companion object {
        private val KEY_ANIMATION_STYLE = stringPreferencesKey("animation_style")
        private val KEY_COLOR_PRESET = stringPreferencesKey("color_preset")
        private val KEY_BLOOM_RADIUS = floatPreferencesKey("bloom_radius")
        private val KEY_ANIMATION_SPEED = intPreferencesKey("animation_speed")
        private val KEY_HAPTIC_STRENGTH = stringPreferencesKey("haptic_strength")
        private val KEY_KEY_HEIGHT = floatPreferencesKey("key_height")
        private val KEY_CORNER_RADIUS = floatPreferencesKey("corner_radius")
        private val KEY_DICTATION_MODE = stringPreferencesKey("dictation_mode")
        private val KEY_EXCLUDED_PACKAGES = stringSetPreferencesKey("excluded_packages")
        private val KEY_KEYS_CAFE_ENABLED = booleanPreferencesKey("keys_cafe_enabled")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_FLOATING_BUBBLE_ENABLED = booleanPreferencesKey("floating_bubble_enabled")
        private val KEY_GROQ_MODEL = stringPreferencesKey("groq_model")
    }

    val settingsFlow: Flow<KeyboardSettings> = context.dataStore.data.map { prefs ->
        val styleName = prefs[KEY_ANIMATION_STYLE] ?: BloomAnimationStyle.RADIAL_BLOOM.name
        val colorName = prefs[KEY_COLOR_PRESET] ?: BloomColorPreset.ICY_BLUE.name
        val hapticName = prefs[KEY_HAPTIC_STRENGTH] ?: HapticStrength.CLICK.name
        val modeName = prefs[KEY_DICTATION_MODE] ?: DictationMode.TAP_TO_TOGGLE.name

        KeyboardSettings(
            animationStyle = try { BloomAnimationStyle.valueOf(styleName) } catch (e: Exception) { BloomAnimationStyle.RADIAL_BLOOM },
            colorPreset = try { BloomColorPreset.valueOf(colorName) } catch (e: Exception) { BloomColorPreset.ICY_BLUE },
            bloomRadiusDp = prefs[KEY_BLOOM_RADIUS] ?: 110f,
            animationSpeedMs = prefs[KEY_ANIMATION_SPEED] ?: 350,
            hapticStrength = try { HapticStrength.valueOf(hapticName) } catch (e: Exception) { HapticStrength.CLICK },
            keyHeightDp = prefs[KEY_KEY_HEIGHT] ?: 48f,
            cornerRadiusDp = prefs[KEY_CORNER_RADIUS] ?: 8f,
            dictationMode = try { DictationMode.valueOf(modeName) } catch (e: Exception) { DictationMode.TAP_TO_TOGGLE },
            excludedPackages = prefs[KEY_EXCLUDED_PACKAGES] ?: emptySet(),
            isKeysCafeEnabled = prefs[KEY_KEYS_CAFE_ENABLED] ?: true,
            isOnboardingCompleted = prefs[KEY_ONBOARDING_COMPLETED] ?: false,
            isFloatingBubbleEnabled = prefs[KEY_FLOATING_BUBBLE_ENABLED] ?: true,
            groqModel = prefs[KEY_GROQ_MODEL] ?: "whisper-large-v3"
        )
    }

    suspend fun updateAnimationStyle(style: BloomAnimationStyle) {
        context.dataStore.edit { it[KEY_ANIMATION_STYLE] = style.name }
    }

    suspend fun updateColorPreset(preset: BloomColorPreset) {
        context.dataStore.edit { it[KEY_COLOR_PRESET] = preset.name }
    }

    suspend fun updateBloomRadius(radiusDp: Float) {
        context.dataStore.edit { it[KEY_BLOOM_RADIUS] = radiusDp }
    }

    suspend fun updateAnimationSpeed(speedMs: Int) {
        context.dataStore.edit { it[KEY_ANIMATION_SPEED] = speedMs }
    }

    suspend fun updateHapticStrength(strength: HapticStrength) {
        context.dataStore.edit { it[KEY_HAPTIC_STRENGTH] = strength.name }
    }

    suspend fun updateKeyHeight(heightDp: Float) {
        context.dataStore.edit { it[KEY_KEY_HEIGHT] = heightDp }
    }

    suspend fun updateCornerRadius(radiusDp: Float) {
        context.dataStore.edit { it[KEY_CORNER_RADIUS] = radiusDp }
    }

    suspend fun updateDictationMode(mode: DictationMode) {
        context.dataStore.edit { it[KEY_DICTATION_MODE] = mode.name }
    }

    suspend fun toggleExcludedPackage(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_EXCLUDED_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            prefs[KEY_EXCLUDED_PACKAGES] = current
        }
    }

    suspend fun setKeysCafeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_KEYS_CAFE_ENABLED] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setFloatingBubbleEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_FLOATING_BUBBLE_ENABLED] = enabled }
    }

    suspend fun updateGroqModel(model: String) {
        context.dataStore.edit { it[KEY_GROQ_MODEL] = model }
    }
}
