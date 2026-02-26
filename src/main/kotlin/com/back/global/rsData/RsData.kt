package com.back.global.rsData

import com.fasterxml.jackson.annotation.JsonIgnore

data class RsData<T>(
    val resultCode: String,
    @JsonIgnore
    val statusCode: Int,
    val msg: String,
    val data: T?,
) {
    // @JvmOverloads를 붙여서 자바에서 인자 2개로도 호출 가능하게 만듭니다.
    @JvmOverloads
    constructor(resultCode: String, msg: String, data: T? = null) : this(
        resultCode,
        resultCode.split("-", limit = 2)[0].toIntOrNull() ?: 200,
        msg,
        data,
    )
}
