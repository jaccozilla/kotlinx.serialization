/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization.json

import kotlinx.serialization.json.JSON.Parser

sealed class JsonElement

object JsonNull : JsonElement()

data class JsonValue(val value: String) : JsonElement()

data class JsonObject(val values: Map<String, JsonElement>) : JsonElement()

data class JsonArray(val values: List<JsonElement>) : JsonElement()


class JsonAstReader(val input: String) {
    private val p: Parser = Parser(input)

    private inline fun Parser.requireTc(expected: Byte, lazyErrorMsg: () -> String) {
        if (tc != expected)
            fail(curPos, lazyErrorMsg())
    }

    private fun readObject(): JsonElement {
        p.requireTc(TC_BEGIN_OBJ) { "Expected start of object" }
        p.nextToken()
        val result: MutableMap<String, JsonElement> = hashMapOf()
        while (true) {
            if (p.tc == TC_COMMA) p.nextToken()
            if (!p.canBeginValue) break
            val key = p.takeStr()
            p.requireTc(TC_COLON) { "Expected ':'" }
            p.nextToken()
            val elem = read()
            result[key] = elem
        }
        p.requireTc(TC_END_OBJ) { "Expected end of object" }
        p.nextToken()
        return JsonObject(result)
    }

    private fun readValue(): JsonElement = JsonValue(p.takeStr())

    private fun readArray(): JsonElement {
        p.requireTc(TC_BEGIN_LIST) { "Expected start of array" }
        p.nextToken()
        val result: MutableList<JsonElement> = arrayListOf()
        while (true) {
            if (p.tc == TC_COMMA) p.nextToken()
            if (!p.canBeginValue) break
            val elem = read()
            result.add(elem)
        }
        p.requireTc(TC_END_LIST) { "Expected end of array" }
        p.nextToken()
        return JsonArray(result)
    }

    fun read(): JsonElement {
        if (!p.canBeginValue) fail(p.curPos, "Can't begin reading value from here")
        val tc = p.tc
        return when (tc) {
            TC_NULL -> JsonNull.also { p.nextToken() }
            TC_STRING, TC_OTHER -> readValue() // string or literal
            TC_BEGIN_OBJ -> readObject()
            TC_BEGIN_LIST -> readArray()
            else -> fail(p.curPos, "Can't begin reading element")
        }
    }

    fun readFully(): JsonElement {
        val r = read()
        p.requireTc(TC_EOF) { "Input wasn't consumed fully" }
        return r
    }
}

// todo: move common things
private const val TC_OTHER: Byte = 0
private const val TC_STRING: Byte = 1
private const val TC_STRING_ESC: Byte = 2
private const val TC_WS: Byte = 3
private const val TC_COMMA: Byte = 4
private const val TC_COLON: Byte = 5
private const val TC_BEGIN_OBJ: Byte = 6
private const val TC_END_OBJ: Byte = 7
private const val TC_BEGIN_LIST: Byte = 8
private const val TC_END_LIST: Byte = 9
private const val TC_NULL: Byte = 10
private const val TC_INVALID: Byte = 11
private const val TC_EOF: Byte = 12

private fun fail(pos: Int, msg: String): Nothing {
    throw IllegalArgumentException("JSON at $pos: $msg")
}
