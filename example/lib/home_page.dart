import 'dart:developer';
import 'dart:isolate';
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter_overlay_window/flutter_overlay_window.dart';

class HomePage extends StatefulWidget {
  const HomePage({Key? key}) : super(key: key);

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  static const String _kPortNameOverlay = 'OVERLAY';
  static const String _kPortNameHome = 'UI';
  final _receivePort = ReceivePort();
  SendPort? homePort;
  String? latestMessageFromOverlay;

  @override
  void initState() {
    super.initState();
    if (homePort != null) return;
    final res = IsolateNameServer.registerPortWithName(
      _receivePort.sendPort,
      _kPortNameHome,
    );
    log("$res: OVERLAY");
    _receivePort.listen((message) {
      log("message from OVERLAY: $message");
      setState(() {
        latestMessageFromOverlay = 'Latest Message From Overlay: $message';
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Plugin example app'),
      ),
      body: Center(
        child: ListView(
          children: [
            TextButton(
              onPressed: () async {
                final status = await FlutterOverlayWindow.isPermissionGranted();
                log("Is Permission Granted: $status");
              },
              child: const Text("Check Permission"),
            ),
            const SizedBox(height: 10.0),
            TextButton(
              onPressed: () async {
                final bool? res = await FlutterOverlayWindow.requestPermission();
                log("status: $res");
              },
              child: const Text("Request Permission"),
            ),
            const SizedBox(height: 10.0),
            ..._createButton("overlayMain", ['args1']),
            ..._createButton("overlayMain", ['args2', "'args2'"]),
            ..._createButton("overlayMain2", ["default 1"]),
            ..._createButton("overlayMain2", ["default 2"]),
            const SizedBox(height: 10.0),
            TextButton(
              onPressed: () async {
                final status = await FlutterOverlayWindow.isActive();
                log("Is Active?: $status");
              },
              child: const Text("Is Active?"),
            ),
            const SizedBox(height: 10.0),
            TextButton(
              onPressed: () async {
                await FlutterOverlayWindow.resizeOverlay(
                  WindowSize.matchParent,
                  (MediaQuery.of(context).size.height * 5).toInt(),
                  false,
                );
              },
              child: const Text("Update Overlay"),
            ),
            const SizedBox(height: 10.0),
            TextButton(
              onPressed: () {
                homePort ??= IsolateNameServer.lookupPortByName(_kPortNameOverlay);
                homePort?.send('Send to overlay: ${DateTime.now()}');
              },
              child: const Text("Send message to overlay"),
            ),
            const SizedBox(height: 20.0),
            TextButton(
              onPressed: () {
                FlutterOverlayWindow.getOverlayPosition().then((value) {
                  log('Overlay Position: $value');
                  setState(() {
                    latestMessageFromOverlay = 'Overlay Position: $value';
                  });
                });
              },
              child: const Text("Get overlay position"),
            ),
            const SizedBox(height: 20.0),
            TextButton(
              onPressed: () {
                FlutterOverlayWindow.moveOverlay(
                  const OverlayPosition(0, 0),
                );
              },
              child: const Text("Move overlay position to (0, 0)"),
            ),
            const SizedBox(height: 20),
            Text(latestMessageFromOverlay ?? ''),
          ],
        ),
      ),
    );
  }

  _createButton(String entryPoint, [List<String>? args]) {
    return [
      TextButton(
        onPressed: () async {
          await FlutterOverlayWindow.showOverlay(
            entryPoint: entryPoint,
            engineId: entryPoint,
            args: args,
            enableDrag: true,
            overlayContent: 'Overlay Enabled',
            flag: OverlayFlag.defaultFlag,
            visibility: NotificationVisibility.visibilityPublic,
            positionGravity: PositionGravity.auto,
            height: (MediaQuery.of(context).size.height * 0.6).toInt(),
            width: WindowSize.matchParent,
            startPosition: const OverlayPosition(0, -259),
          );
        },
        child: Text("Show Overlay '$entryPoint'"),
      ),
      TextButton(
        onPressed: () {
          FlutterOverlayWindow.closeOverlay(entryPoint).then((value) => log('STOPPED: value: $value'));
        },
        child: Text("Close Overlay '$entryPoint'"),
      ),
    ];
  }
}
