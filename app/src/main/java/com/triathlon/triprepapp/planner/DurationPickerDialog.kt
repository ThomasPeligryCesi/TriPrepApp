package com.triathlon.triprepapp.planner

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.triathlon.triprepapp.R

/**
 * Hour/minute picker built on simple +/- steppers and quick-preset chips. Replaced the Android
 * NumberPicker because its wheel text color is overridden internally on every draw on pre-API 29,
 * making the digits disappear as soon as the user scrolled. The stepper approach is fully themable
 * and works consistently across all supported API levels.
 *
 * Output is normalized to "45min", "1h", or "1h30".
 */
object DurationPickerDialog {

    private const val MAX_HOURS = 5
    private const val MINUTE_STEP = 5

    fun show(
        context: Context,
        initialDuration: String?,
        onPicked: (String) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_duration_picker, null)
        val hourValue = view.findViewById<TextView>(R.id.hourValue)
        val minuteValue = view.findViewById<TextView>(R.id.minuteValue)
        val btnHourMinus = view.findViewById<MaterialButton>(R.id.btnHourMinus)
        val btnHourPlus = view.findViewById<MaterialButton>(R.id.btnHourPlus)
        val btnMinuteMinus = view.findViewById<MaterialButton>(R.id.btnMinuteMinus)
        val btnMinutePlus = view.findViewById<MaterialButton>(R.id.btnMinutePlus)
        val preview = view.findViewById<TextView>(R.id.durationPreview)
        val cancel = view.findViewById<MaterialButton>(R.id.btnDurationCancel)
        val ok = view.findViewById<MaterialButton>(R.id.btnDurationOk)

        val (initialHours, initialMinutes) = parseDuration(initialDuration)
        var hours = initialHours.coerceIn(0, MAX_HOURS)
        var minutes = snapToStep(initialMinutes).coerceIn(0, 55)

        fun render() {
            hourValue.text = hours.toString()
            minuteValue.text = minutes.toString().padStart(2, '0')
            preview.text = format(hours, minutes)
            btnHourMinus.isEnabled = hours > 0
            btnHourPlus.isEnabled = hours < MAX_HOURS
            btnMinuteMinus.isEnabled = minutes > 0
            btnMinutePlus.isEnabled = minutes < 55
        }
        render()

        btnHourMinus.setOnClickListener {
            if (hours > 0) { hours--; render() }
        }
        btnHourPlus.setOnClickListener {
            if (hours < MAX_HOURS) { hours++; render() }
        }
        btnMinuteMinus.setOnClickListener {
            if (minutes >= MINUTE_STEP) { minutes -= MINUTE_STEP; render() }
            else if (minutes > 0) { minutes = 0; render() }
        }
        btnMinutePlus.setOnClickListener {
            if (minutes + MINUTE_STEP <= 55) { minutes += MINUTE_STEP; render() }
        }

        // Quick-preset chips
        fun applyPreset(totalMinutes: Int) {
            hours = (totalMinutes / 60).coerceAtMost(MAX_HOURS)
            minutes = snapToStep(totalMinutes % 60)
            render()
        }
        view.findViewById<Chip>(R.id.presetQuick15).setOnClickListener { applyPreset(15) }
        view.findViewById<Chip>(R.id.presetQuick30).setOnClickListener { applyPreset(30) }
        view.findViewById<Chip>(R.id.presetQuick45).setOnClickListener { applyPreset(45) }
        view.findViewById<Chip>(R.id.presetQuick60).setOnClickListener { applyPreset(60) }
        view.findViewById<Chip>(R.id.presetQuick90).setOnClickListener { applyPreset(90) }
        view.findViewById<Chip>(R.id.presetQuick120).setOnClickListener { applyPreset(120) }

        val dialog = AlertDialog.Builder(context).setView(view).create()
        cancel.setOnClickListener { dialog.dismiss() }
        ok.setOnClickListener {
            if (hours == 0 && minutes == 0) {
                dialog.dismiss()
                return@setOnClickListener
            }
            onPicked(format(hours, minutes))
            dialog.dismiss()
        }
        dialog.show()
    }

    /** Normalized output: "45min", "1h", "1h30". */
    fun format(hours: Int, minutes: Int): String = when {
        hours == 0 && minutes == 0 -> "0min"
        hours == 0 -> "${minutes}min"
        minutes == 0 -> "${hours}h"
        else -> "${hours}h${minutes.toString().padStart(2, '0')}"
    }

    private fun snapToStep(minutes: Int): Int {
        val snapped = (minutes / MINUTE_STEP) * MINUTE_STEP
        return snapped.coerceIn(0, 55)
    }

    /**
     * Parses the legacy free-text duration back to (hours, minutes) so editing an existing
     * training pre-fills the picker sensibly. Supports "1h00", "45min", "30'", "1h30".
     */
    fun parseDuration(input: String?): Pair<Int, Int> {
        if (input.isNullOrBlank()) return 0 to 0
        val s = input.trim().lowercase()
        val hIndex = s.indexOf('h')
        if (hIndex >= 0) {
            val h = s.substring(0, hIndex).toIntOrNull() ?: 0
            val rest = s.substring(hIndex + 1).filter { it.isDigit() }
            val m = rest.toIntOrNull() ?: 0
            return h to m
        }
        if (s.contains("min")) {
            val m = s.replace("min", "").filter { it.isDigit() }.toIntOrNull() ?: 0
            return 0 to m
        }
        if (s.endsWith("'")) {
            val m = s.dropLast(1).filter { it.isDigit() }.toIntOrNull() ?: 0
            return 0 to m
        }
        val m = s.filter { it.isDigit() }.toIntOrNull() ?: 0
        return 0 to m
    }
}
