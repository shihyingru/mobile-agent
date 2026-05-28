package com.luna.morningagent.data.tempplan

import com.luna.morningagent.data.secure.TokenStore
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.serialization.json.Json

@OptIn(ExperimentalTime::class)
class TempPlanRepository(
    private val tokenStore: TokenStore,
    private val notionCreator: NotionTaskCreator = NotionTaskCreator(tokenStore),
) {

    fun getActivePlan(): TempPlan? =
        readCache().firstOrNull { !it.archived }

    fun listAll(): List<TempPlan> = readCache()

    fun createPlan(name: String, startDate: String, endDate: String): TempPlan {
        val plans = readCache().toMutableList()
        // Auto-archive any existing active plan.
        plans.replaceAll { if (!it.archived) it.copy(archived = true) else it }
        val plan = TempPlan(
            id        = UUID.randomUUID().toString(),
            name      = name,
            startDate = startDate,
            endDate   = endDate,
            createdAt = Clock.System.now(),
        )
        plans.add(plan)
        writeCache(plans)
        return plan
    }

    fun updatePlanDates(planId: String, startDate: String, endDate: String) {
        updatePlan(planId) { it.copy(startDate = startDate, endDate = endDate) }
    }

    fun deletePlan(planId: String) {
        writeCache(readCache().filter { it.id != planId })
    }

    fun addTask(planId: String, title: String, dayIndex: Int? = null) {
        updatePlan(planId) { plan ->
            val task = TempTask(
                id       = UUID.randomUUID().toString(),
                title    = title,
                dayIndex = dayIndex,
            )
            plan.copy(tasks = plan.tasks + task)
        }
    }

    fun toggleTask(planId: String, taskId: String) {
        updatePlan(planId) { plan ->
            plan.copy(tasks = plan.tasks.map {
                if (it.id == taskId) it.copy(checked = !it.checked) else it
            })
        }
    }

    fun removeTask(planId: String, taskId: String) {
        updatePlan(planId) { plan ->
            plan.copy(tasks = plan.tasks.filter { it.id != taskId })
        }
    }

    suspend fun promoteTask(planId: String, taskId: String): String {
        val plan = readCache().first { it.id == planId }
        val task = plan.tasks.first { it.id == taskId }
        val notionId = notionCreator.createTaskPage(task.title)
        updatePlan(planId) { p ->
            p.copy(tasks = p.tasks.map {
                if (it.id == taskId) it.copy(promotedToNotionId = notionId) else it
            })
        }
        return notionId
    }

    fun archiveExpiredPlans() {
        val today = LocalDate.now()
        val cutoff = today.minusDays(30)
        val plans = readCache()
        val updated = plans
            .map { plan ->
                if (!plan.archived && LocalDate.parse(plan.endDate) < today)
                    plan.copy(archived = true)
                else plan
            }
            .filter { plan ->
                !plan.archived || LocalDate.parse(plan.endDate) >= cutoff
            }
        if (updated != plans) writeCache(updated)
    }

    @Synchronized
    private fun updatePlan(planId: String, transform: (TempPlan) -> TempPlan) {
        writeCache(readCache().map { if (it.id == planId) transform(it) else it })
    }

    private fun readCache(): List<TempPlan> {
        val raw = tokenStore.getTempPlansCacheJson() ?: return emptyList()
        return runCatching { cacheJson.decodeFromString<List<TempPlan>>(raw) }
            .getOrDefault(emptyList())
    }

    private fun writeCache(plans: List<TempPlan>) {
        runCatching {
            tokenStore.saveTempPlansCacheJson(cacheJson.encodeToString(plans))
        }
    }

    private companion object {
        val cacheJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}
