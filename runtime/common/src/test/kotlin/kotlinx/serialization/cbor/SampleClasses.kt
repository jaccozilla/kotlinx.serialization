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

import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.ByteArraySerializer

@Serializable
data class Simple(val a: String)

@Serializable
data class SmallZoo(
        val str: String,
        val i: Int,
        val nullable: Double?,
        val list: List<String>,
        val map: Map<Int, Boolean>,
        val inner: Simple,
        val innersList: List<Simple>
)

@Serializable
data class NumberZoo(
        val int: Int,
        val long: Long,
        val float: Float,
        val double: Double,
        val boolean: Boolean,
        val char: Char
)

@Serializable
data class ArrayZoo(
        @Serializable(ByteArraySerializer::class)
        val byteArray: ByteArray,
        val arrayByte: Array<Byte>) {

    override fun equals(other: Any?): Boolean = other is ArrayZoo && byteArray.contentEquals(other.byteArray)
            && arrayByte.contentEquals(other.arrayByte)
}