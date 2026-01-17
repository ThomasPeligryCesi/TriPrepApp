package com.triathlon.triprepapp.planner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.triathlon.triprepapp.R

class TrainingPartReviewAdapter(
    private val parts: List<TrainingPart>
) : RecyclerView.Adapter<TrainingPartReviewAdapter.PartViewHolder>() {

    class PartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: CheckBox = view.findViewById(R.id.partCompletedCheckbox)
        val typeText: TextView = view.findViewById(R.id.partTypeText)
        val durationText: TextView = view.findViewById(R.id.partDurationText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_training_part_review, parent, false)
        return PartViewHolder(view)
    }

    override fun onBindViewHolder(holder: PartViewHolder, position: Int) {
        val part = parts[position]
        holder.typeText.text = part.type
        holder.durationText.text = part.duration
        holder.checkbox.isChecked = part.completed

        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            part.completed = isChecked
        }
    }

    override fun getItemCount() = parts.size
}
