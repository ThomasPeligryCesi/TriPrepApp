package com.triathlon.triprepapp.planner

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.CalendarView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.triathlon.triprepapp.R
import com.triathlon.triprepapp.notifications.TrainingNotificationReceiver
import java.text.SimpleDateFormat
import java.util.*

class TrainingPlannerActivity : AppCompatActivity() {
    private lateinit var calendarView: CalendarView
    private lateinit var trainingsRecyclerView: RecyclerView
    private lateinit var trainingAdapter: TrainingAdapter
    private val trainings = mutableListOf<Training>()
    private var selectedDate: String = ""

    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_planner)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }

        calendarView = findViewById(R.id.calendarView)
        trainingsRecyclerView = findViewById(R.id.trainingsRecyclerView)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        selectedDate = dateFormat.format(Date(calendarView.date))

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = dateFormat.format(calendar.time)
            filterTrainingsByDate()
        }

        trainingAdapter = TrainingAdapter(mutableListOf())
        trainingsRecyclerView.adapter = trainingAdapter
        trainingsRecyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<FloatingActionButton>(R.id.fabAddTraining).setOnClickListener {
            showAddTrainingDialog()
        }
    }

    private fun filterTrainingsByDate() {
        val filtered = trainings.filter { it.date == selectedDate }.toMutableList()
        trainingAdapter = TrainingAdapter(filtered)
        trainingsRecyclerView.adapter = trainingAdapter
    }

    private fun showAddTrainingDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_training, null)
        val typeInput = dialogView.findViewById<TextInputEditText>(R.id.typeInput)
        val timeInput = dialogView.findViewById<TextInputEditText>(R.id.timeInput)
        val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.descriptionInput)

        AlertDialog.Builder(this)
            .setTitle("Nouvel Entraînement")
            .setView(dialogView)
            .setPositiveButton("Enregistrer") { dialog, _ ->
                val type = typeInput.text.toString()
                val time = timeInput.text.toString()
                val description = descriptionInput.text.toString()

                if (type.isNotEmpty() && time.isNotEmpty()) {
                    val training = Training(
                        date = selectedDate,
                        time = time,
                        type = type,
                        description = description
                    )
                    trainings.add(training)
                    filterTrainingsByDate()
                    scheduleNotification(training)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Annuler") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun scheduleNotification(training: Training) {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, TrainingNotificationReceiver::class.java).apply {
                putExtra("training_type", training.type)
                putExtra("training_description", training.description)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                training.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Parse date and time to schedule notification
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateTime = dateFormat.parse("${training.date} ${training.time}")

            if (dateTime != null && dateTime.time > System.currentTimeMillis()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    dateTime.time,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
