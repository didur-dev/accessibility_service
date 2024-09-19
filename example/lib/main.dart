import 'package:accessibility_service_example/foreground.dart';
import 'package:flutter/material.dart';
import 'package:accessibility_service/accessibility_service.dart';

import 'foreground.page.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeService();
  runApp(const MyApp());
}

void main2() {
  WidgetsFlutterBinding.ensureInitialized();

  runApp(const MyApp2());
}

class MyApp2 extends StatefulWidget {
  const MyApp2({super.key});

  @override
  State<MyApp2> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp2> {
  final _plugin = AccessibilityService();

  @override
  void initState() {
    super.initState();

    _plugin.onAccessibilityEvent.listen((event) {
      print('$event');
    });

    _plugin.onPermissionChanged.listen((isGranted) {
      print('isGranted: $isGranted');
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Flutter A11y Service Example'),
        ),
        body: SafeArea(
          child: Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              mainAxisSize: MainAxisSize.min,
              children: [
                ElevatedButton(
                  onPressed: () async {
                    final isGranted = await _plugin.requestPermission();
                    print('isGranted: $isGranted');
                  },
                  child: const Text('Request Permission'),
                ),
                ElevatedButton(
                  onPressed: () async {
                    final forceStopApp = await _plugin.forceStopApp(
                      'com.android.chrome',
                      determine: '确定',
                      alertDialogName: 'androidx.appcompat.app.AlertDialog',
                      appDetailsName: 'com.android.settings.applications.InstalledAppDetailsTop',
                    );
                    print('forceStopApp: $forceStopApp');
                  },
                  child: const Text('Force stop app'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
