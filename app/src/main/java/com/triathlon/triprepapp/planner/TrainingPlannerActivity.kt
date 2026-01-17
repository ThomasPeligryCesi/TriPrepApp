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
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import com.triathlon.triprepapp.data.TrainingStorageManager
import com.triathlon.triprepapp.notifications.TrainingNotificationReceiver
import android.widget.CalendarView
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

        try {
            setContentView(R.layout.activity_training_planner)

            supportActionBar?.hide()

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

            // Load trainings from persistent storage
            trainings.clear()
            try {
                trainings.addAll(TrainingStorageManager.loadTrainings(this))
            } catch (e: Exception) {
                e.printStackTrace()
                // If loading fails due to incompatible data format, clear and start fresh
                TrainingStorageManager.clearTrainings(this)
            }

            trainingAdapter = TrainingAdapter(mutableListOf()) { training ->
                try {
                    if (training.isPast()) {
                        showReviewTrainingDialog(training)
                    } else {
                        showEditTrainingDialog(training)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            trainingsRecyclerView.adapter = trainingAdapter
            trainingsRecyclerView.layoutManager = LinearLayoutManager(this)

            // Show trainings for selected date
            filterTrainingsByDate()

            findViewById<FloatingActionButton>(R.id.fabAddTraining).setOnClickListener {
                showAddTrainingDialog()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
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

    private fun showEditTrainingDialog(existingTraining: Training) {
        showTrainingDialog(existingTraining)
    }

    private fun showAddTrainingDialog() {
        showTrainingDialog(null)
    }

    private fun showTrainingDialog(existingTraining: Training? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_training, null)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val dateInput = dialogView.findViewById<TextInputEditText>(R.id.dateInput)
        val timeInput = dialogView.findViewById<TextInputEditText>(R.id.timeInput)
        val sportChipGroup = dialogView.findViewById<ChipGroup>(R.id.sportChipGroup)
        val partsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.partsRecyclerView)
        val partTypeInput = dialogView.findViewById<AutoCompleteTextView>(R.id.partTypeInput)
        val partDurationInput = dialogView.findViewById<TextInputEditText>(R.id.partDurationInput)
        val btnAddPart = dialogView.findViewById<MaterialButton>(R.id.btnAddPart)

        // Set dialog title based on mode
        if (existingTraining != null) {
            dialogTitle.text = "✏️ Modifier l'entraînement"
        }

        // Initialize date and time
        if (existingTraining != null) {
            dateInput.setText(existingTraining.date)
            timeInput.setText(existingTraining.time)
        } else {
            dateInput.setText(selectedDate)
            timeInput.setText("08:00")
        }

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

        // Setup parts adapter with existing parts if editing
        val initialParts = existingTraining?.parts?.toMutableList() ?: mutableListOf()
        lateinit var partsAdapter: TrainingPartAdapter
        partsAdapter = TrainingPartAdapter(initialParts) { position ->
            partsAdapter.removePart(position)
        }
        partsRecyclerView.adapter = partsAdapter
        partsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Setup type dropdown based on selected sport
        fun updatePartTypes(sport: Sport) {
            val types = sport.getPartTypes()
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, types)
            partTypeInput.setAdapter(adapter)
        }

        // Initialize sport selection and part types
        val initialSport = existingTraining?.sport ?: Sport.RUNNING
        updatePartTypes(initialSport)

        // Select the correct sport chip
        when (initialSport) {
            Sport.CYCLING -> sportChipGroup.check(R.id.chipCycling)
            Sport.SWIMMING -> sportChipGroup.check(R.id.chipSwimming)
            Sport.RUNNING -> sportChipGroup.check(R.id.chipRunning)
        }

        // Update part types when sport changes
        sportChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val sport = when (checkedIds.first()) {
                    R.id.chipCycling -> Sport.CYCLING
                    R.id.chipSwimming -> Sport.SWIMMING
                    else -> Sport.RUNNING
                }
                updatePartTypes(sport)
                partTypeInput.text?.clear()
            }
        }

        // Add part button
        btnAddPart.setOnClickListener {
            val type = partTypeInput.text.toString()
            val duration = partDurationInput.text.toString()

            if (type.isNotEmpty() && duration.isNotEmpty()) {
                partsAdapter.addPart(TrainingPart(type, duration))
                partTypeInput.setText("")
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
                if (existingTraining != null) {
                    // Edit mode: remove old training and add updated one
                    trainings.removeIf { it.id == existingTraining.id }
                    val updatedTraining = Training(
                        id = existingTraining.id, // Keep same ID
                        date = date,
                        time = time,
                        sport = sport,
                        parts = parts,
                        notes = existingTraining.notes, // Preserve notes
                        reviewed = existingTraining.reviewed // Preserve review status
                    )
                    trainings.add(updatedTraining)
                    scheduleNotification(updatedTraining)
                } else {
                    // Add mode: create new training
                    val training = Training(
                        date = date,
                        time = time,
                        sport = sport,
                        parts = parts
                    )
                    trainings.add(training)
                    scheduleNotification(training)
                }

                // Save trainings to persistent storage
                TrainingStorageManager.saveTrainings(this, trainings)

                filterTrainingsByDate()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showReviewTrainingDialog(training: Training) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_review_training, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val titleText = dialogView.findViewById<TextView>(R.id.reviewDialogTitle)
        val partsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.reviewPartsRecyclerView)
        val notesInput = dialogView.findViewById<TextInputEditText>(R.id.reviewNotesInput)
        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.reviewCancelButton)
        val saveButton = dialogView.findViewById<MaterialButton>(R.id.reviewSaveButton)

        titleText.text = "Valider ${training.getDisplayTitle()}"
        notesInput.setText(training.notes)

        // Set up parts recycler view
        val partAdapter = TrainingPartReviewAdapter(training.parts)
        partsRecyclerView.adapter = partAdapter
        partsRecyclerView.layoutManager = LinearLayoutManager(this)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        saveButton.setOnClickListener {
            training.notes = notesInput.text.toString()
            training.reviewed = true

            // Update training in the list
            val index = trainings.indexOfFirst { it.id == training.id }
            if (index != -1) {
                trainings[index] = training
                TrainingStorageManager.saveTrainings(this, trainings)
                filterTrainingsByDate()
            }

            dialog.dismiss()
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
