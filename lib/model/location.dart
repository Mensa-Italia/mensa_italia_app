import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:maplibre_gl/maplibre_gl.dart';
import 'package:latlong2/latlong.dart' as latlong2;

part 'location.freezed.dart';

part 'location.g.dart';

@freezed
class LocationModel with _$LocationModel {
  const LocationModel._();

  const factory LocationModel({
    required String id,
    required String name,
    required double lat,
    required double lon,
  }) = _LocationModel;

  factory LocationModel.fromJson(Map<String, dynamic> json) => _$LocationModelFromJson(json);

  LatLng toLatLng() {
    return LatLng(lat, lon);
  }

  latlong2.LatLng toLatLong2() {
    return latlong2.LatLng(lat, lon);
  }
}
