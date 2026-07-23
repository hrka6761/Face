package ir.hrka.database.util

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Converts between float embedding arrays and little-endian byte blobs for Room storage.
 */
object EmbeddingConverters {

    /**
     * Serializes a float array to a little-endian [ByteArray].
     *
     * @param values Embedding components.
     * @return Blob suitable for Room persistence.
     */
    fun toByteArray(values: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(values.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        values.forEach(buffer::putFloat)
        return buffer.array()
    }

    /**
     * Deserializes a little-endian float blob.
     *
     * @param bytes Stored embedding blob.
     * @return Float embedding array.
     * @throws IllegalArgumentException if [bytes] length is not a multiple of 4.
     */
    fun toFloatArray(bytes: ByteArray): FloatArray {
        require(bytes.size % Float.SIZE_BYTES == 0) {
            "Embedding blob size ${bytes.size} is not a multiple of ${Float.SIZE_BYTES}"
        }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(bytes.size / Float.SIZE_BYTES) { buffer.float }
    }
}
