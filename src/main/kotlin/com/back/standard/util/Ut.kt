package com.back.standard.util

import com.fasterxml.jackson.databind.ObjectMapper

object Ut {
    object Json {
        @JvmField
        var objectMapper: ObjectMapper? = null

        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        @JvmStatic
        @JvmOverloads
        fun toString(
            obj: Any?,
            defaultValue: String? = null,
        ): String? =
            try {
                objectMapper?.writeValueAsString(obj)
            } catch (e: Exception) {
                defaultValue
            }
    }
}
