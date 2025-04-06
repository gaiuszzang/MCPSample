package io.groovin.mcpsample.mcp

import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.PipedInputStream
import java.io.PipedOutputStream

class McpLocalTransport {
    private val clientInput = PipedInputStream()
    private val serverInput = PipedInputStream()
    private val clientOutput = PipedOutputStream(serverInput)
    private val serverOutput = PipedOutputStream(clientInput)

    fun getServerTransport(): Transport {
        return StdioServerTransport(
            inputStream = serverInput.asSource().buffered(),
            outputStream = serverOutput.asSink().buffered()
        )
    }

    fun getClientTransport(): Transport {
        return StdioClientTransport(
            input = clientInput.asSource().buffered(),
            output = clientOutput.asSink().buffered()
        )
    }
}