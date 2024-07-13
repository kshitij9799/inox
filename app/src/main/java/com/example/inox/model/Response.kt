package com.example.inox.model

data class Response(
    val movieDetails: List<Movies>,
    val schedules: List<Schedules>,
    val theatres: List<Theatres>
)
