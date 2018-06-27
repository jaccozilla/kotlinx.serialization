package kotlinx.serialization

/**
 * Wrapper around a primitive array ([ByteArray], [IntArray], ect...) for serialization. See
 * [KOutput.writePrimitiveArrayValue] and [KInput.readPrimitiveArrayValue]
 */
sealed class PrimitiveArrayValue<T : Number> {

    /**
     * The number of elements in the array
     * */
    abstract val size: Int

    /**
     * Iterator over the elements of the array
     */
    abstract operator fun iterator(): Iterator<T>

    class ByteArrayValue(val array: ByteArray) : PrimitiveArrayValue<Byte>() {
        override val size = array.size

        override operator fun iterator(): ByteIterator = array.iterator()
    }

    class IntArrayValue(val array: IntArray) : PrimitiveArrayValue<Int>() {
        override val size = array.size

        override operator fun iterator(): IntIterator = array.iterator()
    }

    class LongArrayValue(val array: LongArray) : PrimitiveArrayValue<Long>() {
        override val size = array.size

        override operator fun iterator(): LongIterator = array.iterator()
    }
}


fun ByteArray.asPrimitiveArray() : PrimitiveArrayValue.ByteArrayValue = PrimitiveArrayValue.ByteArrayValue(this)
fun Collection<Byte>.toPrimitiveArray(): PrimitiveArrayValue.ByteArrayValue = this.toByteArray().asPrimitiveArray()
fun PrimitiveArrayValue<Byte>.asByteArray(): ByteArray = (this as PrimitiveArrayValue.ByteArrayValue).array

fun IntArray.asPrimitiveArray() : PrimitiveArrayValue.IntArrayValue = PrimitiveArrayValue.IntArrayValue(this)
fun Collection<Int>.toPrimitiveArray(): PrimitiveArrayValue.IntArrayValue = this.toIntArray().asPrimitiveArray()
fun PrimitiveArrayValue<Int>.asIntArray(): IntArray = (this as PrimitiveArrayValue.IntArrayValue).array

fun LongArray.asPrimitiveArray() : PrimitiveArrayValue.LongArrayValue = PrimitiveArrayValue.LongArrayValue(this)
fun Collection<Long>.toPrimitiveArray(): PrimitiveArrayValue.LongArrayValue = this.toLongArray().asPrimitiveArray()
fun PrimitiveArrayValue<Long>.asLongArray(): LongArray = (this as PrimitiveArrayValue.LongArrayValue).array