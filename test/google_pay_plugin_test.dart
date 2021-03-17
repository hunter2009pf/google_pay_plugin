import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:google_pay_plugin/google_pay_plugin.dart';

void main() {
  const MethodChannel channel = MethodChannel('google_pay_plugin');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await GooglePayPlugin.platformVersion, '42');
  });
}
