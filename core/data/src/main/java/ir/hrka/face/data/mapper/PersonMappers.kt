package ir.hrka.face.data.mapper

import ir.hrka.database.model.FaceEmbeddingEntity
import ir.hrka.database.model.PersonEntity
import ir.hrka.database.util.EmbeddingConverters
import ir.hrka.face.model.EnrolledFace
import ir.hrka.face.model.FaceEmbedding
import ir.hrka.face.model.Person

/**
 * Maps a [PersonEntity] to the domain [Person] model.
 */
fun PersonEntity.toDomain(): Person =
    Person(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

/**
 * Maps a domain [Person] to a [PersonEntity].
 */
fun Person.toEntity(): PersonEntity =
    PersonEntity(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

/**
 * Maps a person entity and embedding entity to an [EnrolledFace].
 *
 * @param personEntity Owner row.
 * @param embeddingEntity Embedding row.
 */
fun toEnrolledFace(
    personEntity: PersonEntity,
    embeddingEntity: FaceEmbeddingEntity,
): EnrolledFace =
    EnrolledFace(
        person = personEntity.toDomain(),
        embedding = FaceEmbedding(EmbeddingConverters.toFloatArray(embeddingEntity.embedding)),
    )
