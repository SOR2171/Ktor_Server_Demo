@file:Suppress("VulnerableLibrariesLocal")

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinxRpc)
    alias(libs.plugins.kotlinNoArg)
    alias(libs.plugins.ktor)
}

noArg {
    annotation("io.github.sor2171.backend.annotation.NoArg")
    invokeInitializers = true
}

group = "io.github.sor2171"
version = "1.0.0"

application {
    mainClass = "io.ktor.server.cio.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Ktor Core & Features
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.config.yaml)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.websockets)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.request.validation)

    // Serialization & Datetime & Kotlin
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core)

    // Security & JWT
    implementation(libs.java.jwt)
    implementation(libs.bcrypt)

    // Database: MySQL / MariaDB & Exposed ORM & HikariCP
    implementation(libs.mysql.connector.j)
    implementation(libs.mariadb.java.client)
    implementation(libs.hikaricp)
    implementation(libs.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.jetbrains.exposed.dao)
    implementation(libs.jetbrains.exposed.kotlin.datetime)

    // Mail
    implementation(libs.jakarta.mail)

    // Redis
    implementation(libs.lettuce.core)

    // RabbitMQ
    implementation(libs.amqp.client)

    // Logging
    implementation(libs.logback)

    // kotlinx-rpc
    implementation(libs.kotlinx.rpc.krpc.ktor.server)
    implementation(libs.kotlinx.rpc.krpc.serialization.json)
    testImplementation(libs.kotlinx.rpc.krpc.ktor.client)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.serverTestHost)
}
