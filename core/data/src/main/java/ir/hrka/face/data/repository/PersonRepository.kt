package ir.hrka.face.data.repository

import ir.hrka.face.model.EnrolledFace
import ir.hrka.face.model.FaceEmbedding
import ir.hrka.face.model.Person
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for enrolled person identities and face embeddings.
 */
interface PersonRepository {

    /**
     * Observes all enrolled persons, newest updates first.
     */
    fun observePersons(): Flow<List<Person>>

    /**
     * Loads every enrolled face template (person + embedding) for matching.
     *
     * A person may appear multiple times — once per stored template.
     */
    suspend fun getEnrolledFaces(): List<EnrolledFace>

    /**
     * Enrolls a new person with one or more face embedding templates.
     *
     * Multiple templates (different distances / scales / frames) greatly improve
     * recognition robustness.
     *
     * @param name Display name.
     * @param embeddings L2-normalized face embeddings (at least one).
     * @return The newly created [Person].
     * @throws IllegalArgumentException if [name] is blank or [embeddings] is empty.
     */
    suspend fun enrollPerson(name: String, embeddings: List<FaceEmbedding>): Person

    /**
     * Deletes a person and cascading embeddings.
     *
     * @param personId Person UUID.
     */
    suspend fun deletePerson(personId: String)
}
