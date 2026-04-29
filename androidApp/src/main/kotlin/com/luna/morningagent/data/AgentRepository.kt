package com.luna.morningagent.data

import com.luna.morningagent.data.model.Briefing

class AgentRepository {
    // TODO(Phase 2): call Koog agent → Gemini + Notion MCP
    suspend fun runAgent(): Briefing = TODO("Phase 2: call Koog agent → Gemini + Notion MCP")

    // TODO(Phase 2): read from local cache
    suspend fun getLastBriefing(): Briefing? = TODO("Phase 2: read from local cache")
}
