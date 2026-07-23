package ir.hrka.face.domain

import ir.hrka.face.data.repository.PersonRepository
import ir.hrka.face.model.Person
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes all enrolled persons.
 *
 * @property personRepository Persistence repository.
 */
class ObservePersonsUseCase @Inject constructor(
    private val personRepository: PersonRepository,
) {

    /**
     * @return Cold [Flow] of enrolled persons.
     */
    operator fun invoke(): Flow<List<Person>> = personRepository.observePersons()
}
