package com.triathlon.triprepapp.planner

import android.app.AlertDialog
import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.triathlon.triprepapp.R

/**
 * Simple hour/minute picker that produces a normalized duration string like "1h30" or "45min".
 * Replaces the old free-text duration input ("Ex: 1h00, 45min, 30'") which had no validation and
 * inconsistent formatting.
 */
object DurationPickerDialog {

    fun show(
        context: Context,
        initialDuration: String?,
        onPicked: (String) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_duration_picker, null)
        val hourPicker = view.findViewById<NumberPicker>(R.id.hourPicker)
        val minutePicker = view.findViewById<NumberPicker>(R.id.minutePicker)
        val preview = view.findViewById<TextView>(R.id.durationPreview)
        val cancel = view.findViewById<MaterialButton>(R.id.btnDurationCancel)
        val ok = view.findViewById<MaterialButton>(R.id.btnDurationOk)

        hourPicker.minValue = 0
        hourPicker.maxValue = 5
        hourPicker.wrapSelectorWheel = false

        // Minutes in 5-minute increments to keep the wheel compact and avoid "47 min" inputs.
        val minuteValues = (0..55 step 5).map { it.toString().padStart(2, '0') }.toTypedArray()
        minutePicker.minValue = 0
        minutePicker.maxValue = minuteValues.size - 1
        minutePicker.displayedValues = minuteValues
        minutePicker.wrapSelectorWheel = false

        // On stock themes the NumberPicker's selector text and internal EditText can render in the
        // same color as the background (invisible). Force a readable color for both the wheel text
        // and the centered editable field.
        val pickerColor = ContextCompat.getColor(context, R.color.text_primary)
        setNumberPickerTextColor(hourPicker, pickerColor)
        setNumberPickerTextColor(minutePicker, pickerColor)

        val (initialHours, initialMinutes) = parseDuration(initialDuration)
        hourPicker.value = initialHours.coerceIn(0, 5)
        val minuteIndex = minuteValues.indexOfFirst { it.toInt() >= initialMinutes }
            .takeIf { it >= 0 } ?: 0
        minutePicker.value = minuteIndex

        fun updatePreview() {
            preview.text = format(hourPicker.value, minuteValues[minutePicker.value].toInt())
        }
        updatePreview()

        hourPicker.setOnValueChangedListener { _, _, _ -> updatePreview() }
        minutePicker.setOnValueChangedListener { _, _, _ -> updatePreview() }

        val dialog = AlertDialog.Builder(context).setView(view).create()
        cancel.setOnClickListener { dialog.dismiss() }
        ok.setOnClickListener {
            val hours = hourPicker.value
            val minutes = minuteValues[minutePicker.value].toInt()
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
        hours == 0 -> "${minutes}min"
        minutes == 0 -> "${hours}h"
        else -> "${hours}h${minutes.toString().padStart(2, '0')}"
    }

    /**
     * Forces the NumberPicker's selector wheel paint and inner EditText color so the digits are
     * visible against a light dialog background. Uses reflection because
     * NumberPicker#setTextColor() only exists from API 29 and minSdk is 24.
     */
    private fun setNumberPickerTextColor(picker: NumberPicker, color: Int) {
        try {
            val paintField = NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint")
            paintField.isAccessible = true
            (paintField.get(picker) as? Paint)?.color = color
        } catch (_: Exception) {
        }
        for (i in 0 until picker.childCount) {
            val child = picker.getChildAt(i)
            if (child is EditText) {
                child.setTextColor(color)
            }
        }
        picker.invalidate()
    }

    /**
     * Parses the legacy free-text duration back to (hours, minutes) so editing an existing
     * training pre-fills the picker sensibly. Supports "1h00", "45min", "30'", "1h30".
     */
    fun parseDuration(input: String?): Pair<Int, Int> {
        if (input.isNullOrBlank()) return 0 to 0
        val s = input.trim().lowercase()
        // "1h30" or "1h"
        val hIndex = s.indexOf('h')
        if (hIndex >= 0) {
            val h = s.substring(0, hIndex).toIntOrNull() ?: 0
            val rest = s.substring(hIndex + 1).filter { it.isDigit() }
            val m = rest.toIntOrNull() ?: 0
            return h to m
        }
        // "45min" / "30min"
        if (s.contains("min")) {
            val m = s.replace("min", "").filter { it.isDigit() }.toIntOrNull() ?: 0
            return 0 to m
        }
        // "30'" (minutes apostrophe)
        if (s.endsWith("'")) {
            val m = s.dropLast(1).filter { it.isDigit() }.toIntOrNull() ?: 0
            return 0 to m
        }
        // Plain number → treat as minutes
        val m = s.filter { it.isDigit() }.toIntOrNull() ?: 0
        return 0 to m
    }
}
