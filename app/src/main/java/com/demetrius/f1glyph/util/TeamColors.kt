package com.demetrius.f1glyph.util

/** Ergast constructor id → team color. Unknown teams get neutral grey. */
object TeamColors {

    const val FALLBACK = 0xFF8A8A8A.toInt()

    private val byConstructor = mapOf(
        "red_bull" to 0xFF3671C6.toInt(),
        "ferrari" to 0xFFE8002D.toInt(),
        "mercedes" to 0xFF27F4D2.toInt(),
        "mclaren" to 0xFFFF8000.toInt(),
        "aston_martin" to 0xFF229971.toInt(),
        "alpine" to 0xFF0093CC.toInt(),
        "williams" to 0xFF64C4FF.toInt(),
        "rb" to 0xFF6692FF.toInt(),
        "racing_bulls" to 0xFF6692FF.toInt(),
        "sauber" to 0xFF52E252.toInt(),
        "audi" to 0xFFBB0A30.toInt(),
        "haas" to 0xFFB6BABD.toInt(),
        "cadillac" to 0xFFD4AF37.toInt()
    )

    fun of(constructorId: String): Int =
        byConstructor[constructorId.lowercase()] ?: FALLBACK

    /**
     * Maps an OpenF1 `team_name` (e.g. "Red Bull Racing") to our constructor
     * id. "racing bulls" is checked before "red bull" on purpose.
     */
    fun idFromTeamName(teamName: String): String {
        val n = teamName.lowercase()
        return when {
            "racing bulls" in n || n == "rb" -> "racing_bulls"
            "red bull" in n -> "red_bull"
            "ferrari" in n -> "ferrari"
            "mercedes" in n -> "mercedes"
            "mclaren" in n -> "mclaren"
            "aston" in n -> "aston_martin"
            "alpine" in n -> "alpine"
            "williams" in n -> "williams"
            "audi" in n -> "audi"
            "sauber" in n -> "sauber"
            "haas" in n -> "haas"
            "cadillac" in n -> "cadillac"
            else -> ""
        }
    }
}
