package com.kinetic.autoclicker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kinetic.autoclicker.data.local.db.entity.ScriptProfileEntity
import com.kinetic.autoclicker.data.local.db.entity.ScriptProfileWithTargets
import com.kinetic.autoclicker.data.local.db.entity.TargetPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptProfileDao {

    @Transaction
    @Query("SELECT * FROM script_profiles ORDER BY updatedAt DESC")
    fun observeAllProfilesWithTargets(): Flow<List<ScriptProfileWithTargets>>

    @Transaction
    @Query("SELECT * FROM script_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileWithTargetsById(id: Long): ScriptProfileWithTargets?

    @Query("SELECT COUNT(*) FROM script_profiles")
    suspend fun getProfileCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ScriptProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargetPoints(targets: List<TargetPointEntity>)

    @Query("DELETE FROM target_points WHERE profileId = :profileId")
    suspend fun deleteTargetsForProfile(profileId: Long)

    @Query("DELETE FROM script_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Long)

    @Transaction
    suspend fun upsertProfileWithTargets(
        profile: ScriptProfileEntity,
        targetsProvider: (Long) -> List<TargetPointEntity>
    ): Long {
        val profileId = insertProfile(profile)
        deleteTargetsForProfile(profileId)
        insertTargetPoints(targetsProvider(profileId))
        return profileId
    }
}
