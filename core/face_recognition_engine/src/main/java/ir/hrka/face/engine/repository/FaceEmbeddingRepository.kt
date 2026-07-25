package ir.hrka.face.engine.repository

import ir.hrka.face.engine.model.FaceEmbedding

/**
 * Persistence contract for face embeddings.
 *
 * The application layer must provide a real implementation (Room, DataStore, etc.).
 * This module intentionally ships only the interface.
 */
interface FaceEmbeddingRepository {

    /** Persists or replaces [embedding]. */
    suspend fun save(embedding: FaceEmbedding)

    /** Returns all stored embeddings. */
    suspend fun getAll(): List<FaceEmbedding>

    /** Returns embeddings belonging to [personId], or an empty list by default. */
    suspend fun getByPersonId(personId: String): List<FaceEmbedding> = emptyList()

    /** Deletes a single embedding by id. Default no-op for lightweight stubs. */
    suspend fun deleteById(id: String) = Unit
}
