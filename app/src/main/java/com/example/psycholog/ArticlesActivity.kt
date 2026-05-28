package com.example.psycholog

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import kotlinx.coroutines.launch

data class Article(val id: Int, val title: String, val content: String)

class ArticlesActivity : AppCompatActivity() {

    private val client = HttpClient(CIO)
    private val baseUrl = "http://10.0.2.2:8080"
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArticleAdapter
    private val articlesList = mutableListOf<Article>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_articles)

        recyclerView = findViewById(R.id.recyclerViewArticles)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ArticleAdapter(articlesList)
        recyclerView.adapter = adapter

        loadArticles()
    }

    private fun loadArticles() {
        lifecycleScope.launch {
            val articles = fetchArticles()
            articlesList.clear()
            articlesList.addAll(articles)
            adapter.notifyDataSetChanged()
        }
    }

    private suspend fun fetchArticles(): List<Article> {
        return try {
            val response: String = client.get("$baseUrl/articles").body()
            parseArticles(response)
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
            emptyList()
        }
    }

    private fun parseArticles(json: String): List<Article> {
        val regex = """\{[^}]*\}""".toRegex()
        return regex.findAll(json).map { match ->
            val obj = match.value
            val id = obj.substringAfter("\"id\":").substringBefore(",").toInt()
            val title = obj.substringAfter("\"title\":\"").substringBefore("\"")
            val content = obj.substringAfter("\"content\":\"").substringBefore("\"")
            Article(id, title, content)
        }.toList()
    }

    override fun onDestroy() {
        super.onDestroy()
        client.close()
    }
}