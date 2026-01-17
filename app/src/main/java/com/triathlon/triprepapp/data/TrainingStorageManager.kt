package com.triathlon.triprepapp.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.triathlon.triprepapp.planner.Training

object TrainingStorageManager {
    private const val PREFS_NAME = "triprep_trainings"
    private const val KEY_TRAININGS = "trainings_list"

    private val gson: Gson = GsonBuilder()
        .serializeNulls()
        .create()

    /**
     * Save trainings list to persistent storage
     */
    fun saveTrainings(context: Context, trainings: List<Training>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(trainings)
        prefs.edit().putString(KEY_TRAININGS, json).apply()
    }

    /**
     * Load trainings list from persistent storage
     */
    fun loadTrainings(context: Context): MutableList<Training> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_TRAININGS, null)

        return if (json != null) {
            try {
                val type = object : TypeToken<MutableList<Training>>() {}.type
                val trainings: MutableList<Training>? = gson.fromJson(json, type)
                trainings ?: mutableListOf()
            } catch (e: Exception) {
                e.printStackTrace()
                // Clear corrupted data
                clearTrainings(context)
                mutableListOf()
            }
        } else {
            mutableListOf()
        }
    }

    /**
     * Clear all trainings from storage
     */
    fun clearTrainings(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_TRAININGS).apply()
    }
}
