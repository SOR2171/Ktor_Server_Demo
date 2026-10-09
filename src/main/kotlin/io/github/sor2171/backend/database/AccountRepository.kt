package io.github.sor2171.backend.database

import io.github.sor2171.backend.database.DatabaseFactory.dbQuery
import io.github.sor2171.backend.entity.dto.Account
import org.jetbrains.exposed.sql.*

class AccountRepository {

    private fun toAccount(row: ResultRow): Account =
        Account(
            id = row[Accounts.id],
            username = row[Accounts.username],
            password = row[Accounts.password],
            email = row[Accounts.email],
            role = row[Accounts.role],
            registerTime = row[Accounts.registerTime]
        )

    suspend fun findByName(username: String): Account? = dbQuery {
        Accounts.selectAll()
            .where { Accounts.username eq username }
            .map(::toAccount)
            .singleOrNull()
    }

    suspend fun findByEmail(email: String): Account? = dbQuery {
        Accounts.selectAll()
            .where { Accounts.email eq email }
            .map(::toAccount)
            .singleOrNull()
    }

    suspend fun findByNameOrEmail(text: String): Account? = dbQuery {
        Accounts.selectAll()
            .where { (Accounts.username eq text) or (Accounts.email eq text) }
            .map(::toAccount)
            .singleOrNull()
    }

    suspend fun existByName(username: String): Boolean = dbQuery {
        Accounts.selectAll()
            .where { Accounts.username eq username }
            .count() > 0
    }

    suspend fun existByEmail(email: String): Boolean = dbQuery {
        Accounts.selectAll()
            .where { Accounts.email eq email }
            .count() > 0
    }

    suspend fun save(account: Account): Int = dbQuery {
        Accounts.insert {
            it[username] = account.username
            it[password] = account.password
            it[email] = account.email
            it[role] = account.role
            it[registerTime] = account.registerTime
        }[Accounts.id]
    }

    suspend fun resetPasswordByEmail(email: String, encodedPassword: String): Boolean = dbQuery {
        val updatedRows = Accounts.update({ Accounts.email eq email }) {
            it[password] = encodedPassword
        }
        updatedRows > 0
    }
}
