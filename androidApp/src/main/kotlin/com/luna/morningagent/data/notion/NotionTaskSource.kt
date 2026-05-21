package com.luna.morningagent.data.notion

import com.luna.morningagent.data.model.Task

// Boundary for "fetch today's tasks". Implementation today is REST against
// api.notion.com (NotionRestClient); when Notion's hosted MCP grows token support
// we can add an MCP-backed implementation behind the same interface.
//
// "Today" = `Status != Done AND Date <= today` (so overdue items remain visible);
// returned list is sorted by priority desc (High → Mid → Low), then date desc.
interface NotionTaskSource {
    suspend fun fetchTodayTasks(): List<Task>
}

class NotionConfigMissingException(message: String) : IllegalStateException(message)
