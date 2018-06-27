/*
 * Copyright 2017 JetBrains s.r.o.
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

package kotlinx.serialization

import kotlinx.io.PrintWriter
import kotlinx.io.Reader
import kotlinx.io.StringReader
import kotlinx.io.StringWriter
import kotlinx.serialization.internal.ByteArraySerializer
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SerializeZooTest {
    @Test
    fun testZoo() {
        // save to string
        val sw = StringWriter()
        val out = KeyValueOutput(PrintWriter(sw))
        out.write(zoo)
        // load from string
        val str = sw.toString()
        val inp = KeyValueInput(Parser(StringReader(str)))
        val other = inp.read<Zoo>()
        // assert we've got it back from string
        assertEquals(zoo, other)
        assertFalse(zoo === other)
    }

    // Test data -- Zoo of types

    enum class Attitude { POSITIVE, NEUTRAL, NEGATIVE }

    @Serializable
    data class IntData(val intV: Int)

    @Serializable
    data class Tree(val name: String, val left: Tree? = null, val right: Tree? = null)

    @Serializable
    data class Zoo(
            val unit: Unit,
            val boolean: Boolean,
            val byte: Byte,
            val short: Short,
            val int: Int,
            val long: Long,
            val float: Float,
            val double: Double,
            val char: Char,
            val string: String,
            val enum: Attitude,
            val intData: IntData,
            val unitN: Unit?,
            val booleanN: Boolean?,
            val byteN: Byte?,
            val shortN: Short?,
            val intN: Int?,
            val longN: Long?,
            val floatN: Float?,
            val doubleN: Double?,
            val charN: Char?,
            val stringN: String?,
            val enumN: Attitude?,
            val intDataN: IntData?,
            val listInt: List<Int>,
            val listIntN: List<Int?>,
            val listNInt: Set<Int>?,
            val listNIntN: MutableSet<Int?>?,
            val listListEnumN: List<List<Attitude?>>,
            val listIntData: List<IntData>,
            val listIntDataN: MutableList<IntData?>,
            val tree: Tree,
            val mapStringInt: Map<String,Int>,
            val mapIntStringN: Map<Int,String?>,
            val arrays: ZooWithArrays
    )

    @Serializable data class ZooWithArrays(
            val arrByte: Array<Byte>,
            @Serializable(with = ByteArraySerializer::class)
            val byteArray: ByteArray,
            val arrInt: Array<Int>,
            val arrIntN: Array<Int?>,
            val arrIntData: Array<IntData>

    ) {
        override fun equals(other: Any?) = other is ZooWithArrays &&
                arrByte.contentEquals(other.arrByte) &&
                byteArray.contentEquals(other.byteArray) &&
                arrInt.contentEquals(other.arrInt) &&
                arrIntN.contentEquals(other.arrIntN) &&
                arrIntData.contentEquals(other.arrIntData)
    }

    val zoo = Zoo(
            Unit, true, 10, 20, 30, 40, 50f, 60.0, 'A', "Str0", Attitude.POSITIVE, IntData(70),
            null, null, 11, 21, 31, 41, 51f, 61.0, 'B', "Str1", Attitude.NEUTRAL, null,
            listOf(1, 2, 3),
            listOf(4, 5, null),
            setOf(6, 7, 8),
            mutableSetOf(null, 9, 10),
            listOf(listOf(Attitude.NEGATIVE, null)),
            listOf(IntData(1), IntData(2), IntData(3)),
            mutableListOf(IntData(1), null, IntData(3)),
            Tree("root", Tree("left"), Tree("right", Tree("right.left"), Tree("right.right"))),
            mapOf("one" to 1, "two" to 2, "three" to 3),
            mapOf(0 to null, 1 to "first", 2 to "second"),
            ZooWithArrays(
                    arrayOf(1, 2, 3),
                    byteArrayOf(4, 5, 6),
                    arrayOf(100, 200, 300),
                    arrayOf(null, -1, -2),
                    arrayOf(IntData(1), IntData(2))
            )
    )

    // KeyValue Input/Output

    class KeyValueOutput(val out: PrintWriter) : ElementValueOutput() {
        override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput {
            out.print('{')
            return this
        }

        override fun writeEnd(desc: KSerialClassDesc) = out.print('}')

        override fun writeElement(desc: KSerialClassDesc, index: Int): Boolean {
            if (index > 0) out.print(", ")
            out.print(desc.getElementName(index))
            out.print(':')
            return true
        }

        override fun writeNullValue() = out.print("null")
        override fun writeNonSerializableValue(value: Any) = out.print(value)

        override fun writeStringValue(value: String) {
            out.print('"')
            out.print(value)
            out.print('"')
        }

        override fun writePrimitiveArrayValue(values: PrimitiveArrayValue<*>) {
            out.print('[')
            out.print(values.iterator().asSequence().joinToString(","))
            out.print(']')
        }

        override fun writeCharValue(value: Char) = writeStringValue(value.toString())
    }

    class KeyValueInput(val inp: Parser) : ElementValueInput() {
        override fun readBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KInput {
            inp.expectAfterWhiteSpace('{')
            return this
        }

        override fun readEnd(desc: KSerialClassDesc) = inp.expectAfterWhiteSpace('}')

        override fun readElement(desc: KSerialClassDesc): Int {
            inp.skipWhitespace(',')
            val name = inp.nextUntil(':', '}')
            if (name.isEmpty())
                return READ_DONE
            val index = desc.getElementIndexOrThrow(name)
            inp.expect(':')
            return index
        }

        private fun readToken(): String {
            inp.skipWhitespace()
            return inp.nextUntil(' ', ',', '}')
        }

        override fun readNotNullMark(): Boolean {
            inp.skipWhitespace()
            if (inp.cur != 'n'.toInt()) return true
            return false
        }

        override fun readNullValue(): Nothing? {
            check(readToken() == "null") { "'null' expected" }
            return null
        }

        override fun readBooleanValue(): Boolean = readToken() == "true"
        override fun readByteValue(): Byte = readToken().toByte()
        override fun readShortValue(): Short = readToken().toShort()
        override fun readIntValue(): Int = readToken().toInt()
        override fun readLongValue(): Long = readToken().toLong()
        override fun readFloatValue(): Float = readToken().toFloat()
        override fun readDoubleValue(): Double = readToken().toDouble()

        override fun <T : Enum<T>> readEnumValue(enumClass: KClass<T>): T {
            return enumFromName(enumClass, readToken())
        }

        override fun readStringValue(): String {
            inp.expectAfterWhiteSpace('"')
            val value = inp.nextUntil('"')
            inp.expect('"')
            return value
        }

        override fun <T : Number> readPrimitiveArrayValue(numberClass: KClass<T>): PrimitiveArrayValue<T> {
            inp.expectAfterWhiteSpace('[')
            val value = inp.nextUntil(']')
            val ans: PrimitiveArrayValue<*> = when(numberClass) {
                Byte::class -> value.split(",").map { it.toByte() }.toPrimitiveArray()
                Int::class -> value.split(",").map { it.toInt() }.toPrimitiveArray()
                else -> TODO()
            }
            inp.expect(']')
            @Suppress("UNCHECKED_CAST")
            return ans as PrimitiveArrayValue<T>
        }

        override fun readCharValue(): Char = readStringValue().single()
    }

    // Parser

    // Very simple char-by-char parser
    class Parser(private val inp: Reader) {
        var cur: Int = inp.read()

        fun next() {
            cur = inp.read()
        }

        fun skipWhitespace(vararg c: Char) {
            while (cur >= 0 && (cur.toChar().isWhitespace() || cur.toChar() in c))
                next()
        }

        fun expect(c: Char) {
            check(cur == c.toInt()) { "Expected '$c'" }
            next()
        }

        fun expectAfterWhiteSpace(c: Char) {
            skipWhitespace()
            expect(c)
        }

        fun  nextUntil(vararg c: Char): String {
            val sb = StringBuilder()
            while (cur >= 0 && cur.toChar() !in c) {
                sb.append(cur.toChar())
                next()
            }
            return sb.toString()
        }
    }
}
