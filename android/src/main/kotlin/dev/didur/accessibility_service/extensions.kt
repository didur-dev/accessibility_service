package dev.didur.accessibility_service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.Html
import android.text.Spanned
import android.view.Gravity
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.emoji2.text.EmojiCompat
import es.dmoral.toasty.Toasty


fun Context.performAction(action: Int) = require().performGlobalAction(action)

// 返回
fun Context.back() = performAction(AccessibilityService.GLOBAL_ACTION_BACK)

// Home键
fun Context.home() = performAction(AccessibilityService.GLOBAL_ACTION_HOME)

// 最近任务
fun Context.recent() = performAction(AccessibilityService.GLOBAL_ACTION_RECENTS)

// 电源菜单
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Context.powerDialog() = performAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)

// 通知栏
fun Context.notificationBar() = performAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)

// 通知栏 → 快捷设置
fun Context.quickSettings() = performAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)

// 锁屏
@RequiresApi(Build.VERSION_CODES.P)
fun Context.lockScreen() = performAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)

// 应用分屏
@RequiresApi(Build.VERSION_CODES.N)
fun Context.splitScreen() = performAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)

// Screenshot
@RequiresApi(Build.VERSION_CODES.P)
fun Context.screenshot() = performAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)

fun CharSequence?.nullableString() = if (this.isNullOrBlank()) "" else this.toString()

fun String?.nullableString() = if (this.isNullOrBlank()) "" else this

fun Context.requestPermission() {
    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

fun Context.require() = run { if (!AccessibilityServiceListener.isGranted) requestPermission(); AccessibilityServiceListener.instance!! }

fun Context.openAppSettings(name: String) {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", name, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

fun Context.showToast(message: String) {
    Toast.makeText(this, message.toSpanned(), Toast.LENGTH_LONG).show();
}

fun Context.showToastCustom(message: String, vertical: Int, horizontal: Int) {
    var toast: Toast? = null

    var fromHtml: CharSequence? = Html.fromHtml(message)
    try {
        fromHtml = EmojiCompat.get().process(fromHtml)
    } catch (_: Exception) {
    }

//    Toasty.Config.getInstance().setTextColor(i7).tintIcon(z4).apply()

    toast = Toasty.custom(this, fromHtml!!, null, 1, false)
    setGravity(this, toast, vertical, horizontal)
    toast.show()
}

fun setGravity(context: Context, toast: Toast, vertical: Int, horizontal: Int) {
   // val dimensionPixelOffset: Int = context.resources.getDimensionPixelOffset(R.dimen.toast_margin)

    var i7 = 1
    val i6: Int = if (vertical == 0) {
        Gravity.BOTTOM
    } else if (vertical == 1) {
        Gravity.CENTER_VERTICAL
    } else if (vertical != 2) {
        Gravity.NO_GRAVITY
    } else {
        Gravity.TOP
    }
    if (horizontal != 0) {
        i7 = if (horizontal == 1) {
            Gravity.LEFT
        } else if (horizontal != 2) {
            Gravity.NO_GRAVITY
        } else {
            Gravity.RIGHT
        }
    }
    toast.setGravity(i6 or i7, 0, 0)
}

fun String.toSpanned(): Spanned {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        return Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)
    } else {
        @Suppress("DEPRECATION")
        return Html.fromHtml(this)
    }
}


fun List<AccessibilityNodeInfo>?.click(): Boolean {
    if (this == null) return false
    for (node in this) {
        if (node.isClickable && node.isEnabled) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }
    return false
}

fun AccessibilityNodeInfo?.findTextAndClick(text: String): Boolean {
    this?.let {
        val nodes = this.findAccessibilityNodeInfosByText(text)
        for (node in nodes) {
            if (node.isClickable && node.isEnabled) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
    }

    return false
}