// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'event_owner.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

EventOwnerModel _$EventOwnerModelFromJson(Map<String, dynamic> json) {
  return _EventOwnerModel.fromJson(json);
}

/// @nodoc
mixin _$EventOwnerModel {
  String get id => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String get email => throw _privateConstructorUsedError;
  String get avatar => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $EventOwnerModelCopyWith<EventOwnerModel> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $EventOwnerModelCopyWith<$Res> {
  factory $EventOwnerModelCopyWith(
          EventOwnerModel value, $Res Function(EventOwnerModel) then) =
      _$EventOwnerModelCopyWithImpl<$Res, EventOwnerModel>;
  @useResult
  $Res call({String id, String name, String email, String avatar});
}

/// @nodoc
class _$EventOwnerModelCopyWithImpl<$Res, $Val extends EventOwnerModel>
    implements $EventOwnerModelCopyWith<$Res> {
  _$EventOwnerModelCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? email = null,
    Object? avatar = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      email: null == email
          ? _value.email
          : email // ignore: cast_nullable_to_non_nullable
              as String,
      avatar: null == avatar
          ? _value.avatar
          : avatar // ignore: cast_nullable_to_non_nullable
              as String,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$EventOwnerModelImplCopyWith<$Res>
    implements $EventOwnerModelCopyWith<$Res> {
  factory _$$EventOwnerModelImplCopyWith(_$EventOwnerModelImpl value,
          $Res Function(_$EventOwnerModelImpl) then) =
      __$$EventOwnerModelImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({String id, String name, String email, String avatar});
}

/// @nodoc
class __$$EventOwnerModelImplCopyWithImpl<$Res>
    extends _$EventOwnerModelCopyWithImpl<$Res, _$EventOwnerModelImpl>
    implements _$$EventOwnerModelImplCopyWith<$Res> {
  __$$EventOwnerModelImplCopyWithImpl(
      _$EventOwnerModelImpl _value, $Res Function(_$EventOwnerModelImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? email = null,
    Object? avatar = null,
  }) {
    return _then(_$EventOwnerModelImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      email: null == email
          ? _value.email
          : email // ignore: cast_nullable_to_non_nullable
              as String,
      avatar: null == avatar
          ? _value.avatar
          : avatar // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$EventOwnerModelImpl implements _EventOwnerModel {
  const _$EventOwnerModelImpl(
      {required this.id,
      required this.name,
      required this.email,
      required this.avatar});

  factory _$EventOwnerModelImpl.fromJson(Map<String, dynamic> json) =>
      _$$EventOwnerModelImplFromJson(json);

  @override
  final String id;
  @override
  final String name;
  @override
  final String email;
  @override
  final String avatar;

  @override
  String toString() {
    return 'EventOwnerModel(id: $id, name: $name, email: $email, avatar: $avatar)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$EventOwnerModelImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.email, email) || other.email == email) &&
            (identical(other.avatar, avatar) || other.avatar == avatar));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode => Object.hash(runtimeType, id, name, email, avatar);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$EventOwnerModelImplCopyWith<_$EventOwnerModelImpl> get copyWith =>
      __$$EventOwnerModelImplCopyWithImpl<_$EventOwnerModelImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$EventOwnerModelImplToJson(
      this,
    );
  }
}

abstract class _EventOwnerModel implements EventOwnerModel {
  const factory _EventOwnerModel(
      {required final String id,
      required final String name,
      required final String email,
      required final String avatar}) = _$EventOwnerModelImpl;

  factory _EventOwnerModel.fromJson(Map<String, dynamic> json) =
      _$EventOwnerModelImpl.fromJson;

  @override
  String get id;
  @override
  String get name;
  @override
  String get email;
  @override
  String get avatar;
  @override
  @JsonKey(ignore: true)
  _$$EventOwnerModelImplCopyWith<_$EventOwnerModelImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
