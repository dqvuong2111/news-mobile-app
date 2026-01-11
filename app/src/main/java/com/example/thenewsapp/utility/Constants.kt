package com.example.thenewsapp.utility
import com.example.thenewsapp.BuildConfig

class Constants {
    companion object {
        val API_KEY = BuildConfig.NEWS_API_KEY
        const val BASE_URL = "https://newsapi.org/"
        const val SEARCH_DELAY_MS = 500L
        const val QUERY_PAGE_SIZE = 20
    }
}