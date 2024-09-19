import 'dart:convert';

import 'package:flutter/services.dart';

import 'accessibility_service_result.dart';

class AccessibilityService {
  static const methodChannel = MethodChannel('dev.didur.driver.app/accessibility/method');

  static const _eventChannel = EventChannel('dev.didur.driver.app/accessibility/event');
  static const _permissionChannel = EventChannel('dev.didur.driver.app/accessibility/permission');

  Future<bool?> requestPermission() async {
    return await methodChannel.invokeMethod<bool>('requestPermission');
  }

  Future<bool?> isGranted() async {
    return await methodChannel.invokeMethod<bool>('isGranted');
  }

  Stream<AccessibilityServiceResult> get onAccessibilityEvent {
    return _eventChannel.receiveBroadcastStream().map((event) {
      return AccessibilityServiceResult.fromJson(jsonDecode(event));
    });
  }

  Stream<bool> get onPermissionChanged {
    return _permissionChannel.receiveBroadcastStream().map((event) => event as bool);
  }

  Future<bool?> forceStopApp(
    String name, {
    String forceStop = 'Force stop',
    String determine = 'Definitive',
    String alertDialogName = 'android.app.AlertDialog',
    String appDetailsName = 'com.android.settings.applications.InstalledAppDetailsTop',
  }) async {
    return await methodChannel.invokeMethod<bool>('forceStopApp', {
      'name': name,
      'forceStop': forceStop,
      'determine': determine,
      'alertDialogName': alertDialogName,
      'appDetailsName': appDetailsName,
    });
  }

  Future<bool?> actionBack() => methodChannel.invokeMethod<bool>('actionBack');

  Future<bool?> actionHome() => methodChannel.invokeMethod<bool>('actionHome');

  Future<bool?> actionRecent() => methodChannel.invokeMethod<bool>('actionRecent');

  Future<bool?> actionPowerDialog() => methodChannel.invokeMethod<bool>('actionPowerDialog');

  Future<bool?> actionNotificationBar() => methodChannel.invokeMethod<bool>('actionNotificationBar');

  Future<bool?> actionQuickSettings() => methodChannel.invokeMethod<bool>('actionQuickSettings');

  Future<bool?> actionLockScreen() => methodChannel.invokeMethod<bool>('actionLockScreen');

  Future<bool?> actionSplitScreen() => methodChannel.invokeMethod<bool>('actionSplitScreen');

  Future<bool?> actionScreenshot() => methodChannel.invokeMethod<bool>('actionScreenshot');

  /// Show toast
  /// @param message [String]
  Future<bool?> showToast(String message) => methodChannel.invokeMethod<bool>('showToast', {
        'message': message,
      });

  /// Show toast
  /// @param message [String]
  /// @param vertical [
  ///  0 Gravity.BOTTOM
  ///  1 Gravity.CENTER_VERTICAL
  ///  2 Gravity.NO_GRAVITY
  ///  3 Gravity.TOP
  /// ]
  /// @param horizontal [
  ///  1 Gravity.LEFT
  ///  2 Gravity.NO_GRAVITY
  ///  3 Gravity.RIGHT
  /// ]
  ///     Gravity.BOTTOM
  //     } else if (vertical == 1) {
  //         Gravity.CENTER_VERTICAL
  //     } else if (vertical != 2) {
  //         Gravity.NO_GRAVITY
  //     } else {
  //         Gravity.TOP
  //     }
  //     if (horizontal != 0) {
  //         i7 = if (horizontal == 1) {
  //             Gravity.LEFT
  //         } else if (horizontal != 2) {
  //             Gravity.NO_GRAVITY
  //         } else {
  //             Gravity.RIGHT
  //         }
  //     }
  Future<bool?> showToastCustom(
    String message, [
    GravityVertical vertical = GravityVertical.top,
    GravityHorizonal horizontal = GravityHorizonal.center,
  ]) =>
      methodChannel.invokeMethod<bool>('showToastCustom', {
        'message': message,
        'vertical': vertical.index,
        'horizontal': horizontal.index,
      });

  /// Find text node and click
  /// if [expectedText] is not null, it will compare with the [expectedText] of the expected node found, equal to return true
  /// if [expectedText] is null, it will find [text] node found, and click it directly
  /// @param match [int] 1: EQUALS, 2: CONTAINS, 3: REGEX
  Future<bool?> actionFindTextAndClick({
    required String packageName,
    required String text,
    String? expectedText,
    int? timeout = 10000,
    bool? includeDesc = true,
    int? match = 1,
  }) =>
      methodChannel.invokeMethod<bool>('actionFindTextAndClick', {
        'packageName': packageName,
        'text': text,
        'expectedText': expectedText,
        'timeout': timeout,
        'includeDesc': includeDesc,
        'match': match,
      });
}

enum GravityVertical { botton, center, top }

enum GravityHorizonal { center, right, left }
