package com.back.domain.game.game.dto

data class GameVideoResponse(
    val videoId: String,
    val trailerEmbedUrl: String,
) {
    companion object {
        private const val VIDEO_URL_TEMPLATE = "https://www.youtube.com/embed/"

        @JvmStatic
        fun from(videoId: String?): GameVideoResponse {
            val vId = videoId ?: ""
            val url = if (vId.isBlank()) "" else VIDEO_URL_TEMPLATE + vId
            return GameVideoResponse(vId, url)
        }
    }
}
