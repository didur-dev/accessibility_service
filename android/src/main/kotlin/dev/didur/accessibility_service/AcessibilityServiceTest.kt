//package dev.didur.accessibility_service
//
//import android.accessibilityservice.AccessibilityService
//import android.annotation.TargetApi
//import android.content.Intent
//import android.graphics.Color
//import android.graphics.PixelFormat
//import android.graphics.Rect
//import android.os.Build
//import android.util.Log
//import android.util.LruCache
//import android.view.Gravity
//import android.view.WindowManager
//import android.view.accessibility.AccessibilityEvent
//import android.view.accessibility.AccessibilityNodeInfo
//import android.view.accessibility.AccessibilityWindowInfo
//import androidx.annotation.RequiresApi
//import com.google.gson.Gson
//import io.flutter.embedding.android.FlutterTextureView
//import io.flutter.embedding.android.FlutterView
//import io.flutter.embedding.engine.FlutterEngineCache
//import slayer.accessibility.service.flutter_accessibility_service.Constants.*
//import slayer.accessibility.service.flutter_accessibility_service.FlutterAccessibilityServicePlugin.CACHED_TAG
//import java.util.function.Function
//import java.util.stream.Collectors
//
//
//class AccessibilityListener : AccessibilityService() {
//    @RequiresApi(api = Build.VERSION_CODES.N)
//    override fun onAccessibilityEvent(accessibilityEvent: AccessibilityEvent) {
//        try {
//            val eventType = accessibilityEvent.eventType
//            val parentNodeInfo = accessibilityEvent.source
//            var windowInfo: AccessibilityWindowInfo? = null
//            val actions: MutableList<Int> = ArrayList()
//            val subNodeActions: MutableList<HashMap<String, Any>> = ArrayList()
//            val traversedNodes = HashSet<AccessibilityNodeInfo>()
//
//            if (parentNodeInfo == null) {
//                return
//            }
//
//            val nodeId = generateNodeId(parentNodeInfo)
//            val packageName = parentNodeInfo.packageName.toString()
//            storeNode(nodeId, parentNodeInfo)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                windowInfo = parentNodeInfo.window
//            }
//            val intent: Intent = Intent(ACCESSIBILITY_INTENT)
//
//            val data = HashMap<String, Any>()
//            data["mapId"] = nodeId
//            data["packageName"] = packageName
//            data["eventType"] = eventType
//            data["actionType"] = accessibilityEvent.action
//            data["eventTime"] = accessibilityEvent.eventTime
//            data["movementGranularity"] = accessibilityEvent.movementGranularity
//
//            val rect = Rect()
//            parentNodeInfo.getBoundsInScreen(rect)
//            data["screenBounds"] = getBoundingPoints(rect)
//
//            data["contentChangeTypes"] = accessibilityEvent.contentChangeTypes
//
//            if (parentNodeInfo.text != null) {
//                data["capturedText"] = parentNodeInfo.text.toString()
//            }
//
//            data["nodeId"] = parentNodeInfo.viewIdResourceName
//
//            getSubNodes(parentNodeInfo, subNodeActions, traversedNodes)
//
//            actions.addAll(parentNodeInfo.actionList.map { it.id })
//            data["parentActions"] = actions
//
//            data["subNodesActions"] = subNodeActions
//            data["isClickable"] = parentNodeInfo.isClickable
//            data["isScrollable"] = parentNodeInfo.isScrollable
//            data["isFocusable"] = parentNodeInfo.isFocusable
//            data["isCheckable"] = parentNodeInfo.isCheckable
//            data["isLongClickable"] = parentNodeInfo.isLongClickable
//            data["isEditable"] = parentNodeInfo.isEditable
//
//            if (windowInfo != null) {
//                data["isActive"] = windowInfo.isActive
//                data["isFocused"] = windowInfo.isFocused
//                data["windowType"] = windowInfo.type
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    data["isPip"] = windowInfo.isInPictureInPictureMode
//                }
//            }
//
//            storeToSharedPrefs(data)
//            intent.putExtra(SEND_BROADCAST, true)
//            sendBroadcast(intent)
//
//        } catch (ex: Exception) {
//            Log.e("EVENT", "onAccessibilityEvent: " + ex.message)
//        }
//    }
//
//    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
//        val globalAction = intent.getBooleanExtra(INTENT_GLOBAL_ACTION, false)
//        val systemActions = intent.getBooleanExtra(INTENT_SYSTEM_GLOBAL_ACTIONS, false)
//        if (systemActions && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            val actions = getSystemActions().map { it.id }
//            val broadcastIntent: Intent = Intent(BROD_SYSTEM_GLOBAL_ACTIONS)
//            broadcastIntent.putIntegerArrayListExtra("actions", ArrayList(actions))
//            sendBroadcast(broadcastIntent)
//        }
//        if (globalAction) {
//            val actionId = intent.getIntExtra(INTENT_GLOBAL_ACTION_ID, 8)
//            performGlobalAction(actionId)
//        }
//        Log.d("CMD_STARTED", "onStartCommand: $startId")
//        return START_STICKY
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.N)
//    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//    fun getSubNodes(node: AccessibilityNodeInfo, arr: MutableList<HashMap<String, Any>>, traversedNodes: HashSet<AccessibilityNodeInfo>) {
//        if (traversedNodes.contains(node)) return
//        traversedNodes.add(node)
//
//        val mapId = generateNodeId(node)
//        var windowInfo: AccessibilityWindowInfo? = null
//        val nested = HashMap<String, Any>()
//        val rect = Rect()
//        node.getBoundsInScreen(rect)
//        windowInfo = node.window
//        nested["mapId"] = mapId
//        nested["nodeId"] = node.viewIdResourceName
//        nested["capturedText"] = node.text
//        nested["screenBounds"] = getBoundingPoints(rect)
//        nested["isClickable"] = node.isClickable
//        nested["isScrollable"] = node.isScrollable
//        nested["isFocusable"] = node.isFocusable
//        nested["isCheckable"] = node.isCheckable
//        nested["isLongClickable"] = node.isLongClickable
//        nested["isEditable"] = node.isEditable
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            nested["parentActions"] =  systemActions.map { it.id }
//        }
//        if (windowInfo != null) {
//            nested["isActive"] = node.window.isActive
//            nested["isFocused"] = node.window.isFocused
//            nested["windowType"] = node.window.type
//        }
//        arr.add(nested)
//        storeNode(mapId, node)
//        for (i in 0 until node.childCount) {
//            val child = node.getChild(i) ?: continue
//            getSubNodes(child, arr, traversedNodes)
//        }
//    }
//
//    private fun getBoundingPoints(rect: Rect): HashMap<String, Int> {
//        val frame = HashMap<String, Int>()
//        frame["left"] = rect.left
//        frame["right"] = rect.right
//        frame["top"] = rect.top
//        frame["bottom"] = rect.bottom
//        frame["width"] = rect.width()
//        frame["height"] = rect.height()
//        return frame
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
//    override fun onServiceConnected() {
//        mWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager
//        mOverlayView = FlutterView(applicationContext, FlutterTextureView(applicationContext))
//        mOverlayView!!.attachToFlutterEngine(FlutterEngineCache.getInstance()[CACHED_TAG]!!)
//        mOverlayView!!.fitsSystemWindows = true
//        mOverlayView!!.isFocusable = true
//        mOverlayView!!.isFocusableInTouchMode = true
//        mOverlayView!!.setBackgroundColor(Color.TRANSPARENT)
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        removeOverlay()
//        val sharedPreferences = getSharedPreferences(SHARED_PREFS_TAG, MODE_PRIVATE)
//        val editor = sharedPreferences.edit()
//        editor.remove(ACCESSIBILITY_NODE).apply()
//    }
//
//    override fun onInterrupt() {}
//    private fun generateNodeId(node: AccessibilityNodeInfo): String {
//        return node.windowId.toString() + "_" + node.className + "_" + node.text + "_" + node.contentDescription //UUID.randomUUID().toString();
//    }
//
//    private fun storeNode(uuid: String, node: AccessibilityNodeInfo?) {
//        if (node == null) {
//            return
//        }
//        nodeMap.put(uuid, node)
//    }
//
//    fun storeToSharedPrefs(data: HashMap<String, Any>?) {
//        val sharedPreferences = getSharedPreferences(SHARED_PREFS_TAG, MODE_PRIVATE)
//        val editor = sharedPreferences.edit()
//        val gson = Gson()
//        val json = gson.toJson(data)
//        editor.putString(ACCESSIBILITY_NODE, json)
//        editor.apply()
//    }
//
//    companion object {
//        private var mWindowManager: WindowManager? = null
//        private var mOverlayView: FlutterView? = null
//        private var isOverlayShown = false
//        private const val CACHE_SIZE = 1000
//        private val nodeMap = LruCache<String, AccessibilityNodeInfo>(CACHE_SIZE)
//        fun getNodeInfo(id: String): AccessibilityNodeInfo {
//            return nodeMap[id]
//        }
//
//        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
//        fun showOverlay() {
//            if (!isOverlayShown) {
//                val lp = WindowManager.LayoutParams()
//                lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
//                lp.format = PixelFormat.TRANSLUCENT
//                lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_FULLSCREEN
//                lp.width = WindowManager.LayoutParams.MATCH_PARENT
//                lp.height = WindowManager.LayoutParams.MATCH_PARENT
//                lp.gravity = Gravity.TOP
//                mWindowManager!!.addView(mOverlayView, lp)
//                isOverlayShown = true
//            }
//        }
//
//        fun removeOverlay() {
//            if (isOverlayShown) {
//                mWindowManager!!.removeView(mOverlayView)
//                isOverlayShown = false
//            }
//        }
//    }
//}