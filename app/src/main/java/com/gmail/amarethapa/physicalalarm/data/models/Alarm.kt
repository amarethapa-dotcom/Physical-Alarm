package com.gmail.amarethapa.physicalalarm.data.models

// A simple Kotlin Data Class (Replaces heavy Java POJOs)
data class Alarm(
    val id: Int,
    val time: String,
    val days: String,
    val isEnabled: Boolean
)

