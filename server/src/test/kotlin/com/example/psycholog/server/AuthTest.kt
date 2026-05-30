package com.example.psycholog.server

import com.example.server.module
import io.ktor.client.request.*
import io.ktor.client.request.post
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class AuthTest {

    @Test
    fun testRegisterAndLogin() = testApplication {
        application { module() }

        val registerResponse = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"testuser", "password":"123"}""")
        }
        assertEquals(HttpStatusCode.OK, registerResponse.status)
        val registerBody = registerResponse.bodyAsText()
        assertTrue(registerBody.contains("userId"))
        val userId = registerBody.substringAfter("\"userId\":\"").substringBefore("\"")

        val loginResponse = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"testuser", "password":"123"}""")
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)
        val loginBody = loginResponse.bodyAsText()
        assertTrue(loginBody.contains("userId"))

        val duplicateResponse = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"testuser", "password":"123"}""")
        }
        assertEquals(HttpStatusCode.Conflict, duplicateResponse.status)
    }
}