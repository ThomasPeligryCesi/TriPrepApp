package com.triathlon.triprepapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.triathlon.triprepapp.calculators.CyclingCalculatorActivity
import com.triathlon.triprepapp.calculators.RunningCalculatorActivity
import com.triathlon.triprepapp.calculators.SwimmingCalculatorActivity
import com.triathlon.triprepapp.data.PerformanceDataManager
import com.triathlon.triprepapp.planner.TrainingPlannerActivity
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var vmaValue: TextView
    private lateinit var vo2maxValue: TextView
    private lateinit var ftpValue: TextView
    private lateinit var swim400mValue: TextView
    private lateinit var nextTrainingTitle: TextView
    private lateinit var nextTrainingTime: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vmaValue = findViewById(R.id.vmaValue)
        vo2maxValue = findViewById(R.id.vo2maxValue)
        ftpValue = findViewById(R.id.ftpValue)
        swim400mValue = findViewById(R.id.swim400mValue)
        nextTrainingTitle = findViewById(R.id.nextTrainingTitle)
        nextTrainingTime = findViewById(R.id.nextTrainingTime)

        // Load performance data
        loadPerformanceData()

        // Load next training
        loadNextTraining()

        // Calculator dropdown menu
        findViewById<MaterialButton>(R.id.btnCalculators).setOnClickListener { view ->
            showCalculatorMenu(view)
        }

        findViewById<MaterialButton>(R.id.btnTrainingPlanner).setOnClickListener {
            startActivity(Intent(this, TrainingPlannerActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadPerformanceData()
        loadNextTraining()
    }

    private fun loadPerformanceData() {
        val metrics = PerformanceDataManager.getPerformanceMetrics(this)

        vmaValue.text = metrics.vma?.let { String.format("%.1f", it) } ?: "--"
        vo2maxValue.text = metrics.vo2max?.let { String.format("%.1f", it) } ?: "--"
        ftpValue.text = metrics.ftp?.toString() ?: "--"
        swim400mValue.text = metrics.swim400m ?: "--"
    }

    private fun loadNextTraining() {
        // TODO: Load from planner database
        // For now, show placeholder
        nextTrainingTitle.text = "Aucun entraînement planifié"
        nextTrainingTime.visibility = View.GONE
    }

    private fun showCalculatorMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_calculators, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_running -> {
                    startActivity(Intent(this, RunningCalculatorActivity::class.java))
                    true
                }
                R.id.menu_swimming -> {
                    startActivity(Intent(this, SwimmingCalculatorActivity::class.java))
                    true
                }
                R.id.menu_cycling -> {
                    startActivity(Intent(this, CyclingCalculatorActivity::class.java))
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}
