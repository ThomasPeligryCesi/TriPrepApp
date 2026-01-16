package com.triathlon.triprepapp.data

import android.content.Context
import android.content.SharedPreferences

data class PerformanceMetrics(
    val vma: Double? = null,
    val vo2max: Double? = null,
    val ftp: Int? = null,
    val swim400m: String? = null
)

object PerformanceDataManager {
    private const val PREFS_NAME = "triprep_performance"
    private const val KEY_VMA = "vma"
    private const val KEY_VO2MAX = "vo2max"
    private const val KEY_FTP = "ftp"
    private const val KEY_SWIM_400M = "swim_400m"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveVMA(context: Context, vma: Double, vo2max: Double) {
        getPrefs(context).edit()
            .putFloat(KEY_VMA, vma.toFloat())
            .putFloat(KEY_VO2MAX, vo2max.toFloat())
            .apply()
    }

    fun saveFTP(context: Context, ftp: Int) {
        getPrefs(context).edit()
            .putInt(KEY_FTP, ftp)
            .apply()
    }

    fun saveSwim400m(context: Context, time: String) {
        getPrefs(context).edit()
            .putString(KEY_SWIM_400M, time)
            .apply()
    }

    fun getPerformanceMetrics(context: Context): PerformanceMetrics {
        val prefs = getPrefs(context)
        val vma = prefs.getFloat(KEY_VMA, 0f).toDouble()
        val vo2max = prefs.getFloat(KEY_VO2MAX, 0f).toDouble()
        val ftp = prefs.getInt(KEY_FTP, 0)
        val swim400m = prefs.getString(KEY_SWIM_400M, null)

        return PerformanceMetrics(
            vma = if (vma > 0) vma else null,
            vo2max = if (vo2max > 0) vo2max else null,
            ftp = if (ftp > 0) ftp else null,
            swim400m = swim400m
        )
    }
}
