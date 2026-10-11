package io.github.sor2171.backend.rpc

import io.github.sor2171.backend.entity.ApiResponse
import io.github.sor2171.backend.entity.enums.EmailType
import io.github.sor2171.backend.entity.vo.request.EmailRegisterVO
import io.github.sor2171.backend.entity.vo.request.LoginVO
import io.github.sor2171.backend.entity.vo.request.PasswordResetVO
import io.github.sor2171.backend.entity.vo.response.AuthorizeVO
import io.github.sor2171.backend.entity.vo.response.ReloginVO
import kotlinx.rpc.annotations.Rpc

@Rpc
interface AuthRpcService {
    suspend fun login(loginVO: LoginVO): ApiResponse<AuthorizeVO?>
    suspend fun askCode(email: String, type: EmailType, clientIp: String = "127.0.0.1"): ApiResponse<String?>
    suspend fun register(registerVO: EmailRegisterVO): ApiResponse<String?>
    suspend fun resetPassword(resetVO: PasswordResetVO): ApiResponse<String?>
    suspend fun logout(token: String): ApiResponse<String?>
    suspend fun relogin(token: String): ApiResponse<ReloginVO?>
}
