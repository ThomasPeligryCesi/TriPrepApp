package com.triathlon.triprepapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.triathlon.triprepapp.calculators.CyclingCalculatorActivity
import com.triathlon.triprepapp.calculators.RunningCalculatorActivity
import com.triathlon.triprepapp.calculators.SwimmingCalculatorActivity
import com.triathlon.triprepapp.planner.TrainingPlannerActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<MaterialButton>(R.id.btnRunningCalculator).setOnClickListener {
            startActivity(Intent(this, RunningCalculatorActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnSwimmingCalculator).setOnClickListener {
            startActivity(Intent(this, SwimmingCalculatorActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnCyclingCalculator).setOnClickListener {
            startActivity(Intent(this, CyclingCalculatorActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnTrainingPlanner).setOnClickListener {
            startActivity(Intent(this, TrainingPlannerActivity::class.java))
        }
    }
}
