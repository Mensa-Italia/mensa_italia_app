import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/model/stamp.dart';

part 'stamp_user.freezed.dart';

part 'stamp_user.g.dart';

@freezed
class StampUserModel with _$StampUserModel {
  const StampUserModel._();
  const factory StampUserModel({
    required String id,
    required DateTime created,
    required DateTime updated,
    @JsonKey(
      readValue: getDataFromExpanded,
    )
    required StampModel stamp,
    required String user,
  }) = _StampUserModel;

  factory StampUserModel.fromJson(Map<String, dynamic> json) => _$StampUserModelFromJson(json);

  int fastHash() {
    var hash = 0xcbf29ce484222325;

    var i = 0;
    while (i < id.length) {
      final codeUnit = id.codeUnitAt(i++);
      hash ^= codeUnit >> 8;
      hash *= 0x100000001b3;
      hash ^= codeUnit & 0xFF;
      hash *= 0x100000001b3;
    }

    return hash;
  }
}
