package dev.didur.accessibility_service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.flutter.plugin.common.EventChannel.EventSink


class AccessibilityServiceReceive(private val eventSink: EventSink?) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = intent.getStringExtra(Constants.SEND_BROADCAST);
        eventSink?.success(event);
    }

}
