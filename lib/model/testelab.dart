import 'package:freezed_annotation/freezed_annotation.dart';

part 'testelab.freezed.dart';

part 'testelab.g.dart';

@freezed
abstract class TestelabModel with _$TestelabModel {
  const TestelabModel._();

  const factory TestelabModel({
    required String id,
    required String fullname,
    required String typeOfTest,
    required String modality,
    required String status,
    required String state,
  }) = _TestelabModel;

  factory TestelabModel.fromJson(Map<String, dynamic> json) =>
      _$TestelabModelFromJson(json);

  String getAvailableModality() {
    if (modality.isEmpty) {
      return "Test A/B";
    }
    return modality;
  }
}
