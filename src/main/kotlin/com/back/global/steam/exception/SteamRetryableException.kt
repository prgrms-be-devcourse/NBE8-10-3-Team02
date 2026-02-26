package com.back.global.steam.exception

class SteamRetryableException(
    actionName: String,
    statusCode: Int,
    cause: Throwable,
) : RuntimeException("$actionName 실패, status: $statusCode", cause)
