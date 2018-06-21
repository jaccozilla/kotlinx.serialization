package kotlinx.serialization

import kotlin.test.assertEquals


infix fun <T> T.shouldBe(expected: T) = assertEquals(expected, this)