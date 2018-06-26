package kotlinx.io

sealed class PrimitiveArrayView<T : Number> {

    /**
     * The number of elements in the array
     * */
    abstract val size: Int

    /**
     * Iterator over the elements of the array
     */
    abstract operator fun iterator(): Iterator<T>

    class ByteArrayView(val array: ByteArray) : PrimitiveArrayView<Byte>() {
        override val size = array.size

        override operator fun iterator(): ByteIterator = array.iterator()
    }

    class IntArrayView(val array: IntArray) : PrimitiveArrayView<Int>() {
        override val size = array.size

        override operator fun iterator(): IntIterator = array.iterator()
    }

    companion object {
        fun adapt(array: ByteArray): ByteArrayView = ByteArrayView(array)
        fun adapt(array: IntArray): IntArrayView = IntArrayView(array)
    }
}