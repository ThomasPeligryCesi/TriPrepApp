package com.triathlon.triprepapp.planner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.triathlon.triprepapp.R

class TrainingPartAdapter(
    private val parts: MutableList<TrainingPart>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<TrainingPartAdapter.PartViewHolder>() {

    class PartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val typeText: TextView = view.findViewById(R.id.partType)
        val durationText: TextView = view.findViewById(R.id.partDuration)
        val removeButton: MaterialButton = view.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_training_part, parent, false)
        return PartViewHolder(view)
    }

    override fun onBindViewHolder(holder: PartViewHolder, position: Int) {
        val part = parts[position]
        holder.typeText.text = part.type
        holder.durationText.text = part.duration

        holder.removeButton.setOnClickListener {
            onRemoveClick(position)
        }
    }

    override fun getItemCount() = parts.size

    fun addPart(part: TrainingPart) {
        parts.add(part)
        notifyItemInserted(parts.size - 1)
    }

    fun removePart(position: Int) {
        if (position >= 0 && position < parts.size) {
            parts.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun getParts(): List<TrainingPart> = parts.toList()
}
