package com.triathlon.triprepapp.planner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.triathlon.triprepapp.R

class TrainingAdapter(
    private val trainings: MutableList<Training>,
    private val onTrainingClick: (Training) -> Unit = {}
) : RecyclerView.Adapter<TrainingAdapter.TrainingViewHolder>() {

    class TrainingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val sportIcon: TextView = view.findViewById(R.id.trainingSportIcon)
        val sportName: TextView = view.findViewById(R.id.trainingSportName)
        val timeText: TextView = view.findViewById(R.id.trainingTime)
        val partsText: TextView = view.findViewById(R.id.trainingParts)
        val completionStatus: TextView = view.findViewById(R.id.trainingCompletionStatus)
        val notesText: TextView = view.findViewById(R.id.trainingNotes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_training, parent, false)
        return TrainingViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrainingViewHolder, position: Int) {
        val training = trainings[position]
        holder.sportIcon.text = training.sport.icon()
        holder.sportName.text = training.sport.displayName()
        holder.timeText.text = training.time
        holder.partsText.text = training.getDisplayParts()

        // Show completion status if training is past
        if (training.isPast() && training.reviewed) {
            holder.completionStatus.visibility = View.VISIBLE
            holder.completionStatus.text = "Validé : ${training.getCompletionRate()}"
        } else if (training.isPast()) {
            holder.completionStatus.visibility = View.VISIBLE
            holder.completionStatus.text = "En attente de validation"
        } else {
            holder.completionStatus.visibility = View.GONE
        }

        // Show notes if available
        if (training.notes.isNotEmpty()) {
            holder.notesText.visibility = View.VISIBLE
            holder.notesText.text = "Note : ${training.notes}"
        } else {
            holder.notesText.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onTrainingClick(training)
        }
    }

    override fun getItemCount() = trainings.size

    fun addTraining(training: Training) {
        trainings.add(training)
        notifyItemInserted(trainings.size - 1)
    }

    fun updateTrainings(newTrainings: List<Training>) {
        trainings.clear()
        trainings.addAll(newTrainings)
        notifyDataSetChanged()
    }
}
