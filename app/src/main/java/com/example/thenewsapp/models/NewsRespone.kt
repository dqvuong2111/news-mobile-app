package com.example.thenewsapp.models

data class NewsRespone(
    val articles: MutableList<Article>,
    val status: String,
    val totalResults: Int
)