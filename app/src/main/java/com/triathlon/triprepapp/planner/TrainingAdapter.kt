package com.triathlon.triprepapp.planner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.triathlon.triprepapp.R

class TrainingAdapter(private val trainings: MutableList<Training>) :
    RecyclerView.Adapter<TrainingAdapter.TrainingViewHolder>() {

    class TrainingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val typeText: TextView = view.findViewById(R.id.trainingType)
        val timeText: TextView = view.findViewById(R.id.trainingTime)
        val descriptionText: TextView = view.findViewById(R.id.trainingDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_training, parent, false)
        return TrainingViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrainingViewHolder, position: Int) {
        val training = trainings[position]
        holder.typeText.text = training.type
        holder.timeText.text = "${training.date} - ${training.time}"
        holder.descriptionText.text = training.description
    }

    override fun getItemCount() = trainings.size

    fun addTraining(training: Training) {
        trainings.add(training)
        notifyItemInserted(trainings.size - 1)
    }
}
