package com.back.global.igdb.util

object IgdbImageUtil {
    private const val BASE = "https://images.igdb.com/igdb/image/upload/"

    @JvmStatic
    fun cover(imageId: String?): String? {
        if (imageId.isNullOrBlank()) return null
        return "${BASE}t_cover_big/$imageId.jpg"
    }
}
