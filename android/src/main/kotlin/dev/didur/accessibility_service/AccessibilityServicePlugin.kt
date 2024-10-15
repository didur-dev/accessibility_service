package dev.didur.accessibility_service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.lifecycle.HiddenLifecycleReference
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.util.concurrent.atomic.AtomicBoolean


/** AccessibilityServicePlugin */
class AccessibilityServicePlugin : FlutterPlugin, MethodCallHandler, DefaultLifecycleObserver, ActivityAware {
    private lateinit var channel: MethodChannel

    private lateinit var context: Context

    private var _requestResult: Result? = null

    private var _supportOverlayWindow: Boolean = false

    private lateinit var activity: Activity

    private var permissionStream: EventSink? = null

    private val _isGranted = AtomicBoolean(false)

    private lateinit var accessibilityReceiver: AccessibilityServiceReceive

    companion object {
        private var eventStream: EventSink? = null

        fun sendEvent(event: Any) {
            Handler(Looper.getMainLooper()).post {
                eventStream?.success(event)
            }
        }
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext

        EventChannel(flutterPluginBinding.binaryMessenger, Constants.EVENT_CHANNEL_NAME).setStreamHandler(object : StreamHandler {
            override fun onListen(arguments: Any?, events: EventSink?) {
                configureReceiver(events)
                eventStream = events
            }

            override fun onCancel(arguments: Any?) {
                eventStream = null

                context.unregisterReceiver(accessibilityReceiver)
//                accessibilityReceiver = null
                Log.d(Constants.LOG_TAG, "EventChannel - onCancel")
            }
        })

        EventChannel(flutterPluginBinding.binaryMessenger, Constants.PERMISSION_CHANNEL_NAME).setStreamHandler(object : StreamHandler {
            override fun onListen(arguments: Any?, events: EventSink?) {
                permissionStream = events
            }

            override fun onCancel(arguments: Any?) {
                permissionStream = null
            }
        })

        channel = MethodChannel(flutterPluginBinding.binaryMessenger, Constants.METHOD_CHANNEL_NAME)
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "isGranted" -> result.success(AccessibilityServiceListener.isGranted)
            "requestPermission" -> requestPermission(result)
            "showOverlayWindow" -> showOverlayWindow(result)
            "forceStopApp" -> forceStopApp(call.arguments as Map<*, *>?, result)
            "actionBack" -> result.success(context.back())
            "actionHome" -> result.success(context.home())
            "actionRecent" -> result.success(context.recent())
            "actionPowerDialog" -> result.success(context.powerDialog())
            "actionNotificationBar" -> result.success(context.notificationBar())
            "actionQuickSettings" -> result.success(context.quickSettings())
            "actionLockScreen" -> result.success(context.lockScreen())
            "actionSplitScreen" -> result.success(context.splitScreen())
            "actionScreenshot" -> result.success(context.screenshot())
            "saveScreenshot" -> context.saveScreenshot()
            "actionFindTextAndClick" -> actionFindTextAndClick(call.arguments as Map<*, *>?, result)
            "actionFindTreeIdAndClick" -> actionFindTreeIdAndClick(call.arguments as Map<*, *>?, result)
            "showToast" -> context.showToast(call.argument<String?>("message") ?: "")
            "showToastCustom" -> context.showToastCustom(
                call.argument<String?>("message") ?: "",
                call.argument<Int>("vertical") ?: 1,
                call.argument<Int>("horizontal") ?: 1,
                )
            else -> result.notImplemented()
        }
    }


    private fun requestPermission(result: Result) {
        if (AccessibilityServiceListener.isGranted) {
            result.success(true)
        } else {
            _requestResult = result
            context.requestPermission()
        }
    }

    private fun configureReceiver(events: EventSink?    ){
        /// Set up receiver

        /// Set up receiver
        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.ACCESSIBILITY_INTENT)

        accessibilityReceiver = AccessibilityServiceReceive(events)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(accessibilityReceiver, intentFilter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(accessibilityReceiver, intentFilter)
        }

        /// Set up listener intent

        /// Set up listener intent
        val listenerIntent = Intent(context, AccessibilityServiceListener::class.java)
        context.startService(listenerIntent)
        Log.i("AccessibilityPlugin", "Started the accessibility tracking service.")
    }

    private fun getActivityLifecycle(activityPluginBinding: ActivityPluginBinding): Lifecycle {
        val reference = activityPluginBinding.lifecycle as HiddenLifecycleReference
        return reference.lifecycle
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity

        val lifecycle = getActivityLifecycle(binding)
        lifecycle.addObserver(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {}
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {}
    override fun onDetachedFromActivity() {}

    private fun sendPermissionResult(result: Boolean) {
        _requestResult?.success(result)
        _requestResult = null
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        val isGranted = AccessibilityServiceListener.isGranted

        sendPermissionResult(isGranted)

        if (_isGranted.getAndSet(isGranted) != isGranted) {
            permissionStream?.success(AccessibilityServiceListener.isGranted)
        }
    }

    private fun showOverlayWindow(result: Result) {
        if (!_supportOverlayWindow) {
            result.success(false)
        }
        TODO("Show overlay window")
    }

    private fun forceStopApp(map: Map<*, *>?, result: Result) {
        map?.let {
            try {
                val forceStop = it["forceStop"] as String
                val determine = it["determine"] as String
                val alertDialogName = it["alertDialogName"] as String? ?: Constants.NAME_ALERT_DIALOG
                val appDetailsName = it["appDetailsName"] as String? ?: Constants.NAME_APP_DETAILS
                val found = AtomicBoolean(false)

                val start = System.currentTimeMillis()
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    Log.d(Constants.LOG_TAG, "forceStopApp timeout, used: ${System.currentTimeMillis() - start}ms")
                    AccessibilityServiceListener.setAnalyzeTreeCallback(null)
                    Thread.sleep(100)
                    context.back()
                    result.success(false)
                }, 10000)

                AccessibilityServiceListener.setAnalyzeTreeCallback { event, _ ->
                    if (event.className!! == appDetailsName && found.compareAndSet(false, true)) {
                        if (!event.source.findTextAndClick(forceStop)) {
                            AccessibilityServiceListener.setAnalyzeTreeCallback(null)
                            handler.removeCallbacksAndMessages(null)
                            Thread.sleep(100)
                            context.back()
                            result.success(false)
                            return@setAnalyzeTreeCallback
                        }
                    }
                    if (event.className!! == alertDialogName) {
                        val success = event.source.findTextAndClick(determine)
                        AccessibilityServiceListener.setAnalyzeTreeCallback(null)
                        handler.removeCallbacksAndMessages(null)
                        Thread.sleep(100)
                        context.back()
                        result.success(success)
                        return@setAnalyzeTreeCallback
                    }
                }
                context.openAppSettings(it["name"] as String)
            } catch (e: Throwable) {
                result.success(false)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun actionFindTextAndClick(map: Map<*, *>?, result: Result) {
        map?.let {
            try {
                val packageName = it["packageName"] as String
                val text = it["text"] as String
                val expectedText = it["expectedText"] as String?
                val timeout = (it["timeout"] as Int? ?: 10000).toLong()
                val includeDesc = it["includeDesc"] as Boolean? ?: true
                val match = TextMatchType.from(it["matchType"] as Int?)

                val start = System.currentTimeMillis()
                val handler = Handler(Looper.getMainLooper())

                val success = AtomicBoolean(false)

                handler.postDelayed({
                    Log.d(Constants.LOG_TAG, "actionFindTextAndClick timeout, used: ${System.currentTimeMillis() - start}ms")
                    AccessibilityServiceListener.setAnalyzeTreeCallback(null)
                    result.success(false)
                }, timeout)

                AccessibilityServiceListener.setAnalyzeTreeCallback { event, analyzed ->
                    if (event.packageName == packageName) {
                        // find node and click it
                        val node = analyzed.findNodeByText(text, includeDesc = includeDesc, match = match)
                        var expected = node.click()

                        // find expected node
                        if (expectedText != null) {
                            expected = analyzed.findNodeByText(expectedText, includeDesc = includeDesc, match = match) != null
                        }

                        Log.d(Constants.LOG_TAG, "expected: $expected, findNodeByText: $node, nodes: ${analyzed.nodes.size}")

                        if (expected && success.compareAndSet(false, true)) {
                            result.success(true)
                            handler.removeCallbacksAndMessages(null)
                            AccessibilityServiceListener.setAnalyzeTreeCallback(null)
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(Constants.LOG_TAG, e.message, e)
                result.success(false)
            }
        }
    }

    private fun actionFindTreeIdAndClick(map: Map<*, *>?, result: Result) {
        TODO("Not yet implemented")
    }
}
