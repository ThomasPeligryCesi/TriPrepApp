package com.triathlon.triprepapp.planner

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.LineBackgroundSpan
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade

/**
 * Decorator for calendar to show training indicators
 */
class TrainingDecorator(
    private val date: CalendarDay,
    private val trainingCount: Int,
    private val color: Int
) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return day == date
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(TrainingDotSpan(color, trainingCount))
    }
}

/**
 * Custom span to draw a colored dot below the date
 */
class TrainingDotSpan(
    private val color: Int,
    private val count: Int
) : LineBackgroundSpan {

    override fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lineNumber: Int
    ) {
        val oldColor = paint.color
        paint.color = color

        // Calculate radius based on count
        // 1 training: 8dp, 2 trainings: 12dp, 3+ trainings: 16dp
        val baseRadius = when (count) {
            1 -> 8f
            2 -> 12f
            else -> 16f
        }

        // Draw circle below the date text
        val centerX = (left + right) / 2f
        val centerY = bottom + baseRadius + 8f

        canvas.drawCircle(centerX, centerY, baseRadius, paint)
        paint.color = oldColor
    }
}
