import 'package:flutter/material.dart';
import 'package:flutter_overlay_window_example/home_page.dart';
import 'package:flutter_overlay_window_example/overlays/true_caller_overlay.dart';

import 'overlays/messanger_chathead.dart';
import 'overlays/text_field_overlay.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

@pragma("vm:entry-point")
void overlayMain(List<String>? args) {
  print("********** $args");
  WidgetsFlutterBinding.ensureInitialized();
  runApp(
    MaterialApp(
      debugShowCheckedModeBanner: false,
      home: TrueCallerOverlay(args: args),
    ),
  );
}

@pragma("vm:entry-point")
void overlayMain2(List<String>? args) {
  print("********** $args");
  WidgetsFlutterBinding.ensureInitialized();
  runApp(
    MaterialApp(
      debugShowCheckedModeBanner: false,
      home: TextFieldOverlay(args),
    ),
  );
}

@pragma("vm:entry-point")
void overlayMain3(List<String>? args) {
  print("********** $args");
  WidgetsFlutterBinding.ensureInitialized();
  runApp(
    const MaterialApp(
      debugShowCheckedModeBanner: false,
      home: MessangerChatHead(),
    ),
  );
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      debugShowCheckedModeBanner: false,
      home: HomePage(),
    );
  }
}
