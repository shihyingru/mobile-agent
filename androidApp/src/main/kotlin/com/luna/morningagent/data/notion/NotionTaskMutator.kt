package com.luna.morningagent.data.notion

import com.luna.morningagent.data.model.Priority

// Write-side mirror of NotionTaskSource. Each method PATCHes one property type
// on `pages/{id}`. Today's implementation is REST (NotionRestMutator); when
// Notion's hosted MCP grows write support we can add an MCP-backed
// implementation behind the same interface.
//
// Reversible operations only by design (no archive, no delete). Hallucinated
// taskIds are pre-filtered in AgentRepository, but HomeViewModel re-validates
// against the current briefing's task set as belt-and-suspenders.
interface NotionTaskMutator {
    suspend fun markDone(taskId: String)
    suspend fun reschedule(taskId: String, newDate: String)
    suspend fun changePriority(taskId: String, newPriority: Priority)
}
