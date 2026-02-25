package com.back.domain.game.platform

import java.util.Locale

object PlatformGroup {
    /**
     * 대표 플랫폼 코드
     */
    val PLATFORM_MAP: MutableMap<String?, MutableList<Long?>?> =
        mutableMapOf( // Map.of 대신 mutableMapOf 사용
            "PS" to mutableListOf(48L, 167L, 9L, 8L, 7L),
            "XBOX" to mutableListOf(49L, 169L, 11L),
            "NINTENDO" to mutableListOf(130L, 41L, 37L, 18L, 33L),
            "PC" to mutableListOf(6L),
            "MOBILE" to mutableListOf(34L, 39L),
            "VR" to mutableListOf(162L, 163L),
        )

    /**
     * 드롭다운용
     */
    val DISPLAY_NAME: MutableMap<String?, String?> =
        mutableMapOf( // Map.of 대신 mutableMapOf 사용
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
        val ids = PLATFORM_MAP[groupName.uppercase(Locale.getDefault())] // get() 대신 [] 사용
        return if (ids != null && ids.isNotEmpty()) ids[0] else null
    }

    @JvmStatic
    fun getPlatformIds(groupName: String?): MutableList<Long?> {
        if (groupName == null) return mutableListOf()
        val ids = PLATFORM_MAP[groupName.uppercase(Locale.getDefault())]
        return ids ?: mutableListOf()
    }

    @JvmStatic
    fun getGroupName(platformId: Long?): String? {
        if (platformId == null) return null
        for ((key, value) in PLATFORM_MAP) { // entries 반복문 최적화
            if (value?.contains(platformId) == true) {
                return key
            }
        }
        return null
    }

    val allGroupNames: MutableSet<String?>
        get() = PLATFORM_MAP.keys
}
