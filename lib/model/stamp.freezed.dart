// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'stamp.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

StampModel _$StampModelFromJson(Map<String, dynamic> json) {
  return _StampModel.fromJson(json);
}

/// @nodoc
mixin _$StampModel {
  String get id => throw _privateConstructorUsedError;
  DateTime get created => throw _privateConstructorUsedError;
  DateTime get updated => throw _privateConstructorUsedError;
  String get description => throw _privateConstructorUsedError;
  String get image => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $StampModelCopyWith<StampModel> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $StampModelCopyWith<$Res> {
  factory $StampModelCopyWith(
          StampModel value, $Res Function(StampModel) then) =
      _$StampModelCopyWithImpl<$Res, StampModel>;
  @useResult
  $Res call(
      {String id,
      DateTime created,
      DateTime updated,
      String description,
      String image});
}

/// @nodoc
class _$StampModelCopyWithImpl<$Res, $Val extends StampModel>
    implements $StampModelCopyWith<$Res> {
  _$StampModelCopyWithImpl(this._value, this._then);

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
    Object? description = null,
    Object? image = null,
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
      description: null == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      image: null == image
          ? _value.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$StampModelImplCopyWith<$Res>
    implements $StampModelCopyWith<$Res> {
  factory _$$StampModelImplCopyWith(
          _$StampModelImpl value, $Res Function(_$StampModelImpl) then) =
      __$$StampModelImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      DateTime created,
      DateTime updated,
      String description,
      String image});
}

/// @nodoc
class __$$StampModelImplCopyWithImpl<$Res>
    extends _$StampModelCopyWithImpl<$Res, _$StampModelImpl>
    implements _$$StampModelImplCopyWith<$Res> {
  __$$StampModelImplCopyWithImpl(
      _$StampModelImpl _value, $Res Function(_$StampModelImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? created = null,
    Object? updated = null,
    Object? description = null,
    Object? image = null,
  }) {
    return _then(_$StampModelImpl(
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
      description: null == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      image: null == image
          ? _value.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$StampModelImpl implements _StampModel {
  const _$StampModelImpl(
      {required this.id,
      required this.created,
      required this.updated,
      required this.description,
      required this.image});

  factory _$StampModelImpl.fromJson(Map<String, dynamic> json) =>
      _$$StampModelImplFromJson(json);

  @override
  final String id;
  @override
  final DateTime created;
  @override
  final DateTime updated;
  @override
  final String description;
  @override
  final String image;

  @override
  String toString() {
    return 'StampModel(id: $id, created: $created, updated: $updated, description: $description, image: $image)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$StampModelImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.image, image) || other.image == image));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode =>
      Object.hash(runtimeType, id, created, updated, description, image);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$StampModelImplCopyWith<_$StampModelImpl> get copyWith =>
      __$$StampModelImplCopyWithImpl<_$StampModelImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$StampModelImplToJson(
      this,
    );
  }
}

abstract class _StampModel implements StampModel {
  const factory _StampModel(
      {required final String id,
      required final DateTime created,
      required final DateTime updated,
      required final String description,
      required final String image}) = _$StampModelImpl;

  factory _StampModel.fromJson(Map<String, dynamic> json) =
      _$StampModelImpl.fromJson;

  @override
  String get id;
  @override
  DateTime get created;
  @override
  DateTime get updated;
  @override
  String get description;
  @override
  String get image;
  @override
  @JsonKey(ignore: true)
  _$$StampModelImplCopyWith<_$StampModelImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
