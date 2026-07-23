package ir.hrka.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.hrka.database.FaceDatabase
import ir.hrka.database.dao.FaceEmbeddingDao
import ir.hrka.database.dao.PersonDao
import javax.inject.Singleton

/**
 * Provides the singleton [FaceDatabase] instance.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    /**
     * Builds the app Room database file `face-database`.
     *
     * Uses destructive migration for pre-release schema resets.
     *
     * @param context Application context.
     * @return Configured [FaceDatabase].
     */
    @Provides
    @Singleton
    fun providesFaceDatabase(
        @ApplicationContext context: Context,
    ): FaceDatabase =
        Room.databaseBuilder(
            context,
            FaceDatabase::class.java,
            "face-database",
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
}

/**
 * Provides Room DAOs from [FaceDatabase].
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DaosModule {

    /**
     * @param database App Room database.
     * @return [PersonDao] for person rows.
     */
    @Provides
    fun providesPersonDao(
        database: FaceDatabase,
    ): PersonDao = database.personDao()

    /**
     * @param database App Room database.
     * @return [FaceEmbeddingDao] for embedding rows.
     */
    @Provides
    fun providesFaceEmbeddingDao(
        database: FaceDatabase,
    ): FaceEmbeddingDao = database.faceEmbeddingDao()
}
