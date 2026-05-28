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
    }
}