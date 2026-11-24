package com.example.mojadieta.domain.model

/**
 * Posiłek rozpoznany przez model wizyjny AI ze zdjęcia.
 */
data class RecognizedMeal(
    val name: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val salt: Double? = null
)
