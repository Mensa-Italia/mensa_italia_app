import 'package:google_api_headers/google_api_headers.dart';

class MapsApiHeader {
  static final MapsApiHeader _instance = MapsApiHeader._internal();
  factory MapsApiHeader() => _instance;
  MapsApiHeader._internal();

  Map<String, String> headers = {};

  static Future<void> init() async {
    _instance.headers = await GoogleApiHeaders().getHeaders();
  }
}
