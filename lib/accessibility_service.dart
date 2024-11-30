import 'dart:convert';
import 'dart:developer';
import 'dart:io';

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

  static Future<bool> performAction(
    String nodeId,
    NodeAction action, [
    dynamic arguments,
  ]) async {
    try {
      if (action == NodeAction.unknown) return false;
      return await methodChannel.invokeMethod<bool?>(
            'performActionById',
            {
              "nodeId": nodeId,
              "nodeAction": action.id,
              "extras": arguments,
            },
          ) ??
          false;
    } on PlatformException catch (error) {
      log("$error");
      return false;
    }
  }

  Future<Uint8List?> saveScreenshot({int quality = 50}) async {
    var filePath = await methodChannel.invokeMethod<String>('saveScreenshot', {
      'quality': quality,
    });

    if (filePath != null) {
      var file = File(filePath);
      return file.readAsBytesSync();
    }

    return null;
  }

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

enum NodeAction {
  /// Action that gives accessibility focus to the node.
  actionAccessibilityFocus(64),

  /// Action that clears accessibility focus of the node.
  actionClearAccessibilityFocus(128),

  /// Action that clears input focus of the node.
  actionClearFocus(2),

  /// Action that deselects the node.
  actionClearSelection(8),

  /// Action that clicks on the node info. see:
  /// https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo.AccessibilityAction#ACTION_CLICK
  actionClick(16),

  /// Action to collapse an expandable node.
  actionCollapse(524288),

  /// Action to copy the current selection to the clipboard.
  actionCopy(16384),

  /// Action to cut the current selection and place it to the clipboard.
  actionCut(65536),

  /// Action to dismiss a dismissable node.
  actionDismiss(1048576),

  /// Action to expand an expandable node.
  actionExpand(262144),

  /// Action that gives input focus to the node.
  actionFocus(1),

  /// Action that long clicks on the node.
  actionLongClick(32),

  /// Action that requests to go to the next entity in this node's text at a given movement granularity.
  /// For example, move to the next character, word, etc.
  /// pass an argument when you perform an action
  ///
  /// example:
  ///
  /// ```dart
  ///   final status = await FlutterAccessibilityService.performAction(
  ///     frame.nodeId!,
  ///     NodeAction.actionNextAtMovementGranularity,
  ///     false,
  ///  );
  /// ```
  actionNextAtMovementGranularity(256),

  /// Action to move to the next HTML element of a given type. For example, move to the BUTTON, INPUT, TABLE, etc.
  /// pass an argument when you perform an action
  ///
  /// example:
  ///
  /// ```dart
  ///   final status = await FlutterAccessibilityService.performAction(
  ///     frame.nodeId!,
  ///     NodeAction.actionNextHtmlElement,
  ///     "BUTTON",
  ///  );
  /// ```
  actionNextHtmlElement(1024),

  /// Action to paste the current clipboard content.
  actionPaste(32768),

  /// Action that requests to go to the previous entity in this node's text at a given movement granularity.
  /// For example, move to the next character, word, etc.
  /// pass an argument when you perform an action
  ///
  /// example:
  ///
  /// ```dart
  ///   final status = await FlutterAccessibilityService.performAction(
  ///     frame.nodeId!,
  ///     NodeAction.actionPreviousAtMovementGranularity,
  ///     false,
  ///  );
  /// ```
  actionPreviousAtMovementGranularity(512),

  /// Action to move to the previous HTML element of a given type. For example, move to the BUTTON, INPUT, TABLE, etc.
  /// pass an argument when you perform an action
  ///
  /// example:
  ///
  /// ```dart
  ///   final status = await FlutterAccessibilityService.performAction(
  ///     frame.nodeId!,
  ///     NodeAction.actionPreviousHtmlElement,
  ///     "BUTTON",
  ///  );
  /// ```
  actionPreviousHtmlElement(2048),

  /// Action to scroll the node content backward.
  actionScrollBackward(8192),

  /// Action to scroll the node content forward.
  actionScrollForward(4096),

  /// Action that selects the node.
  actionSelect(4),

  /// Action to set the selection. Performing this action with no arguments clears the selection.
  /// pass an argument when you perform an action
  ///
  /// example:
  ///
  /// ```dart
  ///   final status = await FlutterAccessibilityService.performAction(
  ///     frame.nodeId!,
  ///     NodeAction.actionSetSelection,
  ///     {"start": 1, "end": 2},
  ///  );
  /// ```
  actionSetSelection(131072),

  /// Action that sets the text of the node. Performing the action without argument, using null or empty CharSequence will clear the text.
  /// This action will also put the cursor at the end of text.
  /// pass an argument when you perform an action
  ///
  /// example:
  ///
  /// ```dart
  ///   final status = await FlutterAccessibilityService.performAction(
  ///     frame.nodeId!,
  ///     NodeAction.actionSetText,
  ///     "Flutter",
  ///  );
  /// ```
  actionSetText(2097152),

  /// The accessibility focus.
  focusAccessibility(2),

  /// The input focus.
  focusInput(1),

  /// Movement granularity bit for traversing the text of a node by character.
  movementGranularityCharacter(1),

  /// Movement granularity bit for traversing the text of a node by line.
  movementGranularityLine(4),

  /// Movement granularity bit for traversing the text of a node by page.
  movementGranularityPage(16),

  /// Movement granularity bit for traversing the text of a node by paragraph.
  movementGranularityParagraph(8),

  /// Movement granularity bit for traversing the text of a node by word.
  movementGranularityWord(2),

  /// Unknown action
  unknown(null);

  final int? id;

  const NodeAction(this.id);
}
