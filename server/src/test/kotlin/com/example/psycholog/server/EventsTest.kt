package com.example.psycholog.server

import com.example.server.module
import io.ktor.client.request.*
import io.ktor.client.request.post
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class EventsTest {

    @Test
    fun testPanicAndMoodAndStats() = testApplication {
        application { module() }

        val registerResponse = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"statuser", "password":"pass"}""")
        }
        val userId = registerResponse.bodyAsText()
            .substringAfter("\"userId\":\"")
            .substringBefore("\"")

        val panicResponse = client.post("/panic") {
            header("X-User-Id", userId)
        }
        assertEquals(HttpStatusCode.OK, panicResponse.status)

        val moodResponse = client.post("/mood") {
            header("X-User-Id", userId)
            contentType(ContentType.Application.Json)
            setBody("""{"mood":"happy"}""")
        }
        assertEquals(HttpStatusCode.OK, moodResponse.status)

        val statsResponse = client.get("/today_stats") {
            header("X-User-Id", userId)
        }
        assertEquals(HttpStatusCode.OK, statsResponse.status)
        val statsBody = statsResponse.bodyAsText()
        assertTrue(statsBody.contains("\"panicCount\":1"))
        assertTrue(statsBody.contains("\"lastMood\":\"happy\""))
    }
}