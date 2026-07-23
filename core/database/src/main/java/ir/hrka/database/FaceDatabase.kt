package ir.hrka.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ir.hrka.database.dao.FaceEmbeddingDao
import ir.hrka.database.dao.PersonDao
import ir.hrka.database.model.FaceEmbeddingEntity
import ir.hrka.database.model.PersonEntity

/**
 * App-wide Room database for Face identities.
 *
 * Kept `internal`; consumers inject DAOs via Hilt, not this class directly.
 */
@Database(
    entities = [
        PersonEntity::class,
        FaceEmbeddingEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
internal abstract class FaceDatabase : RoomDatabase() {

    /**
     * DAO for person identity rows.
     */
    abstract fun personDao(): PersonDao

    /**
     * DAO for face embedding rows.
     */
    abstract fun faceEmbeddingDao(): FaceEmbeddingDao
}
