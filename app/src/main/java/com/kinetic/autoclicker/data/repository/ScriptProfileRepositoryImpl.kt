package com.kinetic.autoclicker.data.repository

import com.kinetic.autoclicker.data.local.db.dao.ScriptProfileDao
import com.kinetic.autoclicker.data.local.db.entity.ScriptProfileEntity
import com.kinetic.autoclicker.data.local.db.entity.ScriptProfileWithTargets
import com.kinetic.autoclicker.data.local.db.entity.TargetPointEntity
import com.kinetic.autoclicker.domain.model.ActionType
import com.kinetic.autoclicker.domain.model.AutomationMode
import com.kinetic.autoclicker.domain.model.EngineSettings
import com.kinetic.autoclicker.domain.model.ScriptProfile
import com.kinetic.autoclicker.domain.model.StopCondition
import com.kinetic.autoclicker.domain.model.StopConditionType
import com.kinetic.autoclicker.domain.model.TargetPoint
import com.kinetic.autoclicker.domain.model.TimeUnitOption
import com.kinetic.autoclicker.domain.repository.ScriptProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScriptProfileRepositoryImpl @Inject constructor(
    private val dao: ScriptProfileDao
) : ScriptProfileRepository {

    override fun observeAllProfiles(): Flow<List<ScriptProfile>> {
        return dao.observeAllProfilesWithTargets().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getProfileById(id: Long): ScriptProfile? {
        return dao.getProfileWithTargetsById(id)?.toDomain()
    }

    override suspend fun saveProfile(profile: ScriptProfile): Long {
        val entity = ScriptProfileEntity(
            id = profile.id,
            name = profile.name,
            mode = profile.mode.name,
            intervalValue = profile.settings.intervalValue,
            timeUnit = profile.settings.timeUnit.name,
            stopConditionType = profile.settings.stopCondition.type.name,
            timerMinutes = profile.settings.stopCondition.timerMinutes,
            cycleCount = profile.settings.stopCondition.cycleCount,
            smartJitterEnabled = profile.settings.smartJitterEnabled,
            jitterRadiusPx = profile.settings.jitterRadiusPx,
            holdDurationMs = profile.settings.holdDurationMs.coerceAtLeast(10L),
            swipeDurationMs = profile.settings.swipeDurationMs.coerceAtLeast(10L),
            updatedAt = System.currentTimeMillis()
        )
        return dao.upsertProfileWithTargets(entity) { savedId ->
            profile.targets.mapIndexed { idx, point ->
                TargetPointEntity(
                    id = point.id.ifBlank { UUID.randomUUID().toString() },
                    profileId = savedId,
                    orderIndex = idx + 1,
                    label = point.label,
                    x = point.x,
                    y = point.y,
                    normalizedX = point.normalizedX.coerceIn(0f, 1f),
                    normalizedY = point.normalizedY.coerceIn(0f, 1f),
                    endX = point.endX,
                    endY = point.endY,
                    normalizedEndX = point.normalizedEndX?.coerceIn(0f, 1f),
                    normalizedEndY = point.normalizedEndY?.coerceIn(0f, 1f),
                    actionType = point.actionType.name,
                    intervalValue = point.intervalValue,
                    timeUnit = point.timeUnit.name,
                    holdDurationMs = point.holdDurationMs.coerceAtLeast(10L),
                    stopConditionType = point.stopCondition.type.name,
                    timerMinutes = point.stopCondition.timerMinutes,
                    cycleCount = point.stopCondition.cycleCount,
                    enabled = point.enabled
                )
            }
        }
    }

    override suspend fun deleteProfile(id: Long) {
        dao.deleteProfileById(id)
    }

    override suspend fun ensureDefaultProfilesSeeded() {
        if (dao.getProfileCount() > 0) return

        val dailyFarmProfile = ScriptProfile(
            name = "Config #1 (Daily Farm)",
            mode = AutomationMode.MULTI_TARGET,
            settings = EngineSettings(
                intervalValue = 120,
                timeUnit = TimeUnitOption.MILLISECONDS,
                stopCondition = StopCondition(StopConditionType.INFINITE, 5, 100),
                smartJitterEnabled = true
            ),
            targets = listOf(
                TargetPoint(
                    id = "seed-target-1",
                    orderIndex = 1,
                    label = "Collect Reward Button",
                    x = 740f,
                    y = 1480f,
                    actionType = ActionType.TAP,
                    intervalValue = 120
                ),
                TargetPoint(
                    id = "seed-target-2",
                    orderIndex = 2,
                    label = "Skill Slot #1",
                    x = 540f,
                    y = 1120f,
                    actionType = ActionType.TAP,
                    intervalValue = 120
                ),
                TargetPoint(
                    id = "seed-target-3",
                    orderIndex = 3,
                    label = "Skill Slot #2",
                    x = 340f,
                    y = 1120f,
                    actionType = ActionType.TAP,
                    intervalValue = 120
                ),
                TargetPoint(
                    id = "seed-target-4",
                    orderIndex = 4,
                    label = "Confirm Dialog",
                    x = 540f,
                    y = 920f,
                    actionType = ActionType.TAP,
                    intervalValue = 150
                ),
                TargetPoint(
                    id = "seed-target-5",
                    orderIndex = 5,
                    label = "Arena Map Pan",
                    x = 320f,
                    y = 780f,
                    endX = 760f,
                    endY = 780f,
                    actionType = ActionType.SWIPE,
                    intervalValue = 300
                )
            )
        )

        val rapidTapProfile = ScriptProfile(
            name = "Rapid Boss Tap (100ms)",
            mode = AutomationMode.SINGLE_TARGET,
            settings = EngineSettings(
                intervalValue = 100,
                timeUnit = TimeUnitOption.MILLISECONDS,
                stopCondition = StopCondition(StopConditionType.CYCLE_COUNT, 5, 100),
                smartJitterEnabled = true
            ),
            targets = listOf(
                TargetPoint(
                    id = "seed-rapid-1",
                    orderIndex = 1,
                    label = "Anchor point #1",
                    x = 540f,
                    y = 1120f,
                    actionType = ActionType.TAP,
                    intervalValue = 100
                )
            )
        )

        saveProfile(dailyFarmProfile)
        saveProfile(rapidTapProfile)
    }

    private fun ScriptProfileWithTargets.toDomain(): ScriptProfile {
        val modeEnum = runCatching { AutomationMode.valueOf(profile.mode) }
            .getOrDefault(AutomationMode.MULTI_TARGET)
        val unitEnum = runCatching { TimeUnitOption.valueOf(profile.timeUnit) }
            .getOrDefault(TimeUnitOption.MILLISECONDS)
        val stopTypeEnum = runCatching { StopConditionType.valueOf(profile.stopConditionType) }
            .getOrDefault(StopConditionType.INFINITE)

        val sortedTargets = targets.sortedBy { it.orderIndex }.map { entity ->
            val tUnit = runCatching { TimeUnitOption.valueOf(entity.timeUnit) }
                .getOrDefault(TimeUnitOption.MILLISECONDS)
            val tAction = runCatching { ActionType.valueOf(entity.actionType) }
                .getOrDefault(ActionType.TAP)
            val tStop = runCatching { StopConditionType.valueOf(entity.stopConditionType) }
                .getOrDefault(StopConditionType.INFINITE)

            TargetPoint(
                id = entity.id,
                orderIndex = entity.orderIndex,
                label = entity.label,
                x = entity.x,
                y = entity.y,
                normalizedX = entity.normalizedX,
                normalizedY = entity.normalizedY,
                endX = entity.endX,
                endY = entity.endY,
                normalizedEndX = entity.normalizedEndX,
                normalizedEndY = entity.normalizedEndY,
                actionType = tAction,
                intervalValue = entity.intervalValue,
                timeUnit = tUnit,
                holdDurationMs = entity.holdDurationMs.coerceAtLeast(10L),
                stopCondition = StopCondition(
                    type = tStop,
                    timerMinutes = entity.timerMinutes,
                    cycleCount = entity.cycleCount
                ),
                enabled = entity.enabled
            )
        }

        return ScriptProfile(
            id = profile.id,
            name = profile.name,
            mode = modeEnum,
            targets = sortedTargets,
            settings = EngineSettings(
                intervalValue = profile.intervalValue,
                timeUnit = unitEnum,
                stopCondition = StopCondition(
                    type = stopTypeEnum,
                    timerMinutes = profile.timerMinutes,
                    cycleCount = profile.cycleCount
                ),
                smartJitterEnabled = profile.smartJitterEnabled,
                jitterRadiusPx = profile.jitterRadiusPx,
                holdDurationMs = profile.holdDurationMs.coerceAtLeast(10L),
                swipeDurationMs = profile.swipeDurationMs.coerceAtLeast(10L)
            ),
            updatedAt = profile.updatedAt
        )
    }
}
