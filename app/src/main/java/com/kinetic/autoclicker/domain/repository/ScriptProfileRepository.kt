package com.kinetic.autoclicker.domain.repository

import com.kinetic.autoclicker.domain.model.ScriptProfile
import kotlinx.coroutines.flow.Flow

interface ScriptProfileRepository {
    fun observeAllProfiles(): Flow<List<ScriptProfile>>
    suspend fun getProfileById(id: Long): ScriptProfile?
    suspend fun saveProfile(profile: ScriptProfile): Long
    suspend fun deleteProfile(id: Long)
    suspend fun ensureDefaultProfilesSeeded()
}
