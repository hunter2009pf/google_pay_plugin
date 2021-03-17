
import 'dart:async';

import 'package:flutter/services.dart';

class GooglePayPlugin {
  static const MethodChannel _channel = const MethodChannel('google_pay_plugin');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<String> get googlePayInit async {
    final String result = await _channel.invokeMethod('initGooglePayment');
    return result;
  }

  static Future<Map> buyOneTimeConsumedProduct(String productId) async{
    if(productId == null || productId.isEmpty) return null;
    final Map result = await _channel.invokeMethod('buyOneTimeConsumedProduct', {'id' : productId});
    return result;
  }

  static Future<Map> subscribeService(String productId) async{
    if(productId == null || productId.isEmpty) return null;
    final Map result = await _channel.invokeMethod('subscribeService', {'id' : productId});
    return result;
  }

  static Future<Map> verifySubscription(String productId) async{
    if(productId == null || productId.isEmpty) return null;
    final Map result = await _channel.invokeMethod('verifySubscription', {'id' : productId});
    return result;
  }

}
