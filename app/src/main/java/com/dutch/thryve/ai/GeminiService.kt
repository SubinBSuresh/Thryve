package com.dutch.thryve.ai

import com.dutch.thryve.BuildConfig
import com.dutch.thryve.domain.model.MealLog
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID


private const val API_KEY = BuildConfig.GEMINI_API_KEY
private const val GEMINI_MODEL = "gemini-1.5-flash-preview-0514"
private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent?key=$API_KEY"

// --- @SERIALIZABLE DATA CLASSES FOR API REQUEST PAYLOAD ---

@Serializable
data class Property(
    val type: String,
    val description: String
)

@Serializable
data class AnalysisSchema(
    val type: String,
    val properties: Map<String, Property>,
    val required: List<String>
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String,
    val responseSchema: AnalysisSchema
)

@Serializable
data class SystemInstruction(val parts: List<Part>)

@Serializable
data class Tool(val google_search: Map<String, String> = emptyMap())

@Serializable
data class Part(val text: String)

@Serializable
data class Content(val parts: List<Part>)

// The top-level request object
@Serializable
data class GeminiRequest(
    val contents: List<Content>,
    val tools: List<Tool>,
    val systemInstruction: SystemInstruction,
    val generationConfig: GenerationConfig
)
// --- END API PAYLOAD CLASSES ---


// --- @SERIALIZABLE DATA CLASSES FOR API RESPONSE PROCESSING ---

/**
 * Represents the final structured JSON output from the AI, containing the nutritional data.
 */
@Serializable
data class AnalysisResult(
    val description: String,
    val calories: Int,
    val protein: Int,
    val fat: Int,
    val carbs: Int
)

/**
 * These classes mirror the complex, nested JSON structure of the API's full response.
 * We need these to safely and correctly extract the 'text' which contains the AnalysisResult JSON string.
 */
@Serializable
data class PartResponse(val text: String)

@Serializable
data class ContentResponse(val parts: List<PartResponse>, val role: String)

@Serializable
data class CandidateResponse(val content: ContentResponse)

@Serializable
data class GeminiResponse(val candidates: List<CandidateResponse>)

class GeminiService @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        coerceInputValues = true
    }

    private val analysisSchema = AnalysisSchema(
        type = "OBJECT",
        properties = mapOf(
            "description" to Property("STRING", "A clean, concise description of the meal."),
            "calories" to Property("INTEGER", "Total estimated calories in kcal."),
            "protein" to Property("INTEGER", "Total estimated protein in grams."),
            "fat" to Property("INTEGER", "Total estimated fat in grams."),
            "carbs" to Property("INTEGER", "Total estimated carbohydrates in grams.")
        ),
        required = listOf("description", "calories", "protein", "fat", "carbs")
    )

    private val systemPrompt = """
        You are a highly accurate nutritional analysis assistant. 
        Your task is to estimate the macronutrient and calorie content of a user's food description. 
        Analyze the ingredients and portion sizes provided. If portion sizes are vague, make a reasonable, average-sized estimate. 
        You MUST respond only with a single JSON object that conforms to the provided schema. Do not include any conversational text, explanations, or code formatting.
    """.trimIndent()

    suspend fun analyzeMeal(mealDescription: String, date: LocalDate): MealLog? {
        if (API_KEY.isBlank()) {
            System.err.println("FATAL ERROR: The Gemini API Key is missing. Please set the API_KEY constant in DefaultGeminiService.kt.")
            return null
        }

        val userQuery = "Analyze the nutritional content of the following meal description: $mealDescription"

        // constructing api payload
        val payload = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = userQuery)))),
            tools = listOf(Tool(google_search = emptyMap())),
            systemInstruction = SystemInstruction(parts = listOf(Part(text = systemPrompt))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = analysisSchema
            )
        )

        val payloadJson = json.encodeToString(payload)

        //nw call
        val responseText = try {
            makePostRequest(API_URL, payloadJson)
        } catch (e: Exception) {
            System.err.println("Network error or API call failed: ${e.message}")
            e.printStackTrace()
            return null
        }

        //process response
        return try {

            // 1. Decode the full API response into our strongly-typed object
            val geminiResponse = json.decodeFromString<GeminiResponse>(responseText)

            // 2. Safely navigate the object structure to extract the raw JSON string
            val jsonText = geminiResponse.candidates
                .firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text

            if (jsonText.isNullOrBlank()) {
                System.err.println("Gemini returned empty or invalid JSON text.")
                return null
            }

            // 3. Parse the structured JSON response into our final AnalysisResult object
            val analysisResult = json.decodeFromString<AnalysisResult>(jsonText)
            val timestamp = Timestamp(date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0)

            MealLog(
                id = UUID.randomUUID().toString(),
                userId = "", // Will be set by the ViewModel
                date = timestamp,
                description = analysisResult.description,
                calories = analysisResult.calories,
                protein = analysisResult.protein,
                fat = analysisResult.fat,
                carbs = analysisResult.carbs
            )
        } catch (e: Exception) {
            System.err.println("Error parsing Gemini response: ${e.message}. Raw: $responseText")
            e.printStackTrace()
            return null
        }
    }
}

/**
 * Performs an asynchronous HTTP POST request using HttpURLConnection on the Dispatchers.IO thread.
 * @throws Exception for networking or API errors.
 */
private suspend fun makePostRequest(url: String, body: String): String = withContext(Dispatchers.IO) {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.requestMethod = "POST"
    connection.setRequestProperty("Content-Type", "application/json")
    connection.doOutput = true
    connection.connectTimeout = 10000 // 10 seconds
    connection.readTimeout = 10000 // 10 seconds

    try {
        // Write the request body
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

        // Read the response body
        val responseBody = BufferedReader(InputStreamReader(responseStream)).use { br ->
            br.readText()
        }

        if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            val errorMessage = "HTTP Error $responseCode: $responseBody"
            System.err.println(errorMessage) // Log the error body
            throw Exception(errorMessage)
        }

        return@withContext responseBody
    } finally {
        // Always disconnect the connection
        connection.disconnect()
    }
}
