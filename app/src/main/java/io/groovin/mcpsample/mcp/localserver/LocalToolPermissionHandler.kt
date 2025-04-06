package io.groovin.mcpsample.mcp.localserver

import android.content.Intent

class LocalToolPermissionHandler {
    private var handler: PermissionRequestHandler? = null
    fun setHandler(handler: PermissionRequestHandler?) {
        this.handler = handler
    }
    suspend fun request(permissions: List<String>): Boolean {
        return handler?.onRequestPermission(permissions) ?: false
    }

    fun requestCustomPermission(guideText: String, onActionIntent: Intent?) {
        handler?.onRequestCustomPermission(guideText, onActionIntent)
    }
}

interface PermissionRequestHandler {
    suspend fun onRequestPermission(permissions: List<String>): Boolean
    fun onRequestCustomPermission(guideText: String, onActionIntent: Intent?)
}
