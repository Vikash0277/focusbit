package com.focusflow.ui.theme

enum class AppTheme(
    val id: String,
    val displayName: String,
    val description: String,
    val isDark: Boolean? = false
) {
    LAVENDER_DREAM(
        id = "lavender_dream",
        displayName = "Lavender Dream",
        description = "Modern vibrant violet & soft lavender (Default)",
        isDark = false
    ),
    MIDNIGHT(
        id = "midnight",
        displayName = "Midnight",
        description = "Deep dark mode with rich slate and neon lavender",
        isDark = true
    ),
    OCEAN_BREEZE(
        id = "ocean_breeze",
        displayName = "Ocean Breeze",
        description = "Fresh teal, crisp sea-foam, and calm aquatic tones",
        isDark = false
    ),
    FOREST_CALM(
        id = "forest_calm",
        displayName = "Forest Calm",
        description = "Natural sage, earthy moss, and tranquil organic tones",
        isDark = false
    ),
    SYSTEM_DEFAULT(
        id = "system_default",
        displayName = "System Default",
        description = "Follows your device's light or dark mode setting",
        isDark = null
    );

    companion object {
        fun fromId(id: String?): AppTheme {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: LAVENDER_DREAM
        }
    }
}

