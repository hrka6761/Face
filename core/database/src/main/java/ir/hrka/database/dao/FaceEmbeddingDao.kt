package ir.hrka.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ir.hrka.database.model.FaceEmbeddingEntity

/**
 * Data-access object for [FaceEmbeddingEntity] rows.
 */
@Dao
interface FaceEmbeddingDao {

    /**
     * Returns every stored embedding.
     */
    @Query("SELECT * FROM face_embeddings")
    suspend fun getAllEmbeddings(): List<FaceEmbeddingEntity>

    /**
     * Returns embeddings for a person.
     *
     * @param personId Owner UUID.
     */
    @Query("SELECT * FROM face_embeddings WHERE person_id = :personId")
    suspend fun getEmbeddingsForPerson(personId: String): List<FaceEmbeddingEntity>

    /**
     * Inserts or replaces an embedding row.
     *
     * @param embedding Embedding entity to persist.
     */
    @Upsert
    suspend fun upsertEmbedding(embedding: FaceEmbeddingEntity)

    /**
     * Deletes all embeddings for a person.
     *
     * @param personId Owner UUID.
     */
    @Query("DELETE FROM face_embeddings WHERE person_id = :personId")
    suspend fun deleteEmbeddingsForPerson(personId: String)
}
