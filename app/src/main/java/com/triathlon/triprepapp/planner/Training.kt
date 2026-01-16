package com.triathlon.triprepapp.planner

import java.io.Serializable

data class TrainingPart(
    val type: String,  // V2, V3, V4, Endurance, Tempo, etc.
    val duration: String  // Format: "1h00", "45min", etc.
) : Serializable

enum class Sport {
    RUNNING,
    CYCLING,
    SWIMMING;

    fun displayName(): String {
        return when(this) {
            RUNNING -> "Course"
            CYCLING -> "Vélo"
            SWIMMING -> "Natation"
        }
    }

    fun icon(): String {
        return when(this) {
            RUNNING -> "🏃"
            CYCLING -> "🚴"
            SWIMMING -> "🏊"
        }
    }
}

data class Training(
    val id: Long = System.currentTimeMillis(),
    val date: String,  // Format: "yyyy-MM-dd"
    val time: String,  // Format: "HH:mm"
    val sport: Sport,
    val parts: List<TrainingPart>
) : Serializable {

    fun getDisplayTitle(): String {
        return "${sport.icon()} ${sport.displayName()}"
    }

    fun getDisplayParts(): String {
        return parts.joinToString("\n") { "${it.type} ${it.duration}" }
    }

    fun getTotalDuration(): String {
        // TODO: Calculate total duration from all parts
        return parts.firstOrNull()?.duration ?: ""
    }
}
