package dev.didur.accessibility_service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import io.flutter.embedding.android.FlutterView
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class AccessibilityServiceListener : AccessibilityService() {
    private val executor: ExecutorService = Executors.newFixedThreadPool(4)

    // var callback: ((AccessibilityEvent?, AnalyzedResult) -> Unit)? = null

    companion object {
        // Exported Accessibility Service Instance
        var instance: AccessibilityServiceListener? = null

        // Is Accessibility Service Granted
        val isGranted: Boolean get() = instance != null

        private var analyzeTreeCallback: ((AccessibilityEvent, AnalyzedResult) -> Unit)? = null

        fun setAnalyzeTreeCallback(callback: ((AccessibilityEvent, AnalyzedResult) -> Unit)?) {
            analyzeTreeCallback = callback
        }

        private var mWindowManager: WindowManager? = null
        private var mOverlayView: FlutterView? = null
    }

    // Get Root Node, if get error, return null
    override fun getRootInActiveWindow() = try {
        super.getRootInActiveWindow()
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()

        instance = null
        executor.shutdown()
        Log.d(Constants.LOG_TAG, "onDestroy")
    }

    override fun onInterrupt() {
        Log.d(Constants.LOG_TAG, "onInterrupt")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            val className = it.className.nullableString()
            val packageName = it.packageName.nullableString()
            val eventType = it.eventType
            val text = it.text.joinToString(separator = " ~~ ")
            val description = it.contentDescription.nullableString()
            val source = event.source

            if (className.isNotBlank() && packageName.isNotBlank()) {
                executor.execute {
                    // Thread.sleep(100)
//                    val start = System.currentTimeMillis()

                    val result = analyze(source, EventWrapper(packageName, className, mapEventType(eventType)))

                    val intent: Intent = Intent(Constants.ACCESSIBILITY_INTENT)
                    intent.putExtra(Constants.SEND_BROADCAST, Gson().toJson(result.toMap()))
                    sendBroadcast(intent)

//                    Log.d(Constants.LOG_TAG, "analyze tree cost ${System.currentTimeMillis() - start}ms")
                }
            }
        }
    }

    fun mapEventType(eventType: Int?) : String{
        var value = "";
        when (eventType){
            AccessibilityEvent.TYPE_VIEW_CLICKED -> value = "TYPE_VIEW_CLICKED"
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> value = "TYPE_VIEW_LONG_CLICKED"
            AccessibilityEvent.TYPE_VIEW_SELECTED -> value = "TYPE_VIEW_SELECTED"
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> value = "TYPE_VIEW_FOCUSED"
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> value = "TYPE_VIEW_TEXT_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> value = "TYPE_WINDOW_STATE_CHANGED"
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> value = "TYPE_NOTIFICATION_STATE_CHANGED"
            AccessibilityEvent.TYPE_VIEW_HOVER_ENTER -> value = "TYPE_VIEW_HOVER_ENTER"
            AccessibilityEvent.TYPE_VIEW_HOVER_EXIT -> value = "TYPE_VIEW_HOVER_EXIT"
            AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START -> value = "TYPE_TOUCH_EXPLORATION_GESTURE_START"
            AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END -> value = "TYPE_TOUCH_EXPLORATION_GESTURE_END"
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> value = "TYPE_WINDOW_CONTENT_CHANGED"
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> value = "TYPE_VIEW_SCROLLED"
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> value = "TYPE_VIEW_TEXT_SELECTION_CHANGED"
            AccessibilityEvent.TYPE_ANNOUNCEMENT -> value = "TYPE_ANNOUNCEMENT"
            AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY -> value = "TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY"
            AccessibilityEvent.TYPE_GESTURE_DETECTION_START -> value = "TYPE_GESTURE_DETECTION_START"
            AccessibilityEvent.TYPE_GESTURE_DETECTION_END -> value = "TYPE_GESTURE_DETECTION_END"
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> value = "TYPE_TOUCH_INTERACTION_START"
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_END -> value = "TYPE_TOUCH_INTERACTION_END"
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> value = "TYPE_WINDOWS_CHANGED"
            AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED -> value = "TYPE_VIEW_CONTEXT_CLICKED"
        }

        return value;
    }




    fun analyze(source: AccessibilityNodeInfo?, event: EventWrapper? = null): AnalyzedResult {
        val result = AnalyzedResult(event = event)
        analyzeTree(source, result)
        return result
    }

    private fun analyzeTree(node: AccessibilityNodeInfo?, list: AnalyzedResult, depth: List<Int>? = null) {
        if (node == null) return

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val trace = depth ?: listOf(0)
        val data = NodeData(
            depth = trace,
            info = node,
            bounds = bounds,
        )

        list.nodes[trace.joinToString("-")] = data

        if (node.childCount > 0) {
            for (i in 0 until node.childCount) {
                analyzeTree(node.getChild(i), list, trace + i)
            }
        }
    }
}

