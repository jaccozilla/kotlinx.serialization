package kotlinx.serialization.formats.json

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
data class Payload(val from: Long, val to: Long, val msg: String)

sealed class DummyEither {
    data class Left(val errorMsg: String): DummyEither()
    data class Right(val data: Payload): DummyEither()
}

@Serializer(forClass = DummyEither::class)
object EitherSerializer: KSerializer<DummyEither> {
    override fun load(input: KInput): DummyEither {
        val jsonReader = input as? JSON.JsonInput
                ?: throw SerializationException("This class can be loaded only by JSON")
        val tree = jsonReader.readAsTree() as? JsonObject
                ?: throw SerializationException("Expected JSON object")
        if ("error" in tree) return DummyEither.Left(tree.getAsValue("error")?.str!!)
        return DummyEither.Right(JsonTreeMapper().readTree(tree, Payload.serializer()))
    }
}

@Serializable
data class Event(
    val id: Int,
    @Serializable(with=EitherSerializer::class) val payload: DummyEither,
    val timestamp: Long
)

class JsonTreeAndMapperTest {
    val inputData = """{"id": 0, "payload": {"from": 42, "to" : 43, "msg": "Hello world"}, "timestamp": 1000}"""
    val inputError = """{"id": 1, "payload": {"error": "Connection timed out"}, "timestamp": 1001}"""

    @Test
    fun testParseData() {
        val ev = JSON.parse(Event.serializer(), inputData)
        with(ev) {
            assertEquals(0, id)
            assertEquals(DummyEither.Right(Payload(42, 43, "Hello world")), payload)
            assertEquals(1000, timestamp)
        }
    }

    @Test
    fun testParseError() {
        val ev = JSON.parse(Event.serializer(), inputError)
        with(ev) {
            assertEquals(1, id)
            assertEquals(DummyEither.Left("Connection timed out"), payload)
            assertEquals(1001, timestamp)
        }
    }
}
