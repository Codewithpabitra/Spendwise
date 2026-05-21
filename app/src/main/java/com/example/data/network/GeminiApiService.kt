package com.example.data.network

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini API Request Models ---

data class Part(
    val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

data class InlineData(
    val mimeType: String,
    val data: String // Base64 encoded string
)

data class Content(
    val parts: List<Part>,
    val role: String? = null
)

data class GenerationConfig(
    val temperature: Float? = 0.2f,
    val responseMimeType: String? = "application/json"
)

data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

// --- Gemini API Response Models ---

data class Entry(
    val text: String
)

data class CandidatePart(
    val text: String? = null
)

data class CandidateContent(
    val parts: List<CandidatePart>?,
    val role: String? = null
)

data class Candidate(
    val content: CandidateContent?
)

data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

// --- Retrofit Interface ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- Gemini Sync Result Parsers ---

data class ParsedExpenseResult(
    val title: String,
    val amount: Double,
    val category: String
)

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Parse natural language input into a structured transaction.
     * Example input: "Spent ₹350 on subway sandwiches yesterday"
     */
    suspend fun parseExpenseInput(text: String): ParsedExpenseResult? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY_DEFAULT_VALUE") {
            Log.e(TAG, "Gemini API key is not configured")
            return null
        }

        val prompt = """
            You are SpendWise NLP Engine. Parse the following sentence: "$text"
            Return a JSON object containing the fields: 'title' (String, default is "Expense"), 'amount' (Double, default is 0.0), and 'category' (String, must fit one of the allowed values).
            
            Allowed expense categories (strictly match one of the following):
            - Food
            - Transport
            - Shopping
            - Rent
            - Entertainment
            - Health
            - Bills
            - Education
            - Travel
            - Other
            
            Return ONLY a raw JSON object string of the format:
            {
              "title": "Subway Sandwiches",
              "amount": 350.0,
              "category": "Food"
            }
            Do not include markdown blocks like ```json or any explainers. Return ONLY raw JSON.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.1f
            )
        )

        return try {
            val response = service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            Log.d(TAG, "NLP Raw output: $jsonText")
            
            val cleanJson = cleanJsonString(jsonText)
            val adapter = moshi.adapter(Map::class.java)
            val map = adapter.fromJson(cleanJson) ?: return null
            
            val title = (map["title"] as? String) ?: "Expense"
            val amount = when (val amt = map["amount"]) {
                is Double -> amt
                is Float -> amt.toDouble()
                is Number -> amt.toDouble()
                is String -> amt.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            val category = (map["category"] as? String) ?: "Other"
            
            ParsedExpenseResult(title, amount, category)
        } catch (e: Exception) {
            Log.e(TAG, "Error in parseExpenseInput", e)
            null
        }
    }

    /**
     * Chat with the Gemini AI Coach.
     */
    suspend fun getCoachResponse(
        userMessage: String,
        recentExpenses: String, // Text summary of expenses to provide context
        chatHistory: List<Content>
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY_DEFAULT_VALUE") {
            return "API Key is missing. Please set up GEMINI_API_KEY in the Secrets panel."
        }

        val systemPrompt = """
            You are 'SpendWise AI Financial Coach', a friendly, certified financial planning expert.
            Your style is encouraging, highly practical, smart, and educational.
            You help college students, salaried employees, freelancers, couples, and families manage their money wisely.
            
            CONSTRAINTS:
            - Provide clear, actionable tips. Mention realistic habits (saving, emergency funds, cutting online delivery).
            - Keep responses highly formatting-rich (bolding, short bullet points). Keep paragraphs minimal for mobile readability.
            - Answer questions comprehensively but compactly.
            
            REAL-TIME DATA CONTEXT:
            Below are the user's recent recorded expenses for you to reference if relevant:
            $recentExpenses
        """.trimIndent()

        // Combine history with latest message as standard list
        val contents = chatHistory + Content(parts = listOf(Part(text = userMessage)), role = "user")

        val request = GenerateContentRequest(
            contents = contents,
            generationConfig = GenerationConfig(
                responseMimeType = "text/plain",
                temperature = 0.7f
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "I apologize, but I struggled to analyze that request. Could you rephrase?"
        } catch (e: Exception) {
            Log.e(TAG, "Error in getCoachResponse", e)
            "Coach Error: ${e.localizedMessage ?: "Could not reach AI Server"}"
        }
    }

    /**
     * Real receipt OCR analysis. Send Base64 image payload to Gemini and parse receipt into a structured transaction.
     */
    suspend fun scanReceiptImage(base64Image: String): ParsedExpenseResult? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY_DEFAULT_VALUE") {
            return null
        }

        val instruction = """
            Analyze this uploaded receipt.
            Perform OCR to extract:
            1. Total / Grand Total amount (Double)
            2. Merchant / Store/ Restaurant Name (String, use for title)
            3. Appropriate category from the following allowed values: Food, Transport, Shopping, Rent, Entertainment, Health, Bills, Education, Travel, Other.
            
            Return ONLY a raw JSON block with fields 'title', 'amount', 'category'. No markdown wrappers.
            Example:
            {"title": "Walmart Store", "amount": 47.92, "category": "Shopping"}
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = instruction),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                    )
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.1f
            )
        )

        return try {
            val response = service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            Log.d(TAG, "OCR Raw output: $jsonText")
            
            val cleanJson = cleanJsonString(jsonText)
            val adapter = moshi.adapter(Map::class.java)
            val map = adapter.fromJson(cleanJson) ?: return null
            
            val title = (map["title"] as? String) ?: "Receipt Scan"
            val amount = when (val amt = map["amount"]) {
                is Double -> amt
                is Float -> amt.toDouble()
                is Number -> amt.toDouble()
                is String -> amt.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            val category = (map["category"] as? String) ?: "Other"
            
            ParsedExpenseResult(title, amount, category)
        } catch (e: Exception) {
            Log.e(TAG, "Error scanReceiptImage", e)
            null
        }
    }

    private fun cleanJsonString(rawText: String): String {
        return rawText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }
}
