package com.triathlon.triprepapp.calculators

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.triathlon.triprepapp.R
import com.triathlon.triprepapp.data.PerformanceDataManager

class SwimmingCalculatorActivity : AppCompatActivity() {
    private lateinit var timeInput: TextInputEditText
    private lateinit var calculateButton: MaterialButton
    private lateinit var resultsCard: MaterialCardView
    private lateinit var v2ResultText: TextView
    private lateinit var v3ResultText: TextView
    private lateinit var v4ResultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_swimming_calculator)

        supportActionBar?.hide()

        timeInput = findViewById(R.id.timeInput)
        calculateButton = findViewById(R.id.calculateButton)
        resultsCard = findViewById(R.id.resultsCard)
        v2ResultText = findViewById(R.id.v2ResultText)
        v3ResultText = findViewById(R.id.v3ResultText)
        v4ResultText = findViewById(R.id.v4ResultText)

        calculateButton.setOnClickListener {
            calculateResults()
        }
    }

    private fun calculateResults() {
        val timeText = timeInput.text.toString()
        if (timeText.isEmpty()) {
            timeInput.error = "Veuillez entrer un temps"
            return
        }

        val timeInSeconds = parseTimeToSeconds(timeText)
        if (timeInSeconds == null) {
            timeInput.error = "Format invalide (MM:SS)"
            return
        }

        // Base time per 100m
        val base100m = timeInSeconds / 4.0

        // V2: 115% of test time (slower)
        val v2_100m = base100m * 1.15
        val v2_50m = v2_100m / 2.0
        val v2_200m = v2_100m * 2.0

        // V3: 105% of test time
        val v3_100m = base100m * 1.05
        val v3_50m = v3_100m / 2.0
        val v3_200m = v3_100m * 2.0

        // V4: 97% of test time (faster)
        val v4_100m = base100m * 0.97
        val v4_50m = v4_100m / 2.0
        val v4_200m = v4_100m * 2.0

        v2ResultText.text = formatZoneResult("V2", v2_50m, v2_100m, v2_200m)
        v3ResultText.text = formatZoneResult("V3", v3_50m, v3_100m, v3_200m)
        v4ResultText.text = formatZoneResult("V4", v4_50m, v4_100m, v4_200m)

        // Save to SharedPreferences
        PerformanceDataManager.saveSwim400m(this, timeText)

        resultsCard.visibility = View.VISIBLE
    }

    private fun parseTimeToSeconds(time: String): Double? {
        return try {
            val parts = time.split(":")
            if (parts.size != 2) return null
            val minutes = parts[0].toInt()
            val seconds = parts[1].toInt()
            (minutes * 60 + seconds).toDouble()
        } catch (e: Exception) {
            null
        }
    }

    private fun formatZoneResult(zone: String, time50m: Double, time100m: Double, time200m: Double): String {
        return "$zone - 50m: ${formatTime(time50m)} | 100m: ${formatTime(time100m)} | 200m: ${formatTime(time200m)}"
    }

    private fun formatTime(seconds: Double): String {
        val mins = (seconds / 60).toInt()
        val secs = (seconds % 60).toInt()
        return if (mins > 0) {
            String.format("%d:%02d", mins, secs)
        } else {
            String.format("%.1fs", seconds)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
