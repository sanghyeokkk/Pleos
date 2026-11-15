package com.example.myapplication

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle

/** Requests enter only through the permission-protected activity. */
internal object GleoRequestBridge {
    private val pending = java.util.ArrayDeque<Intent>()
    private var listener: (() -> Unit)? = null
    private var recordListener: (() -> Unit)? = null
    @Synchronized fun attach(callback: () -> Unit) { listener = callback }
    @Synchronized fun detach(callback: () -> Unit) { if (listener === callback) listener = null }
    @Synchronized fun enqueue(request: Intent): Boolean {
        pending.addLast(Intent(request))
        val current = listener ?: return false
        current()
        return true
    }
    @Synchronized fun take(): Intent? = pending.pollFirst()
    @Synchronized fun isAttached(): Boolean = listener != null
    @Synchronized fun attachRecordListener(callback: () -> Unit) { recordListener = callback }
    @Synchronized fun detachRecordListener(callback: () -> Unit) { if (recordListener === callback) recordListener = null }
    @Synchronized fun notifyRecordChanged() { recordListener?.invoke() }
}

class GleoActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action?.removePrefix("${context.packageName}.")
        if (action == "ADD_MAINTENANCE_RECORD") {
            val pending = goAsync()
            GleoBackgroundAdd(context.applicationContext, intent) { pending.finish() }.run()
            return
        }
        if (!GleoRequestBridge.isAttached() && action in setOf("GET_MAINTENANCE_RECORD", "GET_MAINTENANCE_DUE")) {
            GleoBackgroundQuery(context.applicationContext, intent, goAsync()).run()
            return
        }
        if (!GleoRequestBridge.enqueue(intent)) {
            context.startActivity(Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
        }
    }
}

class GleoActionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) forward(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        forward(intent)
        finish()
    }

    private fun forward(request: Intent) {
        GleoRequestBridge.enqueue(request)
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        })
    }
}
