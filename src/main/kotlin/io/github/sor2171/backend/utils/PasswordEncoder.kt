package io.github.sor2171.backend.utils

import at.favre.lib.crypto.bcrypt.BCrypt

object PasswordEncoder {
    fun encode(raw: String): String {
        return BCrypt.withDefaults().hashToString(10, raw.toCharArray())
    }

    fun matches(raw: String, hashed: String): Boolean {
        val result = BCrypt.verifyer().verify(raw.toCharArray(), hashed.toCharArray())
        return result.verified
    }
}
