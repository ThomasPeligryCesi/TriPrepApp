package com.triathlon.triprepapp.planner

import java.io.Serializable

data class TrainingPart(
    val type: String,  // V2, V3, V4, Endurance, Tempo, etc.
    val duration: String,  // Format: "1h00", "45min", etc.
    var completed: Boolean = false  // Indicates if this part was completed
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

    fun getPartTypes(): List<String> {
        return when(this) {
            SWIMMING -> listOf(
                "Panaché",
                "Diff. de cr",
                "Climax",
                "Cr",
                "Pull",
                "Plaquettes",
                "R",
                "Prog.",
                "Sprint",
                "Respi 3,5,7...temps",
                "Jbes avec planche",
                "P + P",
                "Sans matos"
            )
            CYCLING -> listOf(
                "Vélocité",
                "End. de force",
                "Parcours plat",
                "Parcours semi-vallonné",
                "Parcours vallonné",
                "Ench. à pied"
            )
            RUNNING -> listOf(
                "Échauffement",
                "Retour au calme",
                "R",
                "Intensité maximale",
                "En trottinant",
                "Endurance",
                "Tempo",
                "Fractionné",
                "Seuil"
            )
        }
    }
}

data class Training(
    val id: Long = System.currentTimeMillis(),
    val date: String,  // Format: "yyyy-MM-dd"
    val time: String,  // Format: "HH:mm"
    val sport: Sport,
    val parts: List<TrainingPart>,
    var notes: String = "",  // Notes on how the training went
    var reviewed: Boolean = false  // Indicates if the training has been reviewed
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

    fun isPast(): Boolean {
        try {
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            val trainingDateTime = dateFormat.parse("$date $time")
            return trainingDateTime?.before(java.util.Date()) ?: false
        } catch (e: Exception) {
            return false
        }
    }

    fun getCompletionRate(): String {
        if (parts.isEmpty()) return "0%"
        val completedCount = parts.count { it.completed }
        val percentage = (completedCount * 100) / parts.size
        return "$percentage%"
    }
}
