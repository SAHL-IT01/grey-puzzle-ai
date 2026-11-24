package com.grey.puzzleai

import android.content.Context

data class BoardConfig(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val rows: Int,
    val cols: Int
)

object AppPrefs {
    private const val PREF_NAME = "grey_puzzle_prefs"

    fun save(context: Context, apiKey: String, model: String, boardRaw: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString("api_key", apiKey.trim())
            .putString("model", model.trim().ifEmpty { "gemini-2.5-flash" })
            .putString("board_raw", boardRaw.trim().ifEmpty { "0.02,0.14,0.98,0.78,8,8" })
            .apply()
    }

    fun getApiKey(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString("api_key", "") ?: ""

    fun getModel(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString("model", "gemini-2.5-flash") ?: "gemini-2.5-flash"

    fun getBoardRaw(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString("board_raw", "0.02,0.14,0.98,0.78,8,8") ?: "0.02,0.14,0.98,0.78,8,8"

    fun getBoardConfig(context: Context): BoardConfig {
        val parts = getBoardRaw(context).split(",").map { it.trim() }
        return try {
            BoardConfig(
                x1 = parts[0].toFloat(),
                y1 = parts[1].toFloat(),
                x2 = parts[2].toFloat(),
                y2 = parts[3].toFloat(),
                rows = parts[4].toInt(),
                cols = parts[5].toInt()
            )
        } catch (_: Exception) {
            BoardConfig(0.02f, 0.14f, 0.98f, 0.78f, 8, 8)
        }
    }
}
