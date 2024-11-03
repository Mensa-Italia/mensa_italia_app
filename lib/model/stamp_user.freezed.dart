// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'stamp_user.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

StampUserModel _$StampUserModelFromJson(Map<String, dynamic> json) {
  return _StampUserModel.fromJson(json);
}

/// @nodoc
mixin _$StampUserModel {
  String get id => throw _privateConstructorUsedError;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get created => throw _privateConstructorUsedError;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get updated => throw _privateConstructorUsedError;
  @JsonKey(readValue: getDataFromExpanded)
  StampModel get stamp => throw _privateConstructorUsedError;
  String get user => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $StampUserModelCopyWith<StampUserModel> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $StampUserModelCopyWith<$Res> {
  factory $StampUserModelCopyWith(
          StampUserModel value, $Res Function(StampUserModel) then) =
      _$StampUserModelCopyWithImpl<$Res, StampUserModel>;
  @useResult
  $Res call(
      {String id,
      @JsonKey(fromJson: getDateTimeLocal) DateTime created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime updated,
      @JsonKey(readValue: getDataFromExpanded) StampModel stamp,
      String user});

  $StampModelCopyWith<$Res> get stamp;
}

/// @nodoc
class _$StampUserModelCopyWithImpl<$Res, $Val extends StampUserModel>
    implements $StampUserModelCopyWith<$Res> {
  _$StampUserModelCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? created = null,
    Object? updated = null,
    Object? stamp = null,
    Object? user = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      created: null == created
          ? _value.created
          : created // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updated: null == updated
          ? _value.updated
          : updated // ignore: cast_nullable_to_non_nullable
              as DateTime,
      stamp: null == stamp
          ? _value.stamp
          : stamp // ignore: cast_nullable_to_non_nullable
              as StampModel,
      user: null == user
          ? _value.user
          : user // ignore: cast_nullable_to_non_nullable
              as String,
    ) as $Val);
  }

  @override
  @pragma('vm:prefer-inline')
  $StampModelCopyWith<$Res> get stamp {
    return $StampModelCopyWith<$Res>(_value.stamp, (value) {
      return _then(_value.copyWith(stamp: value) as $Val);
    });
  }
}

/// @nodoc
abstract class _$$StampUserModelImplCopyWith<$Res>
    implements $StampUserModelCopyWith<$Res> {
  factory _$$StampUserModelImplCopyWith(_$StampUserModelImpl value,
          $Res Function(_$StampUserModelImpl) then) =
      __$$StampUserModelImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      @JsonKey(fromJson: getDateTimeLocal) DateTime created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime updated,
      @JsonKey(readValue: getDataFromExpanded) StampModel stamp,
      String user});

  @override
  $StampModelCopyWith<$Res> get stamp;
}

/// @nodoc
class __$$StampUserModelImplCopyWithImpl<$Res>
    extends _$StampUserModelCopyWithImpl<$Res, _$StampUserModelImpl>
    implements _$$StampUserModelImplCopyWith<$Res> {
  __$$StampUserModelImplCopyWithImpl(
      _$StampUserModelImpl _value, $Res Function(_$StampUserModelImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? created = null,
    Object? updated = null,
    Object? stamp = null,
    Object? user = null,
  }) {
    return _then(_$StampUserModelImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      created: null == created
          ? _value.created
          : created // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updated: null == updated
          ? _value.updated
          : updated // ignore: cast_nullable_to_non_nullable
              as DateTime,
      stamp: null == stamp
          ? _value.stamp
          : stamp // ignore: cast_nullable_to_non_nullable
              as StampModel,
      user: null == user
          ? _value.user
          : user // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$StampUserModelImpl extends _StampUserModel {
  const _$StampUserModelImpl(
      {required this.id,
      @JsonKey(fromJson: getDateTimeLocal) required this.created,
      @JsonKey(fromJson: getDateTimeLocal) required this.updated,
      @JsonKey(readValue: getDataFromExpanded) required this.stamp,
      required this.user})
      : super._();

  factory _$StampUserModelImpl.fromJson(Map<String, dynamic> json) =>
      _$$StampUserModelImplFromJson(json);

  @override
  final String id;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime created;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime updated;
  @override
  @JsonKey(readValue: getDataFromExpanded)
  final StampModel stamp;
  @override
  final String user;

  @override
  String toString() {
    return 'StampUserModel(id: $id, created: $created, updated: $updated, stamp: $stamp, user: $user)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$StampUserModelImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated) &&
            (identical(other.stamp, stamp) || other.stamp == stamp) &&
            (identical(other.user, user) || other.user == user));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode =>
      Object.hash(runtimeType, id, created, updated, stamp, user);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$StampUserModelImplCopyWith<_$StampUserModelImpl> get copyWith =>
      __$$StampUserModelImplCopyWithImpl<_$StampUserModelImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$StampUserModelImplToJson(
      this,
    );
  }
}

abstract class _StampUserModel extends StampUserModel {
  const factory _StampUserModel(
      {required final String id,
      @JsonKey(fromJson: getDateTimeLocal) required final DateTime created,
      @JsonKey(fromJson: getDateTimeLocal) required final DateTime updated,
      @JsonKey(readValue: getDataFromExpanded) required final StampModel stamp,
      required final String user}) = _$StampUserModelImpl;
  const _StampUserModel._() : super._();

  factory _StampUserModel.fromJson(Map<String, dynamic> json) =
      _$StampUserModelImpl.fromJson;

  @override
  String get id;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get created;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get updated;
  @override
  @JsonKey(readValue: getDataFromExpanded)
  StampModel get stamp;
  @override
  String get user;
  @override
  @JsonKey(ignore: true)
  _$$StampUserModelImplCopyWith<_$StampUserModelImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
