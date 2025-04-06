package io.groovin.mcpsample.mcp.localserver

import io.modelcontextprotocol.kotlin.sdk.server.Server

interface LocalTool {
    fun addTool(server: Server)
}

fun Server.addTool(localTool: LocalTool) {
    localTool.addTool(this)
}