package ir.hrka.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted person identity row.
 *
 * @property id Stable unique id (UUID string).
 * @property name Display name.
 * @property createdAt Epoch millis when the person was created.
 * @property updatedAt Epoch millis of the last update.
 */
@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
