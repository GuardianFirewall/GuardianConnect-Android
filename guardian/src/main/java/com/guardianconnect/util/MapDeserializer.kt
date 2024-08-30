package com.guardianconnect.util

import com.google.gson.*
import java.lang.reflect.Type

class MapDeserializer : JsonDeserializer<Map<String, Any>> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Map<String, Any> {
        return read(json) as Map<String, Any>
    }

    private fun read(json: JsonElement): Any? {
        return when {
            json.isJsonArray -> json.asJsonArray.map { read(it) }
            json.isJsonObject -> json.asJsonObject.entrySet().associate { it.key to read(it.value) }
            json.isJsonPrimitive -> {
                val prim = json.asJsonPrimitive
                when {
                    prim.isBoolean -> prim.asBoolean
                    prim.isString -> prim.asString
                    prim.isNumber -> {
                        // Determine if the number is an integer or not
                        val num = prim.asNumber
                        if (num.toLong() == num.toDouble().toLong()) num.toLong() else num.toDouble()
                    }
                    else -> null
                }
            }
            else -> null
        }
    }
}
