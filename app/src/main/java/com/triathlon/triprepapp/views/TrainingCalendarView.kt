package com.triathlon.triprepapp.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.triathlon.triprepapp.R
import com.triathlon.triprepapp.planner.Training
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Fully custom calendar view. Draws its own month header, weekday labels, and date grid so
 * training-load indicator circles are guaranteed to land on the correct day (unlike overlaying
 * a canvas on top of the native CalendarView, whose internal layout is not exposed).
 *
 * Week starts on Monday (French convention). Tap a day to select it. Tap the header chevrons
 * to navigate months.
 */
class TrainingCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.FRENCH)

    private val weekdayLabels = arrayOf("L", "M", "M", "J", "V", "S", "D")

    private data class DayIndicator(val count: Int, val allPast: Boolean)
    private val indicators = mutableMapOf<String, DayIndicator>()

    private val today: Calendar = Calendar.getInstance()
    private val displayed: Calendar = Calendar.getInstance()
    private var selectedDate: String = dateFormat.format(today.time)

    private var onDateSelectedListener: ((String) -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val headerHeight = 48f * density
    private val weekdayRowHeight = 28f * density

    // Hit targets for the previous/next month chevrons
    private val prevHitRect = RectF()
    private val nextHitRect = RectF()

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    fun setOnDateSelectedListener(listener: (String) -> Unit) {
        onDateSelectedListener = listener
    }

    fun getSelectedDate(): String = selectedDate

    fun setSelectedDate(dateString: String) {
        selectedDate = dateString
        try {
            dateFormat.parse(dateString)?.let {
                displayed.time = it
            }
        } catch (_: Exception) {
        }
        invalidate()
    }

    fun getDisplayedMonth(): Int = displayed.get(Calendar.MONTH)
    fun getDisplayedYear(): Int = displayed.get(Calendar.YEAR)

    /**
     * Rebuilds indicator data for the currently displayed month from the full training list.
     * Call this whenever trainings change or the month changes.
     */
    fun setTrainings(trainings: List<Training>) {
        indicators.clear()
        val now = Date()
        val month = displayed.get(Calendar.MONTH)
        val year = displayed.get(Calendar.YEAR)

        trainings.groupBy { it.date }.forEach { (dateString, list) ->
            try {
                val date = dateFormat.parse(dateString) ?: return@forEach
                val cal = Calendar.getInstance().apply { time = date }
                if (cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year) {
                    val allPast = list.all { t ->
                        try {
                            dateTimeFormat.parse("${t.date} ${t.time}")?.before(now) ?: false
                        } catch (_: Exception) {
                            false
                        }
                    }
                    indicators[dateString] = DayIndicator(list.size, allPast)
                }
            } catch (_: Exception) {
            }
        }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        // 6 rows of date cells, each ~44dp tall
        val desiredHeight = (headerHeight + weekdayRowHeight + 6f * 44f * density).toInt()
        val h = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> minOf(desiredHeight, MeasureSpec.getSize(heightMeasureSpec))
            else -> desiredHeight
        }
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        drawHeader(canvas)
        drawWeekdays(canvas)
        drawDayCells(canvas)
    }

    private fun drawHeader(canvas: Canvas) {
        // Month label
        textPaint.color = ContextCompat.getColor(context, R.color.text_primary)
        textPaint.textSize = 18f * density
        textPaint.isFakeBoldText = true
        val label = monthFormat.format(displayed.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString() }
        val centerY = headerHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, width / 2f, centerY, textPaint)
        textPaint.isFakeBoldText = false

        // Chevrons
        val chevronSize = 18f * density
        val chevronY = headerHeight / 2f
        strokePaint.color = ContextCompat.getColor(context, R.color.ocean_blue)
        strokePaint.strokeWidth = 3f * density
        strokePaint.strokeCap = Paint.Cap.ROUND

        // Prev (left)
        val prevCenterX = 24f * density
        canvas.drawLine(prevCenterX + chevronSize / 3f, chevronY - chevronSize / 3f,
            prevCenterX - chevronSize / 3f, chevronY, strokePaint)
        canvas.drawLine(prevCenterX - chevronSize / 3f, chevronY,
            prevCenterX + chevronSize / 3f, chevronY + chevronSize / 3f, strokePaint)
        prevHitRect.set(0f, 0f, prevCenterX + chevronSize, headerHeight)

        // Next (right)
        val nextCenterX = width - 24f * density
        canvas.drawLine(nextCenterX - chevronSize / 3f, chevronY - chevronSize / 3f,
            nextCenterX + chevronSize / 3f, chevronY, strokePaint)
        canvas.drawLine(nextCenterX + chevronSize / 3f, chevronY,
            nextCenterX - chevronSize / 3f, chevronY + chevronSize / 3f, strokePaint)
        nextHitRect.set(nextCenterX - chevronSize, 0f, width.toFloat(), headerHeight)
    }

    private fun drawWeekdays(canvas: Canvas) {
        textPaint.color = ContextCompat.getColor(context, R.color.ocean_blue)
        textPaint.textSize = 12f * density
        textPaint.isFakeBoldText = true
        val cellWidth = width / 7f
        val y = headerHeight + weekdayRowHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        for (i in 0 until 7) {
            canvas.drawText(weekdayLabels[i], cellWidth * i + cellWidth / 2f, y, textPaint)
        }
        textPaint.isFakeBoldText = false
    }

    private fun drawDayCells(canvas: Canvas) {
        val cellWidth = width / 7f
        val gridTop = headerHeight + weekdayRowHeight
        val gridHeight = height - gridTop
        val cellHeight = gridHeight / 6f

        val cal = displayed.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        // Convert Calendar.DAY_OF_WEEK (Sun=1..Sat=7) to Monday-start column (Mon=0..Sun=6)
        val firstColumn = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val todayString = dateFormat.format(today.time)

        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dateString = dateFormat.format(cal.time)
            val position = firstColumn + day - 1
            val row = position / 7
            val col = position % 7
            val cx = cellWidth * col + cellWidth / 2f
            val cy = gridTop + cellHeight * row + cellHeight / 2f

            // Sizing the indicator/selection circle to the smaller of cell dimensions
            val radius = minOf(cellWidth, cellHeight) * 0.38f

            // Selected day background (filled light blue)
            if (dateString == selectedDate) {
                fillPaint.color = ContextCompat.getColor(context, R.color.ocean_blue)
                canvas.drawCircle(cx, cy, radius, fillPaint)
            } else if (dateString == todayString) {
                // Subtle "today" background so current day is always visible
                fillPaint.color = ContextCompat.getColor(context, R.color.sky_blue_light)
                fillPaint.alpha = 60
                canvas.drawCircle(cx, cy, radius, fillPaint)
                fillPaint.alpha = 255
            }

            // Training indicator ring (drawn around the day)
            indicators[dateString]?.let { indicator ->
                strokePaint.color = when {
                    indicator.allPast -> ContextCompat.getColor(context, R.color.cloud_gray)
                    indicator.count == 1 -> ContextCompat.getColor(context, R.color.mint)
                    indicator.count == 2 -> ContextCompat.getColor(context, R.color.coral)
                    else -> ContextCompat.getColor(context, R.color.lavender)
                }
                strokePaint.strokeWidth = 3f * density
                canvas.drawCircle(cx, cy, radius + 2f * density, strokePaint)
            }

            // Day number
            textPaint.color = when {
                dateString == selectedDate -> ContextCompat.getColor(context, R.color.white)
                dateString == todayString -> ContextCompat.getColor(context, R.color.ocean_blue)
                else -> ContextCompat.getColor(context, R.color.text_primary)
            }
            textPaint.textSize = 15f * density
            textPaint.isFakeBoldText = dateString == todayString || dateString == selectedDate
            val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(day.toString(), cx, textY, textPaint)
            textPaint.isFakeBoldText = false
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        val x = event.x
        val y = event.y

        // Header taps switch month
        if (y <= headerHeight) {
            if (prevHitRect.contains(x, y)) {
                displayed.add(Calendar.MONTH, -1)
                invalidate()
                notifyMonthChanged()
                return true
            }
            if (nextHitRect.contains(x, y)) {
                displayed.add(Calendar.MONTH, 1)
                invalidate()
                notifyMonthChanged()
                return true
            }
            return true
        }

        // Grid taps select day
        val gridTop = headerHeight + weekdayRowHeight
        if (y < gridTop) return true
        val cellWidth = width / 7f
        val cellHeight = (height - gridTop) / 6f
        val col = (x / cellWidth).toInt().coerceIn(0, 6)
        val row = ((y - gridTop) / cellHeight).toInt().coerceIn(0, 5)

        val cal = displayed.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstColumn = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dayIndex = row * 7 + col - firstColumn + 1
        if (dayIndex in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, dayIndex)
            selectedDate = dateFormat.format(cal.time)
            onDateSelectedListener?.invoke(selectedDate)
            invalidate()
        }
        return true
    }

    private fun notifyMonthChanged() {
        // Emitting the first day of the displayed month lets the host activity reload indicators,
        // but we don't change the selected date — the activity decides whether to re-select.
        onMonthChangedListener?.invoke(getDisplayedYear(), getDisplayedMonth())
    }

    private var onMonthChangedListener: ((Int, Int) -> Unit)? = null
    fun setOnMonthChangedListener(listener: (Int, Int) -> Unit) {
        onMonthChangedListener = listener
    }
}
