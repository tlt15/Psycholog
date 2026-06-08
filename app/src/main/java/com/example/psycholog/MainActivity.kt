package com.example.psycholog

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.jvm.java

@Serializable
data class StatsResponse(val panicCount: Int, val lastMood: String?)

class MainActivity : AppCompatActivity() {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }
    private val baseUrl = "http://10.0.2.2:8080"
    private lateinit var userId: String
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Проверка авторизации
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val savedUserId = prefs.getString("userId", null)
        if (savedUserId.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        userId = savedUserId

        // UI элементы
        val panicButton = findViewById<Button>(R.id.panicButton)
        val moodHappy = findViewById<Button>(R.id.moodHappyButton)
        val moodNeutral = findViewById<Button>(R.id.moodNeutralButton)
        val moodSad = findViewById<Button>(R.id.moodSadButton)
        val fetchStatsButton = findViewById<Button>(R.id.fetchStatsButton)
        val statsTextView = findViewById<TextView>(R.id.statsTextView)
        val meditationButton = findViewById<Button>(R.id.meditationButton)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnArticles = findViewById<Button>(R.id.btnArticles)

        // Обработчики
        panicButton.setOnClickListener {
            startBreathingExercise()
            lifecycleScope.launch {
                sendPanic()
            }
        }

        moodHappy.setOnClickListener {
            lifecycleScope.launch { sendMood("happy") }
        }
        moodNeutral.setOnClickListener {
            lifecycleScope.launch { sendMood("neutral") }
        }
        moodSad.setOnClickListener {
            lifecycleScope.launch { sendMood("sad") }
        }

        fetchStatsButton.setOnClickListener {
            lifecycleScope.launch {
                val stats = fetchStats()
                statsTextView.text = stats?.let {
                    "Паник сегодня: ${it.panicCount}, Последнее настроение: ${it.lastMood ?: "нет"}"
                } ?: "Ошибка получения статистики"
            }
        }

        meditationButton.setOnClickListener {
            startMeditation()
        }

        btnLogout.setOnClickListener {
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Переход к списку статей
        btnArticles.setOnClickListener {
            startActivity(Intent(this, ArticlesActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val currentUserId = prefs.getString("userId", null)
        if (currentUserId.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else if (currentUserId != userId) {
            userId = currentUserId
            findViewById<TextView>(R.id.statsTextView).text = "Статистика"
        }
    }

    private suspend fun sendPanic() {
        try {
            client.post("$baseUrl/panic") {
                header("X-User-Id", userId)
            }
            Toast.makeText(this, "Событие отправлено", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun sendMood(mood: String) {
        try {
            client.post("$baseUrl/mood") {
                header("X-User-Id", userId)
                setBody("{\"mood\":\"$mood\"}")
            }
            Toast.makeText(this, "Настроение отправлено", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun fetchStats(): StatsResponse? {
        return try {
            val response: String = client.get("$baseUrl/today_stats") {
                header("X-User-Id", userId)
            }.body()
            val panicCount = response.substringAfter("\"panicCount\":").substringBefore(",").toIntOrNull() ?: 0
            val lastMoodRaw = response.substringAfter("\"lastMood\":").substringBefore("}")
            val lastMood = if (lastMoodRaw == "null") null else lastMoodRaw.trim('"')
            StatsResponse(panicCount, lastMood)
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun startBreathingExercise() {
        lifecycleScope.launch {
            repeat(15) {
                delay(4000)
                delay(4000)
            }
            Toast.makeText(this@MainActivity, "Упражнение завершено", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startMeditation() {
        stopMeditation()
        mediaPlayer = MediaPlayer.create(this, R.raw.meditation)
        mediaPlayer?.setOnCompletionListener {
            Toast.makeText(this, "Медитация окончена", Toast.LENGTH_SHORT).show()
            mediaPlayer = null
        }
        mediaPlayer?.start()
    }

    private fun stopMeditation() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMeditation()
        client.close()
    }
}