package com.example.psycholog.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Application.configureRouting() {
    routing {

        // Регистрация
        post("/register") {
            val request = call.receiveText()
            val login = request.substringAfter("\"login\":\"").substringBefore("\"")
            val password = request.substringAfter("\"password\":\"").substringBefore("\"")
            val passwordHash = password.hashCode()

            if (login.isBlank() || password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Login and password required")
                return@post
            }

            val existing = transaction {
                Users.select { Users.login eq login }.firstOrNull()
            }
            if (existing != null) {
                call.respond(HttpStatusCode.Conflict, "Login already exists")
                return@post
            }

            val newUserId = UUID.randomUUID().toString()
            transaction {
                Users.insert {
                    it[Users.login] = login
                    it[Users.passwordHash] = passwordHash
                    it[Users.userId] = newUserId
                }
            }
            call.respond(mapOf("userId" to newUserId))
        }

        // Логин
        post("/login") {
            val request = call.receiveText()
            val login = request.substringAfter("\"login\":\"").substringBefore("\"")
            val password = request.substringAfter("\"password\":\"").substringBefore("\"")
            val passwordHash = password.hashCode()

            val user = transaction {
                Users.select {
                    (Users.login eq login) and (Users.passwordHash eq passwordHash)
                }.firstOrNull()
            }
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid login or password")
                return@post
            }
            call.respond(mapOf("userId" to user[Users.userId]))
        }

        // Паника
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

        // Настроение
        post("/mood") {
            val userId = call.request.headers["X-User-Id"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing user id")
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
        }

        // Статистика за сегодня
        get("/today_stats") {
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

            val json = "{\"panicCount\":$panicCount,\"lastMood\":${if (lastMood != null) "\"$lastMood\"" else "null"}}"
            call.respondText(json, ContentType.Application.Json)
        }

        // Получение всех статей
        get("/articles") {
            val articles = transaction {
                Articles.selectAll().orderBy(Articles.createdAt to SortOrder.DESC).map {
                    mapOf(
                        "id" to it[Articles.id],
                        "title" to it[Articles.title],
                        "content" to it[Articles.content]
                    )
                }
            }
            val json = buildString {
                append("[")
                articles.forEachIndexed { idx, art ->
                    if (idx > 0) append(",")
                    append("{\"id\":${art["id"]},\"title\":\"${art["title"]}\",\"content\":\"${art["content"]}\"}")
                }
                append("]")
            }
            call.respondText(json, ContentType.Application.Json)
        }

        // Поиск статей
        get("/articles/search") {
            val query = call.request.queryParameters["q"] ?: ""
            val articles = if (query.isBlank()) {
                emptyList()
            } else {
                transaction {
                    Articles.select {
                        (Articles.title like "%$query%") or (Articles.content like "%$query%")
                    }.orderBy(Articles.createdAt to SortOrder.DESC).map {
                        mapOf(
                            "id" to it[Articles.id],
                            "title" to it[Articles.title],
                            "content" to it[Articles.content]
                        )
                    }
                }
            }
            val json = buildString {
                append("[")
                articles.forEachIndexed { idx, art ->
                    if (idx > 0) append(",")
                    append("{\"id\":${art["id"]},\"title\":\"${art["title"]}\",\"content\":\"${art["content"]}\"}")
                }
                append("]")
            }
            call.respondText(json, ContentType.Application.Json)
        }
    }
}