package com.amirali.myplugin.mcpinspectorlite.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.json.*

/**
 * Simplified parameter parser using Jackson conversion
 */
object SimpleParameterParser {
    private val logger = Logger.getInstance(SimpleParameterParser::class.java)
    private val objectMapper = ObjectMapper()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Convert string parameters to properly typed Map using Jackson
     */
    fun parseParameters(
        parameters: Map<String, String>,
        schema: JsonObject?
    ): Map<String, Any> {
        try {
            val jsonObject = buildJsonObject {
                parameters.forEach { (key, value) ->
                    if (value.isNotBlank()) {
                        put(key, inferAndBuildJsonElement(value, schema?.get("properties")?.jsonObject?.get(key)))
                    }
                }
            }

            val node = objectMapper.readTree(jsonObject.toString())

            return objectMapper.convertValue(node, object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any>>() {})
                ?: emptyMap()

        } catch (e: Exception) {
            logger.error("Failed to parse parameters", e)
            throw e
        }
    }

    /**
     * Infer the appropriate JsonElement from a string value
     */
    private fun inferAndBuildJsonElement(value: String, propertySchema: JsonElement?): JsonElement {
        val type = propertySchema?.jsonObject?.get("type")?.jsonPrimitive?.contentOrNull

        return when (type?.lowercase()) {
            "integer" -> {
                val intValue = value.toIntOrNull()
                if (intValue != null) JsonPrimitive(intValue) else JsonPrimitive(value)
            }
            "number" -> {
                val doubleValue = value.toDoubleOrNull()
                if (doubleValue != null) JsonPrimitive(doubleValue) else JsonPrimitive(value)
            }
            "boolean" -> {
                val boolValue = when (value.lowercase()) {
                    "true", "yes", "1" -> true
                    "false", "no", "0" -> false
                    else -> null
                }
                if (boolValue != null) JsonPrimitive(boolValue) else JsonPrimitive(value)
            }
            "array" -> {
                try {
                    json.parseToJsonElement(value)
                } catch (e: Exception) {
                    // Fallback to comma-separated
                    val items = value.split(",").map { it.trim() }
                    buildJsonArray {
                        items.forEach { add(it) }
                    }
                }
            }
            "object" -> {
                try {
                    json.parseToJsonElement(value)
                } catch (e: Exception) {
                    JsonPrimitive(value)
                }
            }
            "string" -> JsonPrimitive(value)
            else -> {
                inferType(value)
            }
        }
    }

    /**
     * Infer type from string value
     */
    private fun inferType(value: String): JsonElement {
        return when {
            value.lowercase() in listOf("true", "false") -> JsonPrimitive(value.toBoolean())
            value.toIntOrNull() != null -> JsonPrimitive(value.toInt())
            value.toDoubleOrNull() != null -> JsonPrimitive(value.toDouble())
            value.startsWith("[") && value.endsWith("]") -> {
                try {
                    json.parseToJsonElement(value)
                } catch (e: Exception) {
                    JsonPrimitive(value)
                }
            }
            value.startsWith("{") && value.endsWith("}") -> {
                try {
                    json.parseToJsonElement(value)
                } catch (e: Exception) {
                    JsonPrimitive(value)
                }
            }
            else -> JsonPrimitive(value)
        }
    }

    /**
     * Simple validation - just check required fields
     */
    fun validateRequired(
        parameters: Map<String, String>,
        requiredFields: List<String>
    ): List<String> {
        return requiredFields.filter { field ->
            parameters[field].isNullOrBlank()
        }.map { "Required parameter '$it' is missing" }
    }
}