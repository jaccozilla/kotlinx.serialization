package kotlinx.serialization

import kotlinx.serialization.json.JsonAstReader
import kotlin.test.Test

class JsonAstTest {
    @Test
    fun simple() {
        val input = """{"a": "foo",              "b": 10, "c": ["foo", 100500, {"bar": "baz"}]}"""
        val elem = JsonAstReader(input).readFully()
        println(elem)
    }
}
