package com.example.server

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object Events : Table() {
    val id = integer("id").autoIncrement()
    val userId = varchar("user_id", 64)
    val timestamp = long("timestamp")
    val type = varchar("type", 10)
    val value = varchar("value", 10)
    override val primaryKey = PrimaryKey(id)
}
object Articles : Table() {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 255)
    val content = text("content")
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

object Users : Table() {
    val id = integer("id").autoIncrement()
    val login = varchar("login", 64).uniqueIndex()
    val passwordHash = integer("password_hash")
    val userId = varchar("user_id", 64).uniqueIndex()
    override val primaryKey = PrimaryKey(id)
}

fun initDatabase() {
    val dbFile = File("app.db")
    Database.connect("jdbc:sqlite:${dbFile.absolutePath}", "org.sqlite.JDBC")
    transaction {
        SchemaUtils.create(Events)
        SchemaUtils.create(Events, Users)
        SchemaUtils.create(Events, Users, Articles)
        if (Articles.selectAll().empty()) {
            val now = System.currentTimeMillis()
            Articles.insert {
                it[title] = "Как справляться со стрессом"
                it[content] = "Стресс — это естественная реакция организма на нагрузку. Чтобы снизить уровень стресса, попробуйте глубокое дыхание, прогулки на свежем воздухе и планирование задач. Также полезно вести дневник эмоций."
                it[createdAt] = now
            }
            Articles.insert {
                it[title] = "Техники медитации для начинающих"
                it[content] = "Начните с 5 минут в день. Сядьте удобно, закройте глаза и сосредоточьтесь на дыхании. Не пытайтесь остановить мысли — просто наблюдайте за ними. Постепенно увеличивайте время до 15–20 минут."
                it[createdAt] = now + 1000
            }
            Articles.insert {
                it[title] = "Психология счастья"
                it[content] = "Счастье не является конечной целью, а складывается из маленьких радостей каждый день. Практикуйте благодарность, занимайтесь любимым делом и поддерживайте социальные связи."
                it[createdAt] = now + 2000
            }
            Articles.insert {
                it[title] = "Как победить прокрастинацию"
                it[content] = "Прокрастинация — это откладывание дел на потом. Разбейте задачу на мелкие шаги, используйте технику «Помидора» (25 минут работы, 5 отдыха) и награждайте себя за выполнение."
                it[createdAt] = now + 3000
            }
            Articles.insert {
                it[title] = "Улучшение самооценки"
                it[content] = "Низкая самооценка мешает достигать целей. Каждый день записывайте три своих достижения, перестаньте сравнивать себя с другими и практикуйте позитивные аффирмации."
                it[createdAt] = now + 4000
            }
            Articles.insert {
                it[title] = "Эмоциональный интеллект"
                it[content] = "Эмоциональный интеллект — это умение распознавать и управлять своими эмоциями и эмоциями других. Развивайте его через наблюдение за своими чувствами и эмпатию."
                it[createdAt] = now + 5000
            }
            Articles.insert {
                it[title] = "Как наладить сон"
                it[content] = "Качественный сон важен для психического здоровья. Ложитесь и вставайте в одно время, избегайте экранов за час до сна, проветривайте комнату и не ешьте тяжёлую пищу на ночь."
                it[createdAt] = now + 6000
            }
            Articles.insert {
                it[title] = "Преодоление тревожности"
                it[content] = "При тревоге попробуйте техники заземления: назовите 5 предметов вокруг, 4 звука, 3 тактильных ощущения, 2 запаха и 1 вкус. Также помогает дыхание по квадрату (4 сек вдох, 4 задержка, 4 выдох, 4 задержка)."
                it[createdAt] = now + 7000
            }
            Articles.insert {
                it[title] = "Как сказать нет"
                it[content] = "Умение отказывать — важный навык для сохранения личных границ. Говорите «нет» уверенно, без оправданий, предлагайте альтернативу, если хотите, и не чувствуйте вины."
                it[createdAt] = now + 8000
            }
            Articles.insert {
                it[title] = "Позитивное мышление"
                it[content] = "Позитивное мышление не означает игнорировать проблемы. Это умение находить возможности в трудностях, благодарить за хорошее и не зацикливаться на негативе. Начните с ведения дневника благодарности."
                it[createdAt] = now + 9000
            }
        }
    }
}
