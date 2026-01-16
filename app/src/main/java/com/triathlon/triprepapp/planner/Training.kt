package com.triathlon.triprepapp.planner

import java.io.Serializable

data class Training(
    val id: Long = System.currentTimeMillis(),
    val date: String,
    val time: String,
    val type: String,
    val description: String
) : Serializable
