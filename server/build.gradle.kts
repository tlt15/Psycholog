plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.0.20"
    application
}


dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.sqlite.jdbc)
    implementation(libs.logback.classic)
    implementation(libs.androidx.compose.ui.desktop)
    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.h2)  // для базы в памяти
    testImplementation(libs.exposed.dao) // уже есть, но для тестов тоже
}

application {
    mainClass.set("com.example.server.ApplicationKt")
}