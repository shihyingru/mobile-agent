package com.luna.morningagent.data.notion

import com.luna.morningagent.data.model.Task

// Boundary for "fetch high-priority tasks". Implementation today is REST against
// api.notion.com (NotionRestClient); when Notion's hosted MCP grows token support
// we can add an MCP-backed implementation behind the same interface.
interface NotionTaskSource {
    suspend fun fetchHighPriorityTasks(): List<Task>
}

class NotionConfigMissingException(message: String) : IllegalStateException(message)
