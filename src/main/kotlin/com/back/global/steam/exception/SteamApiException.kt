package com.back.global.steam.exception

class SteamApiException(
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)
