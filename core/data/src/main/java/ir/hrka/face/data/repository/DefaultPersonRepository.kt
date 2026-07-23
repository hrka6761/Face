package ir.hrka.face.data.repository

import ir.hrka.database.dao.FaceEmbeddingDao
import ir.hrka.database.dao.PersonDao
import ir.hrka.database.model.FaceEmbeddingEntity
import ir.hrka.database.model.PersonEntity
import ir.hrka.database.util.EmbeddingConverters
import ir.hrka.face.data.mapper.toDomain
import ir.hrka.face.data.mapper.toEnrolledFace
import ir.hrka.face.model.EnrolledFace
import ir.hrka.face.model.FaceEmbedding
import ir.hrka.face.model.Person
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed [PersonRepository] implementation.
 *
 * @property personDao Person DAO.
 * @property faceEmbeddingDao Embedding DAO.
 */
@Singleton
internal class DefaultPersonRepository @Inject constructor(
    private val personDao: PersonDao,
    private val faceEmbeddingDao: FaceEmbeddingDao,
) : PersonRepository {

    override fun observePersons(): Flow<List<Person>> =
        personDao.observePersons().map { entities -> entities.map(PersonEntity::toDomain) }

    override suspend fun getEnrolledFaces(): List<EnrolledFace> {
        val embeddings = faceEmbeddingDao.getAllEmbeddings()
        if (embeddings.isEmpty()) return emptyList()

        return embeddings.mapNotNull { embeddingEntity ->
            val personEntity = personDao.getPerson(embeddingEntity.personId) ?: return@mapNotNull null
            toEnrolledFace(personEntity, embeddingEntity)
        }
    }

    override suspend fun enrollPerson(name: String, embeddings: List<FaceEmbedding>): Person {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Person name must not be blank" }
        require(embeddings.isNotEmpty()) { "At least one embedding is required" }
        require(embeddings.all { it.values.isNotEmpty() }) { "Embeddings must not be empty" }

        val now = System.currentTimeMillis()
        val personId = UUID.randomUUID().toString()
        val personEntity = PersonEntity(
            id = personId,
            name = trimmed,
            createdAt = now,
            updatedAt = now,
        )
        personDao.upsertPerson(personEntity)

        // Deduplicate near-identical templates to keep the gallery diverse but compact.
        val unique = dedupeEmbeddings(embeddings)
        for (embedding in unique) {
            faceEmbeddingDao.upsertEmbedding(
                FaceEmbeddingEntity(
                    id = UUID.randomUUID().toString(),
                    personId = personId,
                    embedding = EmbeddingConverters.toByteArray(embedding.values),
                    dim = embedding.dimension,
                    createdAt = now,
                ),
            )
        }
        return personEntity.toDomain()
    }

    override suspend fun deletePerson(personId: String) {
        personDao.deletePerson(personId)
    }

    private fun dedupeEmbeddings(embeddings: List<FaceEmbedding>): List<FaceEmbedding> {
        val kept = ArrayList<FaceEmbedding>(embeddings.size)
        for (candidate in embeddings) {
            val tooSimilar = kept.any { existing ->
                cosine(existing.values, candidate.values) >= 0.98f
            }
            if (!tooSimilar) {
                kept += candidate
            }
        }
        return if (kept.isEmpty()) listOf(embeddings.first()) else kept
    }

    private fun cosine(a: FloatArray, b: FloatArray): Float {
        val size = minOf(a.size, b.size)
        var dot = 0f
        var na = 0f
        var nb = 0f
        for (i in 0 until size) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        val denom = kotlin.math.sqrt(na) * kotlin.math.sqrt(nb)
        return if (denom == 0f) 0f else dot / denom
    }
}
