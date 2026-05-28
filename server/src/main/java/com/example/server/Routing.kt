package com.example.server

import java.util.UUID
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureRouting() {
    routing {
        get("/today_stats") {
            try {
                val userId = call.request.headers["X-User-Id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing user id")

                val startOfDay = System.currentTimeMillis() - (System.currentTimeMillis() % (24 * 60 * 60 * 1000))

                var panicCount = 0
                var lastMood: String? = null

                transaction {
                    panicCount = Events.select {
                        (Events.userId eq userId) and
                                (Events.type eq "panic") and
                                (Events.timestamp greaterEq startOfDay)
                    }.count().toInt()

                    val latestMood = Events.select {
                        (Events.userId eq userId) and
                                (Events.type eq "mood") and
                                (Events.timestamp greaterEq startOfDay)
                    }.orderBy(Events.timestamp to SortOrder.DESC).limit(1).firstOrNull()

                    lastMood = latestMood?.get(Events.value)
                }

                val lastMoodJson = if (lastMood != null) "\"$lastMood\"" else "null"
                call.respondText(
                    text = "{\"panicCount\":$panicCount,\"lastMood\":$lastMoodJson}",
                    contentType = ContentType.Application.Json
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
            }
        }

        post("/mood") {
            try {
                val userId = call.request.headers["X-User-Id"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing user id")

                // Получаем тело запроса как текст и разбираем вручную (без сериализации)
                val text = call.receiveText()
                val mood = text.substringAfter("\"mood\":\"").substringBefore("\"")

                val timestamp = System.currentTimeMillis()
                transaction {
                    Events.insert {
                        it[Events.userId] = userId
                        it[Events.timestamp] = timestamp
                        it[Events.type] = "mood"
                        it[Events.value] = mood
                    }
                }
                call.respond(HttpStatusCode.OK, "Mood recorded")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid request")
            }
        }

        post("/register") {
            val request = call.receiveText()
            val login = request.substringAfter("\"login\":\"").substringBefore("\"")
            val password = request.substringAfter("\"password\":\"").substringBefore("\"")

            val existing = transaction {
                Users.select { Users.login eq login }.firstOrNull()
            }
            if (existing != null) {
                call.respond(HttpStatusCode.Conflict, "Login already exists")
                return@post
            }

            val newUserId = UUID.randomUUID().toString()
            val passwordHash = password.hashCode()

            transaction {
                Users.insert {
                    it[Users.login] = login
                    it[Users.passwordHash] = passwordHash
                    it[Users.userId] = newUserId
                }
            }
            post("/login") {
                val request = call.receiveText()
                val login = request.substringAfter("\"login\":\"").substringBefore("\"")
                val password = request.substringAfter("\"password\":\"").substringBefore("\"")
                val passwordHash = password.hashCode()

                val user = transaction {
                    Users.select { (Users.login eq login) and (Users.passwordHash eq passwordHash) }
                        .firstOrNull()
                }

                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid login or password")
                    return@post
                }

                val userId = user[Users.userId]
                call.respond(mapOf("userId" to userId))
            }

            call.respond(mapOf("userId" to newUserId))
        }

        post("/panic") {
            val userId = call.request.headers["X-User-Id"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing user id")
            val timestamp = System.currentTimeMillis()

            transaction {
                Events.insert {
                    it[Events.userId] = userId
                    it[Events.timestamp] = timestamp
                    it[Events.type] = "panic"
                    it[Events.value] = "1"
                }
            }

            call.respond(HttpStatusCode.OK, "Panic event recorded")
        }
    }
}