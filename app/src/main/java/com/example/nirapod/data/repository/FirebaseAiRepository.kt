package com.example.nirapod.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.nirapod.BuildConfig
import com.example.nirapod.data.model.AiAnalysis
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import org.json.JSONObject

class FirebaseAiRepository(
    private val context: Context
) : AiRepository {
    override suspend fun analyze(imageUri: Uri?, description: String): Result<AiAnalysis> = runCatching {
        require(description.isNotBlank() || imageUri != null) { "Add an image or description before AI analysis" }
        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(BuildConfig.GEMINI_MODEL)

        val bitmap = imageUri?.let(::decodeBitmap)
        val prompt = content {
            bitmap?.let { image(it) }
            text(
                """
                You are assisting a Bangladesh public-safety reporting prototype.
                Analyze the attached hazard image and/or the user's description.
                User description: ${description.take(1200)}

                Return JSON only, without markdown fences, in exactly this shape:
                {
                  "category":"Open Manhole|Damaged Road|Broken Drain|Waterlogging|Electrical Hazard|Crime Hotspot|Fire Hazard|Other Hazard",
                  "severity":"LOW|MEDIUM|HIGH|CRITICAL",
                  "summary":"one concise authority-friendly report summary",
                  "risk":"one concise risk statement",
                  "suggestedAuthority":"City Corporation|Police / Safety Authority|Fire Service|Utility Authority|Disaster Management"
                }
                Do not claim certainty. The user and authority must verify the result.
                """.trimIndent()
            )
        }
        val raw = model.generateContent(prompt).text ?: error("AI returned no text")
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        require(start >= 0 && end > start) { "AI response was not valid JSON" }
        val json = JSONObject(raw.substring(start, end + 1))
        AiAnalysis(
            category = json.optString("category", "Other Hazard"),
            severity = json.optString("severity", "MEDIUM"),
            summary = json.optString("summary", "AI analysis available for human review."),
            risk = json.optString("risk", "Potential public-safety risk"),
            suggestedAuthority = json.optString("suggestedAuthority", "City Corporation")
        )
    }

    private fun decodeBitmap(uri: Uri): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val max = 1280
        var sample = 1
        while (bounds.outWidth / sample > max || bounds.outHeight / sample > max) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Unable to decode image")
    }
}
