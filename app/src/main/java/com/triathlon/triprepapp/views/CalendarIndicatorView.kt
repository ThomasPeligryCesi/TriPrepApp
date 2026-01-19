package com.triathlon.triprepapp.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.triathlon.triprepapp.R
import com.triathlon.triprepapp.planner.Training
import java.text.SimpleDateFormat
import java.util.*

class CalendarIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Map of date to indicator info (count, isPast)
    private data class DateIndicator(val count: Int, val isPast: Boolean)
    private val indicators = mutableMapOf<String, DateIndicator>()

    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)

    fun setTrainings(trainings: List<Training>, month: Int, year: Int) {
        indicators.clear()
        currentMonth = month
        currentYear = year

        val today = Date()
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        // Group trainings by date
        trainings.groupBy { it.date }.forEach { (dateString, trainingsOnDate) ->
            try {
                // Check if this date is in the current month
                val date = dateFormat.parse(dateString)
                if (date != null) {
                    val cal = Calendar.getInstance()
                    cal.time = date

                    if (cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year) {
                        // Check if all trainings on this date are past
                        val allPast = trainingsOnDate.all { training ->
                            try {
                                val trainingDateTime = dateTimeFormat.parse("${training.date} ${training.time}")
                                trainingDateTime?.before(today) ?: false
                            } catch (e: Exception) {
                                false
                            }
                        }

                        indicators[dateString] = DateIndicator(trainingsOnDate.size, allPast)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width == 0 || height == 0) return

        // Approximate calendar grid dimensions
        // Standard CalendarView shows 6 rows of dates
        val cellWidth = width / 7f
        val cellHeight = height / 6f
        val radius = 8f

        // Get the first day of the month
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Draw indicators for each date
        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dateString = dateFormat.format(cal.time)

            indicators[dateString]?.let { indicator ->
                // Calculate position
                val totalDays = firstDayOfWeek + day - 1
                val row = totalDays / 7
                val col = totalDays % 7

                val x = col * cellWidth + cellWidth / 2f
                val y = row * cellHeight + cellHeight - 15f // Position near bottom of cell

                // Choose color based on count and past status
                paint.color = when {
                    indicator.isPast -> ContextCompat.getColor(context, R.color.cloud_gray)
                    indicator.count == 1 -> ContextCompat.getColor(context, android.R.color.holo_green_dark)
                    indicator.count == 2 -> ContextCompat.getColor(context, android.R.color.holo_orange_dark)
                    else -> ContextCompat.getColor(context, android.R.color.holo_red_dark)
                }

                canvas.drawCircle(x, y, radius, paint)
            }
        }
    }
}
