package io.github.sor2171.backend.utils

import io.github.sor2171.backend.entity.ApiResponse

object Const {
    const val JWT_BLACK_LIST = "jwt:blacklist:"
    const val VERIFY_EMAIL_LIMIT = "verify:email:limit:"
    const val VERIFY_EMAIL_DATA = "verify:email:data:"

    const val FLOW_LIMIT_COUNTER = "flow:counter:"
    const val FLOW_LIMIT_BLOCK = "flow:block:"

    const val WS_REQUEST_TOKEN = "ws:token:"

    const val INTERNAL_ERROR_STR = "something went wrong. Please contact the administrator."

    // for Routes / Controllers
    fun messageHandler(wrongMessage: String): ApiResponse<Any?> {
        return if (wrongMessage.isBlank()) {
            ApiResponse.success()
        } else {
            ApiResponse.failure(400, null, wrongMessage)
        }
    }
}
