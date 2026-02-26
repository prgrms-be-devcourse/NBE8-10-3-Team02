package com.back.domain.game.platform

object PlatformGroup {
    /**
     * 대표 플랫폼 코드
     */
    @JvmField
    val PLATFORM_MAP =
        mapOf(
            "PS" to listOf(48L, 167L, 9L, 8L, 7L),
            "XBOX" to listOf(49L, 169L, 11L),
            "NINTENDO" to listOf(130L, 41L, 37L, 18L, 33L),
            "PC" to listOf(6L),
            "MOBILE" to listOf(34L, 39L),
            "VR" to listOf(162L, 163L),
        )

    /**
     * 드롭다운용
     */
    @JvmField
    val DISPLAY_NAME =
        mapOf(
            "PS" to "PlayStation",
            "XBOX" to "Xbox",
            "NINTENDO" to "Nintendo",
            "PC" to "PC",
            "MOBILE" to "Mobile",
            "VR" to "VR",
        )

    @JvmStatic
    fun getDefaultPlatformId(groupName: String?): Long? {
        if (groupName == null) return null
        return PLATFORM_MAP[groupName.uppercase()]?.firstOrNull()
    }

    @JvmStatic
    fun getPlatformIds(groupName: String?): List<Long> {
        if (groupName == null) return emptyList()
        return PLATFORM_MAP[groupName.uppercase()] ?: emptyList()
    }

    @JvmStatic
    fun getGroupName(platformId: Long?): String? {
        if (platformId == null) return null
        return PLATFORM_MAP.entries.firstOrNull { platformId in it.value }?.key
    }

    @JvmStatic
    fun getAllGroupNames(): Set<String> = PLATFORM_MAP.keys
}
