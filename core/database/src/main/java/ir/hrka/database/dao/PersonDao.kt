package ir.hrka.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ir.hrka.database.model.PersonEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data-access object for [PersonEntity] rows.
 */
@Dao
interface PersonDao {

    /**
     * Observes all persons ordered by most recently updated first.
     */
    @Query("SELECT * FROM persons ORDER BY updated_at DESC")
    fun observePersons(): Flow<List<PersonEntity>>

    /**
     * Loads a single person by id.
     *
     * @param id Person UUID.
     * @return Matching entity or `null`.
     */
    @Query("SELECT * FROM persons WHERE id = :id LIMIT 1")
    suspend fun getPerson(id: String): PersonEntity?

    /**
     * Inserts or replaces a person row.
     *
     * @param person Person entity to persist.
     */
    @Upsert
    suspend fun upsertPerson(person: PersonEntity)

    /**
     * Deletes a person by id (embeddings cascade via FK).
     *
     * @param id Person UUID.
     */
    @Query("DELETE FROM persons WHERE id = :id")
    suspend fun deletePerson(id: String)
}
