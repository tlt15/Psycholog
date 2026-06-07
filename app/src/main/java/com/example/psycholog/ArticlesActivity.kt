package com.example.psycholog

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import kotlinx.coroutines.launch
import java.net.URLEncoder

data class Article(val id: Int, val title: String, val content: String)

class ArticlesActivity : AppCompatActivity() {

    private val client = HttpClient(CIO)
    private val baseUrl = "http://10.0.2.2:8080"
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArticleAdapter
    private val articlesList = mutableListOf<Article>()
    private lateinit var editSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var chipGroupHistory: ChipGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_articles)

        recyclerView = findViewById(R.id.recyclerViewArticles)
        editSearch = findViewById(R.id.editSearch)
        btnSearch = findViewById(R.id.btnSearch)
        chipGroupHistory = findViewById(R.id.chipGroupHistory)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ArticleAdapter(articlesList)
        recyclerView.adapter = adapter

        loadArticles()

        btnSearch.setOnClickListener {
            val query = editSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                saveSearchQuery(query)
                performSearch(query)
            } else {
                // Если поле поиска пустое – загружаем все статьи
                loadArticles()
            }
        }

        displaySearchHistory()
    }

    private fun loadArticles() {
        lifecycleScope.launch {
            val articles = fetchArticles()
            articlesList.clear()
            articlesList.addAll(articles)
            adapter.notifyDataSetChanged()
        }
    }

    private fun performSearch(query: String) {
        // Если вдруг вызовут с пустой строкой – показываем все статьи
        if (query.isBlank()) {
            loadArticles()
            return
        }
        lifecycleScope.launch {
            val articles = searchArticles(query)
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

    private suspend fun searchArticles(query: String): List<Article> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val response: String = client.get("$baseUrl/articles/search?q=$encodedQuery").body()
            parseArticles(response)
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка поиска: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun saveSearchQuery(query: String) {
        val prefs = getSharedPreferences("search_prefs", MODE_PRIVATE)
        val history = getSearchHistory().toMutableList()
        history.remove(query)
        history.add(0, query)
        val trimmed = if (history.size > 5) history.subList(0, 5) else history
        prefs.edit().putStringSet("recent_searches", trimmed.toSet()).apply()
        displaySearchHistory()
    }

    private fun getSearchHistory(): List<String> {
        val prefs = getSharedPreferences("search_prefs", MODE_PRIVATE)
        return prefs.getStringSet("recent_searches", emptySet())?.toList() ?: emptyList()
    }

    private fun displaySearchHistory() {
        chipGroupHistory.removeAllViews()
        val history = getSearchHistory()
        for (query in history) {
            val chip = Chip(this)
            chip.text = query
            chip.isClickable = true
            chip.setOnClickListener {
                editSearch.setText(query)
                performSearch(query)
            }
            chipGroupHistory.addView(chip)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.close()
    }
}