package com.example.flowlog.data.local

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class ExerciseLastSettings(
    val mode: String,
    val durationMillis: Long,
    val reps: Int,
    val intensity: String
)

object ExerciseOptionsStore {
    private const val PREFS_NAME = "exercise_options"
    private const val KEY_CUSTOM_EXERCISES = "custom_exercises"
    private const val KEY_EXERCISE_ORDER = "exercise_order"
    private const val KEY_HIDDEN_DEFAULT_EXERCISES = "hidden_default_exercises"
    private const val SEPARATOR = "||"
    private val json = Json { ignoreUnknownKeys = true }

    fun loadCustomExercises(context: Context): List<String> {
        return loadStringList(context, KEY_CUSTOM_EXERCISES)
    }

    fun addCustomExercise(context: Context, name: String) {
        val current = loadCustomExercises(context).toMutableList()
        if (name !in current) {
            current.add(name)
            save(context, current)
        }
    }

    fun removeCustomExercise(context: Context, name: String) {
        val updated = loadCustomExercises(context).filter { it != name }
        save(context, updated)
    }

    fun loadHiddenDefaultExercises(context: Context): List<String> {
        return loadStringList(context, KEY_HIDDEN_DEFAULT_EXERCISES)
    }

    fun hideDefaultExercise(context: Context, name: String) {
        val current = loadHiddenDefaultExercises(context)
        if (name !in current) {
            saveHiddenDefaults(context, current + name)
        }
    }

    fun showDefaultExercise(context: Context, name: String) {
        saveHiddenDefaults(context, loadHiddenDefaultExercises(context).filter { it != name })
    }

    fun loadExerciseOrder(context: Context): List<String> {
        return loadStringList(context, KEY_EXERCISE_ORDER)
    }

    fun saveExerciseOrder(context: Context, exercises: List<String>) {
        saveStringList(context, KEY_EXERCISE_ORDER, exercises.distinct())
    }

    fun saveLastSettings(context: Context, exerciseName: String, settings: ExerciseLastSettings) {
        val key = settingsKey(exerciseName)
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("${key}_mode", settings.mode)
            .putLong("${key}_duration", settings.durationMillis)
            .putInt("${key}_reps", settings.reps)
            .putString("${key}_intensity", settings.intensity)
            .apply()
    }

    fun loadLastSettings(context: Context, exerciseName: String): ExerciseLastSettings? {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = settingsKey(exerciseName)
        val mode = prefs.getString("${key}_mode", null) ?: return null
        return ExerciseLastSettings(
            mode = mode,
            durationMillis = prefs.getLong("${key}_duration", 40_000L),
            reps = prefs.getInt("${key}_reps", 12),
            intensity = prefs.getString("${key}_intensity", "힘듦") ?: "힘듦"
        )
    }

    private fun settingsKey(exerciseName: String): String =
        "ex_${exerciseName.replace(" ", "_")}"

    private fun save(context: Context, exercises: List<String>) {
        saveStringList(context, KEY_CUSTOM_EXERCISES, exercises)
    }

    private fun saveHiddenDefaults(context: Context, exercises: List<String>) {
        saveStringList(context, KEY_HIDDEN_DEFAULT_EXERCISES, exercises.distinct())
    }

    private fun loadStringList(context: Context, key: String): List<String> {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key, null)
            ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<String>>(raw)
        }.getOrElse {
            raw.split(SEPARATOR).filter { item -> item.isNotBlank() }
        }
    }

    private fun saveStringList(context: Context, key: String, exercises: List<String>) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(key, json.encodeToString(exercises))
            .apply()
    }
}
