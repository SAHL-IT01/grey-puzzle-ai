package com.grey.puzzleai

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class MovePlan(
    val pieceIndex: Int,
    val pieceCenterXNorm: Float,
    val pieceCenterYNorm: Float,
    val targetRow: Int,
    val targetCol: Int,
    val reason: String
)

object GeminiPuzzlePlanner {
    private const val SYSTEM_PROMPT = """
Kamu adalah otak AI perencana gerakan untuk permainan block puzzle berbasis grid di Android.
Analisis tangkapan layar game (papan grid di atas/tengah dan kepingan di bawah).
Pilih satu kepingan valid yang muat di sel (target_row, target_col, berbasis 0) tanpa tumpang tindih dengan blok yang sudah ada.
Prioritaskan membersihkan baris/kolom penuh, mengambil ikon permata bila ada, dan menjaga papan tetap lega.
Balas HANYA JSON valid tanpa markdown fence:
{"piece_index":0,"piece_center_norm":[0.5,0.87],"target_row":0,"target_col":0,"reason":"alasan singkat"}
"""

    suspend fun planNextMove(
        apiKey: String,
        model: String,
        pngBytes: ByteArray,
        board: BoardConfig
    ): MovePlan = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "API Key Gemini belum diisi." }

        val cleanModel = model.removePrefix("models/")
        val endpoint = URL("https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey")
        val b64Image = Base64.encodeToString(pngBytes, Base64.NO_WRAP)

        val userPrompt = "Kalibrasi papan: x1=${board.x1}, y1=${board.y1}, x2=${board.x2}, y2=${board.y2}, rows=${board.rows}, cols=${board.cols}. Berikan 1 langkah terbaik berikutnya dalam JSON murni."

        val bodyJson = JSONObject().apply {
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT))))
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().apply {
                    put(JSONObject().put("inlineData", JSONObject().apply {
                        put("mimeType", "image/png")
                        put("data", b64Image)
                    }))
                    put(JSONObject().put("text", userPrompt))
                })
            ))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("responseMimeType", "application/json")
            })
        }

        val conn = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            doOutput = true
            connectTimeout = 15000
            readTimeout = 25000
        }

        conn.outputStream.use { os ->
            os.write(bodyJson.toString().toByteArray(Charsets.UTF_8))
        }

        val code = conn.responseCode
        val rawResp = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""

        if (code !in 200..299) {
            throw RuntimeException("HTTP $code: $rawResp")
        }

        val root = JSONObject(rawResp)
        val text = root.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val moveObj = JSONObject(text)
        val center = moveObj.getJSONArray("piece_center_norm")

        MovePlan(
            pieceIndex = moveObj.optInt("piece_index", 0),
            pieceCenterXNorm = center.getDouble(0).toFloat(),
            pieceCenterYNorm = center.getDouble(1).toFloat(),
            targetRow = moveObj.getInt("target_row"),
            targetCol = moveObj.getInt("target_col"),
            reason = moveObj.optString("reason", "")
        )
    }
}
