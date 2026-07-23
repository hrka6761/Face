package ir.hrka.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted face embedding belonging to a [PersonEntity].
 *
 * @property id Stable unique id (UUID string).
 * @property personId Foreign key to [PersonEntity.id].
 * @property embedding Little-endian float blob of the L2-normalized vector.
 * @property dim Embedding dimensionality (e.g. 192).
 * @property createdAt Epoch millis when the embedding was stored.
 */
@Entity(
    tableName = "face_embeddings",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["person_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["person_id"])],
)
data class FaceEmbeddingEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "person_id")
    val personId: String,
    val embedding: ByteArray,
    val dim: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceEmbeddingEntity) return false
        return id == other.id &&
            personId == other.personId &&
            dim == other.dim &&
            createdAt == other.createdAt &&
            embedding.contentEquals(other.embedding)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + personId.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + dim
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
