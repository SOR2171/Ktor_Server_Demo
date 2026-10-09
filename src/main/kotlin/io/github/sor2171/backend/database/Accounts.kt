package io.github.sor2171.backend.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object Accounts : Table("account") {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 255).uniqueIndex("unique_name")
    val password = varchar("password", 255)
    val email = varchar("email", 255).uniqueIndex("unique_email")
    val role = varchar("role", 255)
    val registerTime = datetime("register_time").nullable()

    override val primaryKey = PrimaryKey(id)
}
