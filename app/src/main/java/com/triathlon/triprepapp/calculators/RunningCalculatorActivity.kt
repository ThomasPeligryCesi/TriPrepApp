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

class RunningCalculatorActivity : AppCompatActivity() {
    private lateinit var distanceInput: TextInputEditText
    private lateinit var calculateButton: MaterialButton
    private lateinit var resultsCard: MaterialCardView
    private lateinit var vmaResultText: TextView
    private lateinit var vo2maxResultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_running_calculator)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        distanceInput = findViewById(R.id.distanceInput)
        calculateButton = findViewById(R.id.calculateButton)
        resultsCard = findViewById(R.id.resultsCard)
        vmaResultText = findViewById(R.id.vmaResultText)
        vo2maxResultText = findViewById(R.id.vo2maxResultText)

        calculateButton.setOnClickListener {
            calculateResults()
        }
    }

    private fun calculateResults() {
        val distanceText = distanceInput.text.toString()
        if (distanceText.isEmpty()) {
            distanceInput.error = "Veuillez entrer une distance"
            return
        }

        val distance = distanceText.toDoubleOrNull()
        if (distance == null || distance <= 0) {
            distanceInput.error = "Distance invalide"
            return
        }

        // VMA = (distance en mètres / 100) km/h
        val vma = distance / 100.0

        // VO2 Max = 3.5 * VMA
        val vo2max = vma * 3.5

        vmaResultText.text = getString(R.string.vma_result, vma)
        vo2maxResultText.text = getString(R.string.vo2max_result, vo2max)

        // Save to SharedPreferences
        PerformanceDataManager.saveVMA(this, vma, vo2max)

        resultsCard.visibility = View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
