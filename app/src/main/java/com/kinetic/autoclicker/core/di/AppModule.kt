package com.kinetic.autoclicker.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.kinetic.autoclicker.data.local.datastore.EngineSettingsRepositoryImpl
import com.kinetic.autoclicker.data.local.db.AutoClickerDatabase
import com.kinetic.autoclicker.data.local.db.dao.ScriptProfileDao
import com.kinetic.autoclicker.data.repository.ScriptProfileRepositoryImpl
import com.kinetic.autoclicker.domain.repository.EngineSettingsRepository
import com.kinetic.autoclicker.domain.repository.ScriptProfileRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindEngineSettingsRepository(
        impl: EngineSettingsRepositoryImpl
    ): EngineSettingsRepository

    @Binds
    @Singleton
    abstract fun bindScriptProfileRepository(
        impl: ScriptProfileRepositoryImpl
    ): ScriptProfileRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("kinetic_engine_settings") }
        )
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AutoClickerDatabase {
        return Room.databaseBuilder(
            context,
            AutoClickerDatabase::class.java,
            "kinetic_autoclicker.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideScriptProfileDao(database: AutoClickerDatabase): ScriptProfileDao {
        return database.scriptProfileDao()
    }
}
