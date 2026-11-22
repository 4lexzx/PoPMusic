package com.popmusic.app.data.recommendations

/** Tarjeta de "Estados de ánimo" (Moods de Echo): busca y reproduce una mezcla. */
data class Mood(
    val label: String,
    val query: String,
)

val MOODS = listOf(
    Mood("Fiesta", "fiesta reggaeton mix"),
    Mood("Energía", "workout motivacion mix"),
    Mood("Relax", "relax chill instrumental"),
    Mood("Concentración", "lofi beats para estudiar"),
    Mood("Romance", "canciones romanticas"),
    Mood("Tristeza", "sad songs en español"),
    Mood("Clásicos", "rock clasicos 80s 90s"),
    Mood("Reggaetón", "reggaeton exitos"),
)
