package io.groovin.mcpsample.mcp

import io.groovin.mcpsample.util.logd
import io.ktor.client.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.JSONRPCMessage
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlin.properties.Delegates
import kotlin.time.Duration

/**
 * Client transport for SSE: this will connect to a server using Server-Sent Events for receiving
 * messages and make separate POST requests for sending messages.
 */
class SSETransport(
    private val client: HttpClient,
    private val baseUrl: String,
    private val reconnectionTime: Duration? = null,
    private val requestBuilder: HttpRequestBuilder.() -> Unit = {},
) : Transport {
    private val scope by lazy {
        CoroutineScope(session.coroutineContext + SupervisorJob())
    }

    private val initialized: kotlinx.atomicfu.AtomicBoolean = atomic(false)
    private var session: ClientSSESession by Delegates.notNull()
    private val endpoint = CompletableDeferred<String>()

    override var onClose: (() -> Unit)? = null
    override var onError: ((Throwable) -> Unit)? = null
    override var onMessage: (suspend ((JSONRPCMessage) -> Unit))? = null

    private var job: Job? = null

    override suspend fun start() {
        if (!initialized.compareAndSet(false, true)) {
            error(
                "SSEClientTransport already started! " +
                        "If using Client class, note that connect() calls start() automatically.",
            )
        }

        session = client.sseSession(
            urlString = "$baseUrl/sse",
            reconnectionTime = reconnectionTime,
            block = requestBuilder
        )


        job = scope.launch(CoroutineName("SseMcpClientTransport.collect#${hashCode()}")) {
            session.incoming.collect { event ->
                when (event.event) {
                    "error" -> {
                        val e = IllegalStateException("SSE error: ${event.data}")
                        onError?.invoke(e)
                        throw e
                    }

                    "open" -> {
                        // The connection is open, but we need to wait for the endpoint to be received.
                    }

                    "endpoint" -> {
                        try {
                            val eventData = event.data?.removePrefix("/") ?: ""

                            // check url correctness
                            val maybeEndpoint = Url("$baseUrl/$eventData")

                            endpoint.complete(maybeEndpoint.toString())
                        } catch (e: Exception) {
                            onError?.invoke(e)
                            close()
                            error(e)
                        }
                    }

                    else -> {
                        try {
                            val message = McpJson.decodeFromString<JSONRPCMessage>(event.data ?: "")
                            onMessage?.invoke(message)
                        } catch (e: Exception) {
                            onError?.invoke(e)
                        }
                    }
                }
            }
        }

        endpoint.await()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun send(message: JSONRPCMessage) {
        if (!endpoint.isCompleted) {
            error("Not connected")
        }

        try {
            //TODO : SSE Path issue
            val originEndpoint = endpoint.getCompleted()
            val epReplaced = originEndpoint.replace("/sse/", "")
            logd("SSETransport", "endPoint origin = $originEndpoint, replaced = $epReplaced")
            val response = client.post(epReplaced) {
                headers.append(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(McpJson.encodeToString(message))
            }

            if (!response.status.isSuccess()) {
                val text = response.bodyAsText()
                error("Error POSTing to endpoint (HTTP ${response.status}): $text")
            }
        } catch (e: Exception) {
            onError?.invoke(e)
            throw e
        }
    }

    override suspend fun close() {
        if (!initialized.value) {
            error("SSEClientTransport is not initialized!")
        }

        session.cancel()
        onClose?.invoke()
        job?.cancelAndJoin()
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal val McpJson: Json by lazy {
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        classDiscriminatorMode = ClassDiscriminatorMode.NONE
        explicitNulls = false
    }
}