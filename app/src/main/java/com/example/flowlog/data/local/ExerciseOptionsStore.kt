package com.example.flowlog.data.local

import android.content.Context

data class ExerciseLastSettings(
    val mode: String,
    val durationMillis: Long,
    val reps: Int,
    val intensity: String
)

object ExerciseOptionsStore {
    private const val PREFS_NAME = "exercise_options"
    private const val KEY_CUSTOM_EXERCISES = "custom_exercises"
    private const val SEPARATOR = "||"

    fun loadCustomExercises(context: Context): List<String> {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_EXERCISES, null)
            ?: return emptyList()
        return raw.split(SEPARATOR).filter { it.isNotBlank() }
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
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_EXERCISES, exercises.joinToString(SEPARATOR))
            .apply()
    }
}
