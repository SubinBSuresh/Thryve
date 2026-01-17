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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

// ----------------------------
// CONSTANTS
// ----------------------------

private const val API_KEY = BuildConfig.OPENAI_SECRET_KEY
private const val MODEL = "gpt-4o-mini"
private const val API_URL = "https://api.openai.com/v1/responses"

// ----------------------------
// REQUEST PAYLOAD
// ----------------------------

@Serializable
data class OpenAIMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val response_format: OpenAIResponseSchema
)

@Serializable
data class OpenAIResponseSchema(
    val type: String = "json_schema",
    val json_schema: OpenAIResponseSchemaDef
)

@Serializable
data class OpenAIResponseSchemaDef(
    val name: String = "meal_analysis",
    val schema: OpenAIAnalysisSchema
)

@Serializable
data class OpenAIAnalysisSchema(
    val type: String = "object",
    val properties: Map<String, GeminiProperty>,
    val required: List<String>
)

@Serializable
data class Property(
    val type: String,
    val description: String
)

// ----------------------------
// RESPONSE PARSING
// ----------------------------

@Serializable
data class OpenAIResponse(
    val output: List<OutputItem>
)

@Serializable
data class OutputItem(
    val content: String
)

@Serializable
data class OpenAIAnalysisResult(
    val description: String,
    val calories: Int,
    val protein: Int,
    val fat: Int,
    val carbs: Int
)

class OpenAIService @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val client = OkHttpClient()

    // JSON schema for OpenAI
    private val schema = OpenAIAnalysisSchema(
        properties = mapOf(
            "description" to GeminiProperty("string", "clean description"),
            "calories" to GeminiProperty("integer", "calories"),
            "protein" to GeminiProperty("integer", "protein g"),
            "fat" to GeminiProperty("integer", "fat g"),
            "carbs" to GeminiProperty("integer", "carbs g")
        ),
        required = listOf("description", "calories", "protein", "fat", "carbs")
    )

    private val systemPrompt = """
        You estimate nutritional values. 
        Respond ONLY with JSON following the provided schema.
        NO extra text, no code blocks.
    """.trimIndent()

    suspend fun analyzeMeal(meal: String, date: LocalDate): MealLog? {
        if (API_KEY.isBlank()) {
            Log.e("OpenAIService", "Missing OpenAI API Key")
            return null
        }

        val payload = OpenAIRequest(
            model = MODEL,
            messages = listOf(
                OpenAIMessage("system", systemPrompt),
                OpenAIMessage("user", "Analyze: $meal")
            ),
            response_format = OpenAIResponseSchema(
                json_schema = OpenAIResponseSchemaDef(schema = schema)
            )
        )

        val body = json.encodeToString(payload)
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(API_URL)
            .post(body)
            .addHeader("Authorization", "Bearer $API_KEY")
            .build()

        val raw = withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.e("OpenAIService", "HTTP ${resp.code}: ${resp.body?.string()}")
                    return@withContext null
                }
                resp.body?.string()
            }
        } ?: return null

        return try {
            val parsed = json.decodeFromString<OpenAIResponse>(raw)
            val obj = parsed.output.first().content

            val result = json.decodeFromString<GeminiAnalysisResult>(obj)

            val timestamp = Timestamp(
                date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(),
                0
            )

            MealLog(
                id = UUID.randomUUID().toString(),
                userId = "",
                date = timestamp,
                description = result.description,
                calories = result.calories,
                protein = result.protein,
                fat = result.fat,
                carbs = result.carbs
            )
        } catch (e: Exception) {
            Log.e("OpenAIService", "Parsing error: ${e.message}")
            null
        }
    }
}
