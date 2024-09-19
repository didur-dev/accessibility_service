//package dev.didur.accessibility_service.model
//
//import android.graphics.Rect
//import android.os.Build
//import android.view.accessibility.AccessibilityNodeInfo
//import dev.didur.accessibility_service.nullableString
//
//
//data class EventWrapper(
//        var mapId: String,
//        var packageName: String,
//        var eventType: String,
//        var actionType: String,
//        var eventTime: String,
//        var movementGranularity: String,
//        var screenBounds: String,
//        var contentChangeTypes: String,
//        var capturedText: String,
//        var nodeId: String,
//        var nodesText: String,
//
//        var parentActions: String,
//        var subNodesActions: String,
//        var isClickable: String,
//        var isScrollable: String,
//        var isFocusable: String,
//        var isCheckable: String,
//        var isLongClickable: String,
//        var isEditable: String,
//
//        var isActive: String,
//        var isFocused: String,
//        var windowType: String,
//        var isPip: String,
//) {
//    override fun toString() =
//            "$depth : ${info.packageName} / ${info.className} { ${info.viewIdResourceName} | ${info.text} | ${info.contentDescription} | $bounds | ${info.isClickable} | ${info.isScrollable} | ${info.isEditable} }"
//
//    fun toMap() = mapOf(
//            "depth" to depth,
//            "bounds" to bounds?.let {
//                listOf(it.left, it.top, it.right, it.bottom)
//            },
//            "id" to info.viewIdResourceName.nullableString(),
//            "text" to info.text.nullableString(),
//            "className" to info.className.nullableString(),
//            "packageName" to info.packageName.nullableString(),
//            "description" to info.contentDescription.nullableString(),
//            "clickable" to info.isClickable,
//            "scrollable" to info.isScrollable,
//            "editable" to info.isEditable,
//    )
//}
//
