package kotlinx.serialization.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.ByteArraySerializer
import kotlinx.serialization.internal.IntArraySerializer
import kotlinx.serialization.shouldBe
import kotlin.test.Test


class JSONArraysTest {
    @Serializable
    data class TestArrays(@Serializable(ByteArraySerializer::class) val byteArray: ByteArray,
                                  @Serializable(IntArraySerializer::class) val intArray: IntArray) {
        override fun equals(other: Any?) = other is TestArrays && byteArray.contentEquals(
                other.byteArray) && intArray.contentEquals(other.intArray)
    }


    @Test
    fun testSerializerArrays() {
        val simpleArrays = TestArrays(byteArrayOf(1, 2, 3), intArrayOf(4, 5, 6))
        val stringified = JSON.nonstrict.stringify(simpleArrays)
        stringified shouldBe """{"byteArray":[1,2,3],"intArray":[4,5,6]}"""
        val parsed = JSON.parse<TestArrays>(stringified)
        parsed shouldBe simpleArrays
    }
}