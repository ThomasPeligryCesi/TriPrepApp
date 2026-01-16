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
