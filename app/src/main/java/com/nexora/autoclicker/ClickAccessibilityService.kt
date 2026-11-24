package com.nexora.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class ClickAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
    override fun onServiceConnected() { current = this }
    override fun onDestroy() {
        if (current === this) current = null
        super.onDestroy()
    }

    fun performTap(x: Float, y: Float, durationMs: Long, result: (Boolean) -> Unit) {
        val path = Path().apply { moveTo(x, y); lineTo(x + .1f, y + .1f) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(1)))
            .build()
        val accepted = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) { result(true) }
            override fun onCancelled(gestureDescription: GestureDescription?) { result(false) }
        }, null)
        if (!accepted) result(false)
    }

    companion object {
        @Volatile var current: ClickAccessibilityService? = null
            private set
    }
}
