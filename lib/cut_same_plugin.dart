import 'dart:async';

import 'package:flutter/services.dart';

class CutSamePlugin {
  static const MethodChannel _channel = MethodChannel('cut_same_plugin');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<void> get startCutSamePage async {
    await _channel.invokeMethod('startCutSamePage');
  }
}
