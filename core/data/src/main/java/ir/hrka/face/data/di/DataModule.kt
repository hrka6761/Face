package ir.hrka.face.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.hrka.face.data.repository.DefaultPersonRepository
import ir.hrka.face.data.repository.PersonRepository
import javax.inject.Singleton

/**
 * Hilt bindings for the data layer.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    /**
     * Binds the Room-backed person repository.
     */
    @Binds
    @Singleton
    abstract fun bindsPersonRepository(
        impl: DefaultPersonRepository,
    ): PersonRepository
}
