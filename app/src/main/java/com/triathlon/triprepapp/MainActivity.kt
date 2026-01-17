package com.triathlon.triprepapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.triathlon.triprepapp.calculators.CyclingCalculatorActivity
import com.triathlon.triprepapp.calculators.RunningCalculatorActivity
import com.triathlon.triprepapp.calculators.SwimmingCalculatorActivity
import com.triathlon.triprepapp.data.PerformanceDataManager
import com.triathlon.triprepapp.data.TrainingStorageManager
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

        // Hide action bar
        supportActionBar?.hide()

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

        // Click listener for 400m swim card
        findViewById<LinearLayout>(R.id.swim400mCard).setOnClickListener {
            showSwimPacesDialog()
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
        val trainings = TrainingStorageManager.loadTrainings(this)
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val displayFormat = SimpleDateFormat("dd MMMM 'à' HH:mm", Locale.FRENCH)
        val now = System.currentTimeMillis()

        // Find next training (future trainings sorted by date/time)
        val nextTraining = trainings
            .mapNotNull { training ->
                try {
                    val dateTime = dateTimeFormat.parse("${training.date} ${training.time}")
                    if (dateTime != null && dateTime.time > now) {
                        Pair(training, dateTime)
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
            .minByOrNull { it.second.time }

        if (nextTraining != null) {
            nextTrainingTitle.text = nextTraining.first.getDisplayTitle()
            nextTrainingTime.text = displayFormat.format(nextTraining.second)
            nextTrainingTime.visibility = View.VISIBLE
        } else {
            nextTrainingTitle.text = "Aucun entraînement planifié"
            nextTrainingTime.visibility = View.GONE
        }
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

    private fun showSwimPacesDialog() {
        val metrics = PerformanceDataManager.getPerformanceMetrics(this)
        val swim400mTime = metrics.swim400m

        if (swim400mTime == null || swim400mTime == "--") {
            // Show message if no 400m time is available
            AlertDialog.Builder(this)
                .setTitle("Pas de données")
                .setMessage("Veuillez d'abord enregistrer votre temps de 400m nage via le calculateur de natation.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
            return
        }

        // Parse time to seconds
        val timeInSeconds = parseTimeToSeconds(swim400mTime) ?: return

        // Calculate paces
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

        // Show dialog with paces
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_swim_paces, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val timeText = dialogView.findViewById<TextView>(R.id.swim400mTimeText)
        val v2PacesText = dialogView.findViewById<TextView>(R.id.v2PacesText)
        val v3PacesText = dialogView.findViewById<TextView>(R.id.v3PacesText)
        val v4PacesText = dialogView.findViewById<TextView>(R.id.v4PacesText)
        val closeButton = dialogView.findViewById<MaterialButton>(R.id.closeButton)

        timeText.text = "Basé sur votre 400m : $swim400mTime"
        v2PacesText.text = "50m: ${formatTime(v2_50m)} | 100m: ${formatTime(v2_100m)} | 200m: ${formatTime(v2_200m)}"
        v3PacesText.text = "50m: ${formatTime(v3_50m)} | 100m: ${formatTime(v3_100m)} | 200m: ${formatTime(v3_200m)}"
        v4PacesText.text = "50m: ${formatTime(v4_50m)} | 100m: ${formatTime(v4_100m)} | 200m: ${formatTime(v4_200m)}"

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
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

    private fun formatTime(seconds: Double): String {
        val mins = (seconds / 60).toInt()
        val secs = (seconds % 60).toInt()
        return if (mins > 0) {
            String.format("%d:%02d", mins, secs)
        } else {
            String.format("%.1fs", seconds)
        }
    }
}
