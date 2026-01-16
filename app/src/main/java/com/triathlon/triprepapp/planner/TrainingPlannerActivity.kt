package com.triathlon.triprepapp.planner

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.CalendarView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
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
    private lateinit var selectedDateText: TextView
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
        selectedDateText = findViewById(R.id.selectedDateText)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedDate = dateFormat.format(Date(calendarView.date))
        updateSelectedDateText()

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = dateFormat.format(calendar.time)
            updateSelectedDateText()
            filterTrainingsByDate()
        }

        trainingAdapter = TrainingAdapter(mutableListOf())
        trainingsRecyclerView.adapter = trainingAdapter
        trainingsRecyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<FloatingActionButton>(R.id.fabAddTraining).setOnClickListener {
            showAddTrainingDialog()
        }
    }

    private fun updateSelectedDateText() {
        val displayFormat = SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH)
        val parseFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val date = parseFormat.parse(selectedDate)
            selectedDateText.text = date?.let { displayFormat.format(it) } ?: selectedDate
        } catch (e: Exception) {
            selectedDateText.text = selectedDate
        }
    }

    private fun filterTrainingsByDate() {
        val filtered = trainings.filter { it.date == selectedDate }
        trainingAdapter.updateTrainings(filtered)
    }

    private fun showAddTrainingDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_training, null)
        val dateInput = dialogView.findViewById<TextInputEditText>(R.id.dateInput)
        val timeInput = dialogView.findViewById<TextInputEditText>(R.id.timeInput)
        val sportChipGroup = dialogView.findViewById<ChipGroup>(R.id.sportChipGroup)
        val partsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.partsRecyclerView)
        val partTypeInput = dialogView.findViewById<TextInputEditText>(R.id.partTypeInput)
        val partDurationInput = dialogView.findViewById<TextInputEditText>(R.id.partDurationInput)
        val btnAddPart = dialogView.findViewById<MaterialButton>(R.id.btnAddPart)

        // Initialize date and time with selected date
        dateInput.setText(selectedDate)
        timeInput.setText("08:00")

        // Setup date picker
        dateInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    calendar.set(year, month, day)
                    dateInput.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Setup time picker
        timeInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    timeInput.setText(String.format("%02d:%02d", hour, minute))
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        // Setup parts adapter
        val partsAdapter = TrainingPartAdapter(mutableListOf()) { position ->
            partsAdapter.removePart(position)
        }
        partsRecyclerView.adapter = partsAdapter
        partsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Add part button
        btnAddPart.setOnClickListener {
            val type = partTypeInput.text.toString()
            val duration = partDurationInput.text.toString()

            if (type.isNotEmpty() && duration.isNotEmpty()) {
                partsAdapter.addPart(TrainingPart(type, duration))
                partTypeInput.text?.clear()
                partDurationInput.text?.clear()
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val date = dateInput.text.toString()
            val time = timeInput.text.toString()
            val parts = partsAdapter.getParts()

            val sport = when (sportChipGroup.checkedChipId) {
                R.id.chipCycling -> Sport.CYCLING
                R.id.chipSwimming -> Sport.SWIMMING
                else -> Sport.RUNNING
            }

            if (date.isNotEmpty() && time.isNotEmpty() && parts.isNotEmpty()) {
                val training = Training(
                    date = date,
                    time = time,
                    sport = sport,
                    parts = parts
                )
                trainings.add(training)
                filterTrainingsByDate()
                scheduleNotification(training)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun scheduleNotification(training: Training) {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, TrainingNotificationReceiver::class.java).apply {
                putExtra("training_type", training.getDisplayTitle())
                putExtra("training_description", training.getDisplayParts())
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                training.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Parse date and time to schedule notification
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
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
