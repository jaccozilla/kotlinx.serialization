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

package kotlinx.serialization.cbor

import kotlinx.io.ByteArrayInputStream
import kotlinx.serialization.internal.HexConverter
import kotlinx.serialization.shouldBe
import kotlin.test.Test

class CBORReaderTest {
    fun withDecoder(input: String, block: CBOR.CBORDecoder.() -> Unit) {
        val bytes = HexConverter.parseHexBinary(input.toUpperCase())
        CBOR.CBORDecoder(ByteArrayInputStream(bytes)).block()
    }

    @Test
    fun testDecodeIntegers() {
        withDecoder("0C1903E8", {
            nextNumber() shouldBe 12L
            nextNumber() shouldBe 1000L
        })
        withDecoder("203903e7", {
            nextNumber() shouldBe -1L
            nextNumber() shouldBe -1000L
        })
    }

    @Test
    fun testDecodeStrings() {
        withDecoder("6568656C6C6F", {
            nextString() shouldBe "hello"
        })
        withDecoder("7828737472696E672074686174206973206C6F6E676572207468616E2032332063686172616374657273", {
            nextString() shouldBe "string that is longer than 23 characters"
        })
    }

    @Test
    fun testDecodeDoubles() {
        withDecoder("fb7e37e43c8800759c", {
            nextDouble() shouldBe 1e+300
        })
        withDecoder("fa47c35000", {
            nextFloat() shouldBe 100000.0f
        })
    }

    @Test
    fun testDecodeSimpleObject() {
        CBOR.loads<Simple>("bf616163737472ff") shouldBe Simple("str")
    }

    @Test
    fun testDecodeComplicatedObject() {
        val test = SmallZoo(
                "Hello, world!",
                42,
                null,
                listOf("a", "b"),
                mapOf(1 to true, 2 to false),
                Simple("lol"),
                listOf(Simple("kek"))
        )

        CBOR.loads<SmallZoo>(
                "bf637374726d48656c6c6f2c20776f726c64216169182a686e756c6c61626c65f6646c6973749f61616162ff636d6170bf01f502f4ff65696e6e6572bf6161636c6f6cff6a696e6e6572734c6973749fbf6161636b656bffffff"
        ) shouldBe test
    }

    @Test
    fun testDecodeArrays() {
        val test = ArrayZoo(byteArrayOf(Byte.MIN_VALUE, 0, Byte.MAX_VALUE),
                arrayOf(Byte.MIN_VALUE, 1, Byte.MAX_VALUE),
                intArrayOf(Int.MIN_VALUE, 2, Int.MAX_VALUE),
                arrayOf(Int.MIN_VALUE, 3, Int.MAX_VALUE),
                longArrayOf(Long.MIN_VALUE, 2, Long.MAX_VALUE),
                arrayOf(Long.MIN_VALUE, 3, Long.MAX_VALUE))

        CBOR.loads<ArrayZoo>(
                "bf696279746541727261794380007f696172726179427974659f387f01187fff68696e744172726179833a7fffffff021a7fffffff686172726179496e749f3a7fffffff031a7fffffffff696c6f6e674172726179833b7fffffffffffffff021b7fffffffffffffff6961727261794c6f6e679f3b7fffffffffffffff031b7fffffffffffffffffff"
        ) shouldBe test
    }
}
