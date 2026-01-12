package com.example.thenewsapp.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.thenewsapp.models.Article
import com.example.thenewsapp.models.NewsRespone
import com.example.thenewsapp.repository.NewsRepository
import com.example.thenewsapp.utility.Resource
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException

class NewsViewModel(app: Application, val newsRepository: NewsRepository): AndroidViewModel(app) {

    val headlines: MutableLiveData<Resource<NewsRespone>> = MutableLiveData()
    var headlinesPage = 1
    var headlinesResponse: NewsRespone? = null

    val searchNews: MutableLiveData<Resource<NewsRespone>> = MutableLiveData()
    var searchNewsPage = 1
    var searchNewsRespone: NewsRespone? = null
    var newSearchQuery: String? = null
    var oldSearchQuery: String? = null

    init {
        getHeadlines("vn")
    }

    fun getHeadlines(countryCode: String) = viewModelScope.launch{
        headlinesInternet(countryCode)
    }

    fun searchNews(searchQuery: String) = viewModelScope.launch {
        searchNewsInternet(searchQuery)
    }

    fun refreshHeadlines(countryCode: String) = viewModelScope.launch {
        // Reset pagination to reload from first page
        headlinesPage = 1
        headlinesResponse = null
        headlinesInternet(countryCode)
    }

    private fun handleHeadlinesResponse(response: Response<NewsRespone>): Resource<NewsRespone>{
        if(response.isSuccessful) {
            response.body()?.let { resultResponse ->
                headlinesPage++
                if (headlinesResponse == null) {
                    headlinesResponse = resultResponse
                } else {
                    val oldArticles = headlinesResponse?.articles
                    val newArticles = resultResponse.articles
                    oldArticles?.addAll(newArticles)
                }
                return Resource.Success(headlinesResponse ?: resultResponse)
            }
        }
        return Resource.Error(response.message())
    }

    private fun handleSearchNewsResponse(response: Response<NewsRespone>): Resource<NewsRespone>{
        if(response.isSuccessful) {
            response.body()?.let { resultResponse ->
                resultResponse.articles.removeAll { it.urlToImage.isNullOrBlank() }
                if (searchNewsRespone == null || newSearchQuery != oldSearchQuery) {
                    searchNewsPage = 1
                    oldSearchQuery = newSearchQuery
                    searchNewsRespone = resultResponse
                } else {
                    searchNewsPage++
                    val oldArticles = searchNewsRespone?.articles
                    val newArticles = resultResponse.articles
                    oldArticles?.addAll(newArticles)
                }
                return Resource.Success(searchNewsRespone ?: resultResponse)
            }
        }
        return Resource.Error(response.message())
    }

    fun addToFavourites(article: Article) = viewModelScope.launch {
        article.isFavourite = true
        newsRepository.upsert(article)
    }

    fun getFavourites() = newsRepository.getFavourtiesNews()

    fun deleteArticles(article: Article) = viewModelScope.launch {
        newsRepository.deleteArticle(article)
    }

    fun internetConnection(context: Context): Boolean {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).apply {
            return getNetworkCapabilities(activeNetwork)?.run {
                when {
                    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            } ?: false
        }
    }

    private suspend fun headlinesInternet(countryCode: String) {
        headlines.postValue(Resource.Loading())
        try {
            if(internetConnection(this.getApplication())) {
                val response = if (countryCode.equals("vn", ignoreCase = true)) {
                    newsRepository.searchNews("Vietnam", headlinesPage)
                } else {
                    newsRepository.getHeadlines(countryCode, headlinesPage)
                }

                if (response.isSuccessful) {
                    response.body()?.let { result ->
                        result.articles.removeAll { it.urlToImage.isNullOrBlank() }

                        if (headlinesPage == 1) {
                            newsRepository.clearHeadlines()
                        }
                        val favArticles = newsRepository.getFavouritesList()
                        val favUrls = favArticles.map { it.url }.toSet()

                        result.articles.forEach { article ->
                            if (!favUrls.contains(article.url)) {
                                article.isFavourite = false
                                newsRepository.upsert(article)
                            }
                        }
                    }
                }
                headlines.postValue(handleHeadlinesResponse((response)))
            }
            else {
                loadOfflineHeadlines()
            }
        } catch (t: Throwable) {
            loadOfflineHeadlines()
        }
    }

    private suspend fun loadOfflineHeadlines() {
        val offlineArticles = newsRepository.getHeadlinesList()
        val offlineResponse = NewsRespone(offlineArticles.toMutableList(), "ok", offlineArticles.size)
        headlines.postValue(Resource.Success(offlineResponse))
    }

    fun getArticleContent(url: String, onResult: (String?) -> Unit) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val doc = org.jsoup.Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(10000)
                .get()
            
            // Try common article content selectors
            val articleSelectors = listOf(
                "article p",
                "[class*=article-body] p",
                "[class*=article-content] p", 
                "[class*=post-content] p",
                "[class*=entry-content] p",
                "[class*=story-body] p",
                ".content p",
                "main p",
                "p"
            )
            
            var content: String? = null
            for (selector in articleSelectors) {
                val elements = doc.select(selector)
                if (elements.isNotEmpty()) {
                    val text = elements.eachText().joinToString("\n\n")
                    if (text.length > 200) { // Only use if we got substantial content
                        content = text
                        break
                    }
                }
            }
            
            // Fallback to all paragraphs if no selector worked
            if (content.isNullOrBlank()) {
                content = doc.select("p").eachText().joinToString("\n\n")
            }
            
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                onResult(content?.takeIf { it.length > 50 })
            }
        } catch (e: Exception) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                onResult(null)
            }
        }
    }

    private suspend fun searchNewsInternet(searchQuery: String) {
        newSearchQuery = searchQuery
        searchNews.postValue(Resource.Loading())
        try {
            if(internetConnection(this.getApplication())) {
                val response = newsRepository.searchNews(searchQuery, searchNewsPage)
                searchNews.postValue(handleSearchNewsResponse(response))
            }
            else {
                searchNews.postValue(Resource.Error("No internet connection"))
            }
        } catch (t: Throwable) {
            when(t) {
                is IOException -> searchNews.postValue(Resource.Error("Unable to connect to the internet"))
                else -> searchNews.postValue(Resource.Error("No signal"))
            }
        }
    }
}