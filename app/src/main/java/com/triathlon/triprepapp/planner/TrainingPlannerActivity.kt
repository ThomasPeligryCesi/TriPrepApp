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
import com.triathlon.triprepapp.views.TrainingCalendarView
import java.text.SimpleDateFormat
import java.util.*

class TrainingPlannerActivity : AppCompatActivity() {
    private lateinit var trainingCalendarView: TrainingCalendarView
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

            trainingCalendarView = findViewById(R.id.trainingCalendarView)
            trainingsRecyclerView = findViewById(R.id.trainingsRecyclerView)
            selectedDateText = findViewById(R.id.selectedDateText)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            selectedDate = dateFormat.format(Date())
            trainingCalendarView.setSelectedDate(selectedDate)

            updateSelectedDateText()

            trainingCalendarView.setOnDateSelectedListener { newDate ->
                selectedDate = newDate
                updateSelectedDateText()
                filterTrainingsByDate()
            }

            trainingCalendarView.setOnMonthChangedListener { _, _ ->
                updateCalendarIndicators()
            }

            trainings.clear()
            try {
                trainings.addAll(TrainingStorageManager.loadTrainings(this))
            } catch (e: Exception) {
                e.printStackTrace()
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

            filterTrainingsByDate()
            updateCalendarIndicators()

            findViewById<FloatingActionButton>(R.id.fabAddTraining).setOnClickListener {
                showAddTrainingDialog()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }

    private fun updateSelectedDateText() {
        val displayFormat = SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRENCH)
        val parseFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val date = parseFormat.parse(selectedDate)
            val formatted = date?.let { displayFormat.format(it) } ?: selectedDate
            selectedDateText.text = formatted.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString()
            }
        } catch (e: Exception) {
            selectedDateText.text = selectedDate
        }
    }

    private fun filterTrainingsByDate() {
        val filtered = trainings.filter { it.date == selectedDate }
        trainingAdapter.updateTrainings(filtered)
    }

    private fun updateCalendarIndicators() {
        trainingCalendarView.setTrainings(trainings)
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

        if (existingTraining != null) {
            dialogTitle.text = "✏️ Modifier l'entraînement"
        }

        val displayDateFormat = SimpleDateFormat("EEE dd MMM yyyy", Locale.FRENCH)
        val storageDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        var editingDate: String = existingTraining?.date ?: selectedDate
        var editingTime: String = existingTraining?.time ?: "08:00"

        fun refreshDateLabel() {
            try {
                val parsed = storageDateFormat.parse(editingDate)
                dateInput.setText(parsed?.let { displayDateFormat.format(it) } ?: editingDate)
            } catch (_: Exception) {
                dateInput.setText(editingDate)
            }
        }
        fun refreshTimeLabel() {
            timeInput.setText(editingTime)
        }
        refreshDateLabel()
        refreshTimeLabel()

        dateInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            try {
                storageDateFormat.parse(editingDate)?.let { calendar.time = it }
            } catch (_: Exception) {
            }
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val cal = Calendar.getInstance()
                    cal.set(year, month, day)
                    editingDate = storageDateFormat.format(cal.time)
                    refreshDateLabel()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        timeInput.setOnClickListener {
            val (h, m) = parseHourMinute(editingTime)
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    editingTime = String.format("%02d:%02d", hour, minute)
                    refreshTimeLabel()
                },
                h,
                m,
                true
            ).show()
        }

        val initialParts = existingTraining?.parts?.toMutableList() ?: mutableListOf()
        lateinit var partsAdapter: TrainingPartAdapter
        partsAdapter = TrainingPartAdapter(initialParts) { position ->
            partsAdapter.removePart(position)
        }
        partsRecyclerView.adapter = partsAdapter
        partsRecyclerView.layoutManager = LinearLayoutManager(this)

        fun updatePartTypes(sport: Sport) {
            val types = sport.getPartTypes()
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, types)
            partTypeInput.setAdapter(adapter)
        }

        val initialSport = existingTraining?.sport ?: Sport.RUNNING
        updatePartTypes(initialSport)

        when (initialSport) {
            Sport.CYCLING -> sportChipGroup.check(R.id.chipCycling)
            Sport.SWIMMING -> sportChipGroup.check(R.id.chipSwimming)
            Sport.RUNNING -> sportChipGroup.check(R.id.chipRunning)
        }

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

        // Open the type dropdown on tap to make the field feel button-like
        partTypeInput.setOnClickListener { partTypeInput.showDropDown() }

        // Duration: custom picker dialog instead of free-text entry
        partDurationInput.setOnClickListener {
            DurationPickerDialog.show(this, partDurationInput.text?.toString()) { result ->
                partDurationInput.setText(result)
            }
        }

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
            val parts = partsAdapter.getParts()

            val sport = when (sportChipGroup.checkedChipId) {
                R.id.chipCycling -> Sport.CYCLING
                R.id.chipSwimming -> Sport.SWIMMING
                else -> Sport.RUNNING
            }

            if (editingDate.isNotEmpty() && editingTime.isNotEmpty() && parts.isNotEmpty()) {
                if (existingTraining != null) {
                    trainings.removeIf { it.id == existingTraining.id }
                    val updatedTraining = Training(
                        id = existingTraining.id,
                        date = editingDate,
                        time = editingTime,
                        sport = sport,
                        parts = parts,
                        notes = existingTraining.notes,
                        reviewed = existingTraining.reviewed
                    )
                    trainings.add(updatedTraining)
                    scheduleNotification(updatedTraining)
                } else {
                    val training = Training(
                        date = editingDate,
                        time = editingTime,
                        sport = sport,
                        parts = parts
                    )
                    trainings.add(training)
                    scheduleNotification(training)
                }

                TrainingStorageManager.saveTrainings(this, trainings)

                filterTrainingsByDate()
                updateCalendarIndicators()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun parseHourMinute(value: String): Pair<Int, Int> {
        return try {
            val parts = value.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            h to m
        } catch (_: Exception) {
            8 to 0
        }
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

        val partAdapter = TrainingPartReviewAdapter(training.parts)
        partsRecyclerView.adapter = partAdapter
        partsRecyclerView.layoutManager = LinearLayoutManager(this)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        saveButton.setOnClickListener {
            training.notes = notesInput.text.toString()
            training.reviewed = true

            val index = trainings.indexOfFirst { it.id == training.id }
            if (index != -1) {
                trainings[index] = training
                TrainingStorageManager.saveTrainings(this, trainings)
                filterTrainingsByDate()
                updateCalendarIndicators()
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
                putExtra("training_time", training.time)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                training.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Fire the reminder at 08:00 on the day before the training.
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val trainingDay = dateFormat.parse(training.date) ?: return
            val reminder = Calendar.getInstance().apply {
                time = trainingDay
                add(Calendar.DAY_OF_MONTH, -1)
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (reminder.timeInMillis > System.currentTimeMillis()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.timeInMillis,
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
