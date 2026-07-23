package ir.hrka.face.model

/**
 * Domain representation of an enrolled person identity.
 *
 * @property id Stable unique identifier (UUID string).
 * @property name Display name entered during enrollment.
 * @property createdAt Epoch millis when the person was first enrolled.
 * @property updatedAt Epoch millis of the last identity update.
 */
data class Person(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)
