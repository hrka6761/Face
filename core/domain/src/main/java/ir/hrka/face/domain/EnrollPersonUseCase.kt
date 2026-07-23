package ir.hrka.face.domain

import ir.hrka.face.data.repository.PersonRepository
import ir.hrka.face.model.FaceEmbedding
import ir.hrka.face.model.Person
import javax.inject.Inject

/**
 * Enrolls a new person identity with one or more face embedding templates.
 *
 * @property personRepository Persistence repository.
 */
class EnrollPersonUseCase @Inject constructor(
    private val personRepository: PersonRepository,
) {

    /**
     * @param name Display name.
     * @param embeddings One or more L2-normalized face embeddings (multi-template).
     * @return Newly enrolled [Person].
     */
    suspend operator fun invoke(name: String, embeddings: List<FaceEmbedding>): Person =
        personRepository.enrollPerson(name, embeddings)
}
