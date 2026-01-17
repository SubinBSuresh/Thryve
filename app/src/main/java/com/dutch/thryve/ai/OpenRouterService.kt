package com.dutch.thryve.ai

import android.util.Log
import com.dutch.thryve.BuildConfig
import com.dutch.thryve.domain.model.MealLog
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

private const val API_KEY = BuildConfig.OPENROUTER_SECRET_KEY // Reusing this key for OpenRouter
private const val API_URL = "https://openrouter.ai/api/v1/chat/completions"
private const val MODEL = "mistralai/devstral-2512:free" // Or deepseek/deepseek-chat

@Serializable
data class OpenRouterMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val response_format: ResponseFormat? = null
)

@Serializable
data class ResponseFormat(val type: String)

@Serializable
data class OpenRouterChoice(
    val message: OpenRouterMessage
)

@Serializable
data class OpenRouterResponse(
    val choices: List<OpenRouterChoice>
)

@Serializable
data class AnalysisResult(
    val description: String,
    val calories: Int,
    val protein: Int,
    val fat: Int,
    val carbs: Int
)

class OpenRouterService @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        coerceInputValues = true
    }

    private val systemPrompt = """
        You are a highly accurate nutritional analysis assistant. 
        Your task is to estimate the macronutrient and calorie content of a user's food description. 
        Analyze the ingredients and portion sizes provided. If portion sizes are vague, make a reasonable, average-sized estimate. 
        You MUST respond only with a single JSON object with the following fields:
        - description: A clean, concise description of the meal.
        - calories: Total estimated calories in kcal (integer).
        - protein: Total estimated protein in grams (integer).
        - fat: Total estimated fat in grams (integer).
        - carbs: Total estimated carbohydrates in grams (integer).
        Do not include any conversational text, explanations, or code formatting (no markdown blocks).
    """.trimIndent()

    suspend fun analyzeMeal(mealDescription: String, date: LocalDate): MealLog? {
        if (API_KEY.isBlank()) {
            Log.e("OpenRouterService", "API Key is missing")
            return null
        }

        val userQuery = "Analyze: $mealDescription"

        val payload = OpenRouterRequest(
            model = MODEL,
            messages = listOf(
                OpenRouterMessage("system", systemPrompt),
                OpenRouterMessage("user", userQuery)
            ),
            response_format = ResponseFormat("json_object")
        )

        val payloadJson = json.encodeToString(payload)

        val responseText = try {
            makePostRequest(API_URL, payloadJson, API_KEY)
        } catch (e: Exception) {
            Log.e("OpenRouterService", "API call failed", e)
            return null
        }

        return try {
            val response = json.decodeFromString<OpenRouterResponse>(responseText)
            val jsonText = response.choices.firstOrNull()?.message?.content

            if (jsonText.isNullOrBlank()) return null

            val analysisResult = json.decodeFromString<AnalysisResult>(jsonText)
            val timestamp = Timestamp(date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0)

            MealLog(
                id = UUID.randomUUID().toString(),
                userId = "",
                date = timestamp,
                description = analysisResult.description,
                calories = analysisResult.calories,
                protein = analysisResult.protein,
                fat = analysisResult.fat,
                carbs = analysisResult.carbs
            )
        } catch (e: Exception) {
            Log.e("OpenRouterService", "Error parsing response. Raw: $responseText", e)
            null
        }
    }
}

private suspend fun makePostRequest(url: String, body: String, apiKey: String): String = withContext(Dispatchers.IO) {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.requestMethod = "POST"
    connection.setRequestProperty("Content-Type", "application/json")
    connection.setRequestProperty("Authorization", "Bearer $apiKey")
    connection.setRequestProperty("HTTP-Referer", "https://thryve.app") // OpenRouter requirement
    connection.setRequestProperty("X-Title", "Thryve")
    connection.doOutput = true
    connection.connectTimeout = 30000
    connection.readTimeout = 30000

    try {
        connection.outputStream.use { os ->
            os.write(body.toByteArray())
            os.flush()
        }

        val responseCode = connection.responseCode
        val responseStream = if (responseCode < HttpURLConnection.HTTP_BAD_REQUEST) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val responseBody = BufferedReader(InputStreamReader(responseStream)).use { br ->
            br.readText()
        }

        if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            throw Exception("HTTP Error $responseCode: $responseBody")
        }

        return@withContext responseBody
    } finally {
        connection.disconnect()
    }
}
