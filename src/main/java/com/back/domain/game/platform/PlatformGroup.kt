package com.back.domain.game.platform

import java.util.*
import java.util.Map

object PlatformGroup {
    /**
     * 대표 플랫폼 코드
     */
    val PLATFORM_MAP: MutableMap<String?, MutableList<Long?>?> = Map.of<String?, MutableList<Long?>?>(
        "PS", mutableListOf<Long?>(48L, 167L, 9L, 8L, 7L),
        "XBOX", mutableListOf<Long?>(49L, 169L, 11L),
        "NINTENDO", mutableListOf<Long?>(130L, 41L, 37L, 18L, 33L),
        "PC", mutableListOf<Long?>(6L),
        "MOBILE", mutableListOf<Long?>(34L, 39L),
        "VR", mutableListOf<Long?>(162L, 163L)
    )

    /**
     * 드롭다운용
     */
    val DISPLAY_NAME: MutableMap<String?, String?> = Map.of<String?, String?>(
        "PS", "PlayStation",
        "XBOX", "Xbox",
        "NINTENDO", "Nintendo",
        "PC", "PC",
        "MOBILE", "Mobile",
        "VR", "VR"
    )


    @JvmStatic
    fun getDefaultPlatformId(groupName: String?): Long? {
        if (groupName == null) return null
        val ids = PLATFORM_MAP.get(groupName.uppercase(Locale.getDefault()))
        return if (ids != null && !ids.isEmpty()) ids.get(0) else null
    }


    @JvmStatic
    fun getPlatformIds(groupName: String?): MutableList<Long?> {
        if (groupName == null) return mutableListOf<Long?>()
        val ids = PLATFORM_MAP.get(groupName.uppercase(Locale.getDefault()))
        return if (ids != null) ids else mutableListOf<Long?>()
    }


    @JvmStatic
    fun getGroupName(platformId: Long?): String? {
        if (platformId == null) return null
        for (entry in PLATFORM_MAP.entries) {
            if (entry.value!!.contains(platformId)) {
                return entry.key
            }
        }
        return null
    }

    val allGroupNames: MutableSet<String?>
        get() = PLATFORM_MAP.keys
}
