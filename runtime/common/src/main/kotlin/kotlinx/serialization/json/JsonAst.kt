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

import kotlinx.serialization.*
import kotlinx.serialization.internal.SIZE_INDEX
import kotlin.reflect.KClass

/**
 * Root node for whole JSON DOM
 */
sealed class JsonElement

sealed class JsonPrimitive : JsonElement() {
    protected abstract val content: String

    val asInt: Int get() = content.toInt()
    val asLong: Long get() = content.toLong()

    val asDouble: Double get() = content.toDouble()
    val asFloat: Float get() = content.toFloat()

    val asBoolean: Boolean get() = content.toBoolean()

    val str: String get() = content
}

/**
 * Represents quoted JSON strings
 */
data class JsonString(override val content: String): JsonPrimitive() {
    private var quotedString: String? = null

    override fun toString(): String = if (quotedString != null) quotedString!! else {
        quotedString = buildString { printQuoted(content) }
        quotedString!!
    }
}

/**
 * Represents unquoted JSON primitives (numbers, booleans and null)
 */
data class JsonLiteral(override val content: String): JsonPrimitive() {
    constructor(number: Number): this(number.toString())
    constructor(boolean: Boolean): this(boolean.toString())

    override fun toString() = content
}

val JsonNull = JsonLiteral("null")

data class JsonObject(val content: Map<String, JsonElement>) : JsonElement(), Map<String, JsonElement> by content {
    fun getAsValue(key: String)= content[key] as? JsonPrimitive
    fun getAsObject(key: String) = content[key] as? JsonObject
    fun getAsArray(key: String) = content[key] as? JsonArray

    override fun toString(): String {
        return content.entries.joinToString(
            prefix = "{",
            postfix = "}",
            transform = {(k, v) -> """"$k": $v"""}
        )
    }
}

data class JsonArray(val content: List<JsonElement>) : JsonElement(), List<JsonElement> by content {
    fun getAsValue(index: Int) = content.getOrNull(index) as? JsonPrimitive
    fun getAsObject(index: Int) = content.getOrNull(index) as? JsonObject
    fun getAsArray(index: Int) = content.getOrNull(index) as? JsonArray

    override fun toString() = content.joinToString(prefix = "[", postfix = "]")
}


class JsonTreeParser(val input: String) {
    private val p: Parser = Parser(input)

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

    private fun readValue(asLiteral: Boolean = false): JsonElement {
        val str = p.takeStr()
        return if (asLiteral) JsonLiteral(str) else JsonString(str)
    }

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
            TC_STRING -> readValue(asLiteral = false)
            TC_OTHER -> readValue(asLiteral = true)
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


class JsonTreeMapper(val context: SerialContext? = null) {
    inline fun <reified T : Any> readTree(tree: JsonElement): T = readTree(tree, context.klassSerializer(T::class))

    fun <T> readTree(obj: JsonElement, loader: KSerialLoader<T>): T {
        if (obj !is JsonObject) throw SerializationException("Can't deserialize primitive on root level")
        return JsonTreeInput(obj).read(loader)
    }

    fun <T> writeTree(obj: T, saver: KSerialSaver<T>): JsonElement {
        lateinit var result: JsonElement
        val output = JsonTreeOutput { result = it }
        output.write(saver, obj)
        return result
    }

    private abstract inner class AbstractJsonTreeOutput(val nodeConsumer: (JsonElement) -> Unit) : NamedValueOutput() {
        init {
            this.context = this@JsonTreeMapper.context
        }

        override fun composeName(parentName: String, childName: String): String = childName

        abstract fun putElement(key: String, element: JsonElement)
        abstract fun getCurrent(): JsonElement

        override fun writeTaggedNull(tag: String) = putElement(tag, JsonNull)

        override fun writeTaggedInt(tag: String, value: Int) = putElement(tag, JsonLiteral(value))
        override fun writeTaggedByte(tag: String, value: Byte) = putElement(tag, JsonLiteral(value))
        override fun writeTaggedShort(tag: String, value: Short) = putElement(tag, JsonLiteral(value))
        override fun writeTaggedLong(tag: String, value: Long) = putElement(tag, JsonLiteral(value))
        override fun writeTaggedFloat(tag: String, value: Float) = putElement(tag, JsonLiteral(value))
        override fun writeTaggedDouble(tag: String, value: Double) = putElement(tag, JsonLiteral(value))
        override fun writeTaggedBoolean(tag: String, value: Boolean) = putElement(tag, JsonLiteral(value))

        override fun writeTaggedChar(tag: String, value: Char) = putElement(tag, JsonString(value.toString()))
        override fun writeTaggedString(tag: String, value: String) = putElement(tag, JsonString(value))
        override fun <E : Enum<E>> writeTaggedEnum(tag: String, enumClass: KClass<E>, value: E) = putElement(tag, JsonString(value.toString()))

        override fun writeTaggedValue(tag: String, value: Any) {
            putElement(tag, JsonString(value.toString()))
        }

        override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput {
            val consumer =
                if (currentTagOrNull == null) nodeConsumer
                else { node -> putElement(currentTag, node) }
            return when (desc.kind) {
                KSerialClassKind.LIST, KSerialClassKind.SET -> JsonTreeListOutput(consumer)
                KSerialClassKind.MAP -> JsonTreeMapOutput(consumer)
                KSerialClassKind.ENTRY -> JsonTreeEntryOutput(this@AbstractJsonTreeOutput::putElement)
                else -> JsonTreeOutput(consumer)
            }
        }

        override fun writeFinished(desc: KSerialClassDesc) {
            nodeConsumer(getCurrent())
        }
    }

    private open inner class JsonTreeOutput(nodeConsumer: (JsonElement) -> Unit) :
        AbstractJsonTreeOutput(nodeConsumer) {
        private val map: MutableMap<String, JsonElement> = hashMapOf()

        override fun putElement(key: String, element: JsonElement) {
            map[key] = element
        }

        override fun getCurrent(): JsonElement = JsonObject(map)
    }

    private inner class JsonTreeMapOutput(nodeConsumer: (JsonElement) -> Unit) : JsonTreeOutput(nodeConsumer) {
        override fun shouldWriteElement(desc: KSerialClassDesc, tag: String, index: Int): Boolean = index != SIZE_INDEX
    }

    private inner class JsonTreeListOutput(nodeConsumer: (JsonElement) -> Unit) : AbstractJsonTreeOutput(nodeConsumer) {
        private val array: ArrayList<JsonElement> = arrayListOf()

        override fun shouldWriteElement(desc: KSerialClassDesc, tag: String, index: Int): Boolean = index != SIZE_INDEX

        override fun putElement(key: String, element: JsonElement) {
            val idx = key.toInt() - 1
            array.add(idx, element)
        }

        override fun getCurrent(): JsonElement = JsonArray(array)
    }

    private inner class JsonTreeEntryOutput(val entryConsumer: (String, JsonElement) -> Unit) :
        AbstractJsonTreeOutput({ throw IllegalStateException("Use entryConsumer instead") }) {

        private lateinit var elem: JsonElement
        private lateinit var tag: String

        override fun putElement(key: String, element: JsonElement) {
            if (key != "key") {
                elem = element
            } else {
                check(element is JsonString) { "Expected tag to be JsonString" }
                tag = (element as JsonString).str
            }
        }

        override fun getCurrent(): JsonElement = elem

        override fun writeFinished(desc: KSerialClassDesc) {
            entryConsumer(tag, elem)
        }
    }

    private abstract inner class AbstractJsonTreeInput(open val obj: JsonElement): NamedValueInput() {
        init {
            this.context = this@JsonTreeMapper.context
        }

        override fun composeName(parentName: String, childName: String): String = childName

        private inline fun <reified T: JsonElement> checkCast(obj: JsonElement): T {
            check(obj is T) { "Expected ${T::class.simpleName} but found ${obj::class.simpleName}" }
            return obj as T
        }

        override fun readBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KInput {
            val curObj = currentTagOrNull?.let { currentElement(it) } ?: obj
            return when (desc.kind) {
                KSerialClassKind.LIST, KSerialClassKind.SET -> JsonTreeListInput(checkCast(curObj))
                KSerialClassKind.MAP -> JsonTreeMapInput(checkCast(curObj))
                KSerialClassKind.ENTRY -> JsonTreeMapEntryInput(curObj, currentTag)
                else -> JsonTreeInput(checkCast(curObj))
            }
        }

        protected open fun getValue(tag: String): JsonPrimitive {
            val currentElement = currentElement(tag)
            return currentElement as? JsonPrimitive ?: throw SerializationException("Expected from $tag to be primitive but found $currentElement")
        }

        protected abstract fun currentElement(tag: String): JsonElement

        override fun readTaggedChar(tag: String): Char {
            val o = getValue(tag)
            return if (o.str.length == 1) o.str[0] else throw SerializationException("$o can't be represented as Char")
        }

        override fun <E : Enum<E>> readTaggedEnum(tag: String, enumClass: KClass<E>): E =
            enumFromName(enumClass, (getValue(tag).str))

        override fun readTaggedNull(tag: String): Nothing? = null
        override fun readTaggedNotNullMark(tag: String) = currentElement(tag) !== JsonNull

        override fun readTaggedUnit(tag: String) {
            return
        }

        override fun readTaggedBoolean(tag: String): Boolean = getValue(tag).asBoolean
        override fun readTaggedByte(tag: String): Byte = getValue(tag).asInt.toByte()
        override fun readTaggedShort(tag: String) = getValue(tag).asInt.toShort()
        override fun readTaggedInt(tag: String) = getValue(tag).asInt
        override fun readTaggedLong(tag: String) = getValue(tag).asLong
        override fun readTaggedFloat(tag: String) = getValue(tag).asFloat
        override fun readTaggedDouble(tag: String) = getValue(tag).asDouble
        override fun readTaggedString(tag: String) = getValue(tag).str

    }

    private open inner class JsonTreeInput(override val obj: JsonObject) : AbstractJsonTreeInput(obj) {

        private var pos = 0

        override fun readElement(desc: KSerialClassDesc): Int {
            while (pos < desc.associatedFieldsCount) {
                val name = desc.getTag(pos++)
                if (name in obj) return pos - 1
            }
            return READ_DONE
        }

        override fun currentElement(tag: String): JsonElement = obj.getValue(tag)

    }

    private inner class JsonTreeMapEntryInput(override val obj: JsonElement, val cTag: String): AbstractJsonTreeInput(obj) {

        override fun currentElement(tag: String): JsonElement = if (tag == "key") {
            JsonString(cTag)
        } else {
            check(tag == "value") { "Found unexpected tag: $tag" }
            obj
        }
    }

    private inner class JsonTreeMapInput(override val obj: JsonObject): JsonTreeInput(obj) {

        private val keys = obj.keys.toList()
        private val size: Int = keys.size
        private var pos = 0

        override fun elementName(desc: KSerialClassDesc, index: Int): String {
            val i = index - 1
            return keys[i]
        }

        override fun readElement(desc: KSerialClassDesc): Int {
            while (pos < size) {
                pos++
                return pos
            }
            return READ_DONE
        }
    }

    private inner class JsonTreeListInput(override val obj: JsonArray): AbstractJsonTreeInput(obj) {

        override fun currentElement(tag: String): JsonElement {
            return obj[tag.toInt()]
        }

        private val size = obj.content.size
        private var pos = 0 // 0st element is SIZE. use it?

        override fun elementName(desc: KSerialClassDesc, index: Int): String = (index - 1).toString()

        override fun readElement(desc: KSerialClassDesc): Int {
            while (pos < size) {
                pos++
                return pos
            }
            return READ_DONE
        }
    }
}
