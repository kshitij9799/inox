package com.example.inox.model

data class Languages (
    val language: String,
    val potraitPoster: String,
    val landscapePoster: String,
    val filmFormats: List<String>,
    val PosMovieId: List<String>,
    val trailerLink: String,
    val releaseDate: String
    )