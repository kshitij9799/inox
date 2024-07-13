package com.example.inox.model

data class Movies(
    val filmcommonId: String,
    val filmName: String,
    val PGRating: String,
    val runTime: String,
    val filmGenre: String,
    val filmStatus: String,
    val castCrew: String,
    val director: String,
    val synopsis: String,
    val movieIds: ArrayList<Array<String>>,
    val languages: List<Languages>
)
