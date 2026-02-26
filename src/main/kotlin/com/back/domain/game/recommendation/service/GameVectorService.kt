package com.back.domain.game.recommendation.service

import com.back.domain.game.game.entity.*
import com.back.global.vector.VectorDimensionMapper
import org.springframework.stereotype.Service

@Service
class GameVectorService(
    private val vectorDimensionMapper: VectorDimensionMapper,
) {
    /**
     * 메모리에 이미 로드된 속성 리스트로 벡터 생성 (배치 Writer용)
     */
    fun buildFeatureVector(
        genres: List<Genre>,
        themes: List<Theme>,
        keywords: List<Keyword>,
        gameModes: List<GameMode>,
        playerPerspectives: List<PlayerPerspective>,
    ): FloatArray {
        val vector = FloatArray(VectorDimensionMapper.TOTAL_DIMENSIONS)

        for (g in genres) {
            val idx = vectorDimensionMapper.getGenreIndex(g.id)
            if (idx != null) vector[VectorDimensionMapper.GENRE_OFFSET + idx] = 1.0f
        }
        for (t in themes) {
            val idx = vectorDimensionMapper.getThemeIndex(t.id)
            if (idx != null) vector[VectorDimensionMapper.THEME_OFFSET + idx] = 1.0f
        }
        for (k in keywords) {
            val idx = vectorDimensionMapper.getKeywordIndex(k.id)
            if (idx != null) vector[VectorDimensionMapper.KEYWORD_OFFSET + idx] = 1.0f
        }
        for (gm in gameModes) {
            val idx = vectorDimensionMapper.getModeIndex(gm.id)
            if (idx != null) vector[VectorDimensionMapper.MODE_OFFSET + idx] = 1.0f
        }
        for (pp in playerPerspectives) {
            val idx = vectorDimensionMapper.getPerspectiveIndex(pp.id)
            if (idx != null) vector[VectorDimensionMapper.PERSPECTIVE_OFFSET + idx] = 1.0f
        }
        return vector
    }

    companion object {
        @JvmStatic
        fun vectorToString(vector: FloatArray): String {
            val sb = StringBuilder("[")
            for (i in vector.indices) {
                if (i > 0) sb.append(",")
                sb.append(vector[i])
            }
            sb.append("]")
            return sb.toString()
        }
    }
}
