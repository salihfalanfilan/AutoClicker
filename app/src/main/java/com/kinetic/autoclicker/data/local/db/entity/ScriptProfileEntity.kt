package com.kinetic.autoclicker.data.local.db.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "script_profiles")
data class ScriptProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val mode: String,
    val intervalValue: Int,
    val timeUnit: String,
    val stopConditionType: String,
    val timerMinutes: Int,
    val cycleCount: Int,
    val smartJitterEnabled: Boolean,
    val jitterRadiusPx: Int,
    val holdDurationMs: Long,
    val swipeDurationMs: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "target_points",
    foreignKeys = [
        ForeignKey(
            entity = ScriptProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class TargetPointEntity(
    @PrimaryKey
    val id: String,
    val profileId: Long,
    val orderIndex: Int,
    val label: String,
    val x: Float,
    val y: Float,
    val normalizedX: Float,
    val normalizedY: Float,
    val endX: Float?,
    val endY: Float?,
    val normalizedEndX: Float?,
    val normalizedEndY: Float?,
    val actionType: String,
    val intervalValue: Int,
    val timeUnit: String,
    val holdDurationMs: Long,
    val stopConditionType: String,
    val timerMinutes: Int,
    val cycleCount: Int,
    val enabled: Boolean
)

data class ScriptProfileWithTargets(
    @Embedded val profile: ScriptProfileEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "profileId"
    )
    val targets: List<TargetPointEntity>
)
