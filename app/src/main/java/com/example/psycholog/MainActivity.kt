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
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class StatsResponse(val panicCount: Int, val lastMood: String?)
data class MoodRequest(val mood: String)


class MainActivity : AppCompatActivity() {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }
    private val baseUrl = "http://10.0.2.2:8080"
    private lateinit var userId: String

    // Для медитации
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Генерация или загрузка анонимного ID
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        userId = prefs.getString("userId", null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString("userId", newId).apply()
            newId
        }

        val panicButton = findViewById<Button>(R.id.panicButton)
        val moodHappy = findViewById<Button>(R.id.moodHappyButton)
        val moodNeutral = findViewById<Button>(R.id.moodNeutralButton)
        val moodSad = findViewById<Button>(R.id.moodSadButton)
        val fetchStatsButton = findViewById<Button>(R.id.fetchStatsButton)
        val statsTextView = findViewById<TextView>(R.id.statsTextView)
        val meditationButton = findViewById<Button>(R.id.meditationButton)

        panicButton.setOnClickListener {
            startBreathingExercise()
            lifecycleScope.launch {
                sendPanic()
            }
        }

        moodHappy.setOnClickListener {
            lifecycleScope.launch {
                sendMood("happy")
            }
        }
        moodNeutral.setOnClickListener {
            lifecycleScope.launch {
                sendMood("neutral")
            }
        }
        moodSad.setOnClickListener {
            lifecycleScope.launch {
                sendMood("sad")
            }
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
                contentType(io.ktor.http.ContentType.Application.Json)
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
            // Парсим JSON вручную
            val panicCount = response.substringAfter("\"panicCount\":").substringBefore(",").toIntOrNull() ?: 0
            val lastMoodRaw = response.substringAfter("\"lastMood\":").substringBefore("}")
            val lastMood = if (lastMoodRaw == "null") null else lastMoodRaw.trim('"')
            StatsResponse(panicCount, lastMood)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun startBreathingExercise() {
        lifecycleScope.launch {
            // 2 минуты = 120 секунд. Делаем 15 циклов по 8 секунд (4 сек вдох + 4 сек выдох)
            repeat(15) {
                // Вдох (4 секунды)
                delay(4000)
                // Выдох (4 секунды)
                delay(4000)
            }
            Toast.makeText(this@MainActivity, "Упражнение завершено", Toast.LENGTH_SHORT).show()
        }
    }


    private fun startMeditation() {
        stopMeditation()
        mediaPlayer = MediaPlayer.create(this, R.raw.meditation) // нужно добавить аудиофайл
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