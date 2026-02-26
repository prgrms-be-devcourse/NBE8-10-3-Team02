package com.back.global.igdb.exception

class IgdbApiException(
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)
