/*
 *  Copyright 2017 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package kotlinx.serialization.features

import kotlinx.serialization.KInput
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.byteArrayFromBase64String
import kotlinx.serialization.json.JSON
import kotlinx.serialization.serializerByTypeToken
import kotlinx.serialization.shouldBe
import org.junit.Test
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.Arrays
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResolvingTest {
    @Serializable
    data class Box<out T>(val a: T)

    @Serializable
    data class Data(val l: List<String>, val b: Box<Int>)

    @Serializable
    data class WithCustomDefault(val n: Int) {
        @Serializer(forClass = WithCustomDefault::class)
        companion object {
            override fun load(input: KInput) = WithCustomDefault(input.readIntValue())
        }
    }

    object IntBoxToken : ParameterizedType {
        override fun getRawType() = Box::class.java
        override fun getOwnerType() = null
        override fun getActualTypeArguments(): Array<Type> = arrayOf(Int::class.java)
    }

    @Test
    fun intBoxTest() {
        val b = Box(42)
        val serial = serializerByTypeToken(IntBoxToken)
        val s = JSON.unquoted.stringify(serial, b)
        assertEquals("{a:42}", s)
    }

    @Test
    fun testArrayResolving() {
        val myArr = arrayOf("a", "b", "c")
        val token = myArr::class.java
        val serial = serializerByTypeToken(token)
        val s = JSON.unquoted.stringify(serial, myArr)
        assertEquals("[a,b,c]", s)
    }

    @Test
    fun testByteArrayResolving() {
        val myArr = byteArrayOf(1, 2, 3)
        val token = myArr::class.java
        val serial = serializerByTypeToken(token)
        val s = JSON.unquoted.stringify(serial, myArr)
        byteArrayFromBase64String(s).toList() shouldBe myArr.toList()
    }

    @Test
    fun testListResolving() {
        val myArr = listOf("a", "b", "c")
        val token = object : ParameterizedType {
            override fun getRawType(): Type = List::class.java
            override fun getOwnerType(): Type? = null
            override fun getActualTypeArguments(): Array<Type> = arrayOf(String::class.java)
        }
        val serial = serializerByTypeToken(token)
        val s = JSON.unquoted.stringify(serial, myArr)
        assertEquals("[a,b,c]", s)
    }

    @Test
    fun objectTest() {
        val b = Data(listOf("a", "b", "c"), Box(42))
        val serial = serializerByTypeToken(Data::class.java)
        val s = JSON.unquoted.stringify(serial, b)
        assertEquals("{l:[a,b,c],b:{a:42}}", s)
    }

    @Test
    fun customDefault() {
        val foo = JSON.unquoted.parse<WithCustomDefault>("9")
        assertEquals(9, foo.n)
    }

    @Serializable
    data class WithNamedCompanion(val a: Int) {
        companion object Named
    }

    @Test
    fun namedCompanionTest() {
        val namedCompanion = WithNamedCompanion(1)
        val serial = serializerByTypeToken(WithNamedCompanion::class.java)
        val s = JSON.unquoted.stringify(serial, namedCompanion)
        assertEquals("{a:1}", s)
    }
}