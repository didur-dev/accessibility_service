package dev.didur.accessibility_service

class Constants {
    companion object {
        const val LOG_TAG = "ACCESSIBILITY-SERVICE"

        const val ACCESSIBILITY_INTENT = "dev.didur.driver.app/accessibility_event"
        const val SEND_BROADCAST = "dev.didur.driver.app/SEND_BROADCAST"

        const val METHOD_CHANNEL_NAME = "dev.didur.driver.app/accessibility/method"

        const val EVENT_CHANNEL_NAME = "dev.didur.driver.app/accessibility/event"

        const val PERMISSION_CHANNEL_NAME = "dev.didur.driver.app/accessibility/permission"

        const val TOUCH = "TOUCH"

        val NAME_SETTINGS_PACKAGE: CharSequence = "com.android.settings"

        val NAME_APP_DETAILS: CharSequence = "com.android.settings.applications.InstalledAppDetailsTop"

        val NAME_ALERT_DIALOG: CharSequence = "android.app.AlertDialog"
    }
}