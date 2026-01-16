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

class CyclingCalculatorActivity : AppCompatActivity() {
    private lateinit var ftpInput: TextInputEditText
    private lateinit var calculateButton: MaterialButton
    private lateinit var resultsCard: MaterialCardView
    private lateinit var zonesResultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cycling_calculator)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ftpInput = findViewById(R.id.ftpInput)
        calculateButton = findViewById(R.id.calculateButton)
        resultsCard = findViewById(R.id.resultsCard)
        zonesResultText = findViewById(R.id.zonesResultText)

        calculateButton.setOnClickListener {
            calculateResults()
        }
    }

    private fun calculateResults() {
        val ftpText = ftpInput.text.toString()
        if (ftpText.isEmpty()) {
            ftpInput.error = "Veuillez entrer votre FTP"
            return
        }

        val ftp = ftpText.toIntOrNull()
        if (ftp == null || ftp <= 0) {
            ftpInput.error = "FTP invalide"
            return
        }

        // Calculate power zones based on FTP
        val zone1Max = (ftp * 0.55).toInt()
        val zone2Max = (ftp * 0.75).toInt()
        val zone3Max = (ftp * 0.90).toInt()
        val zone4Max = (ftp * 1.05).toInt()
        val zone5Max = (ftp * 1.20).toInt()

        val zonesText = """
            Zone 1 (Récupération active): 0-${zone1Max}W
            Zone 2 (Endurance): ${zone1Max}-${zone2Max}W
            Zone 3 (Tempo): ${zone2Max}-${zone3Max}W
            Zone 4 (Seuil): ${zone3Max}-${zone4Max}W
            Zone 5 (VO2max): ${zone4Max}-${zone5Max}W
            Zone 6 (Anaérobie): >${zone5Max}W
        """.trimIndent()

        zonesResultText.text = zonesText

        // Save to SharedPreferences
        PerformanceDataManager.saveFTP(this, ftp)

        resultsCard.visibility = View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
