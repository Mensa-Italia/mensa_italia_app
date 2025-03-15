// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'ex_granted_permissions.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

ExGrantedPermissionsModel _$ExGrantedPermissionsModelFromJson(
    Map<String, dynamic> json) {
  return _ExGrantedPermissionsModel.fromJson(json);
}

/// @nodoc
mixin _$ExGrantedPermissionsModel {
  String get id => throw _privateConstructorUsedError;
  String get user => throw _privateConstructorUsedError;
  String get exApp => throw _privateConstructorUsedError;
  List<String> get permissions => throw _privateConstructorUsedError;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get created => throw _privateConstructorUsedError;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get updated => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $ExGrantedPermissionsModelCopyWith<ExGrantedPermissionsModel> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $ExGrantedPermissionsModelCopyWith<$Res> {
  factory $ExGrantedPermissionsModelCopyWith(ExGrantedPermissionsModel value,
          $Res Function(ExGrantedPermissionsModel) then) =
      _$ExGrantedPermissionsModelCopyWithImpl<$Res, ExGrantedPermissionsModel>;
  @useResult
  $Res call(
      {String id,
      String user,
      String exApp,
      List<String> permissions,
      @JsonKey(fromJson: getDateTimeLocal) DateTime created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime updated});
}

/// @nodoc
class _$ExGrantedPermissionsModelCopyWithImpl<$Res,
        $Val extends ExGrantedPermissionsModel>
    implements $ExGrantedPermissionsModelCopyWith<$Res> {
  _$ExGrantedPermissionsModelCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? user = null,
    Object? exApp = null,
    Object? permissions = null,
    Object? created = null,
    Object? updated = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      user: null == user
          ? _value.user
          : user // ignore: cast_nullable_to_non_nullable
              as String,
      exApp: null == exApp
          ? _value.exApp
          : exApp // ignore: cast_nullable_to_non_nullable
              as String,
      permissions: null == permissions
          ? _value.permissions
          : permissions // ignore: cast_nullable_to_non_nullable
              as List<String>,
      created: null == created
          ? _value.created
          : created // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updated: null == updated
          ? _value.updated
          : updated // ignore: cast_nullable_to_non_nullable
              as DateTime,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$ExGrantedPermissionsModelImplCopyWith<$Res>
    implements $ExGrantedPermissionsModelCopyWith<$Res> {
  factory _$$ExGrantedPermissionsModelImplCopyWith(
          _$ExGrantedPermissionsModelImpl value,
          $Res Function(_$ExGrantedPermissionsModelImpl) then) =
      __$$ExGrantedPermissionsModelImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      String user,
      String exApp,
      List<String> permissions,
      @JsonKey(fromJson: getDateTimeLocal) DateTime created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime updated});
}

/// @nodoc
class __$$ExGrantedPermissionsModelImplCopyWithImpl<$Res>
    extends _$ExGrantedPermissionsModelCopyWithImpl<$Res,
        _$ExGrantedPermissionsModelImpl>
    implements _$$ExGrantedPermissionsModelImplCopyWith<$Res> {
  __$$ExGrantedPermissionsModelImplCopyWithImpl(
      _$ExGrantedPermissionsModelImpl _value,
      $Res Function(_$ExGrantedPermissionsModelImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? user = null,
    Object? exApp = null,
    Object? permissions = null,
    Object? created = null,
    Object? updated = null,
  }) {
    return _then(_$ExGrantedPermissionsModelImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      user: null == user
          ? _value.user
          : user // ignore: cast_nullable_to_non_nullable
              as String,
      exApp: null == exApp
          ? _value.exApp
          : exApp // ignore: cast_nullable_to_non_nullable
              as String,
      permissions: null == permissions
          ? _value._permissions
          : permissions // ignore: cast_nullable_to_non_nullable
              as List<String>,
      created: null == created
          ? _value.created
          : created // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updated: null == updated
          ? _value.updated
          : updated // ignore: cast_nullable_to_non_nullable
              as DateTime,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$ExGrantedPermissionsModelImpl extends _ExGrantedPermissionsModel {
  const _$ExGrantedPermissionsModelImpl(
      {required this.id,
      required this.user,
      required this.exApp,
      required final List<String> permissions,
      @JsonKey(fromJson: getDateTimeLocal) required this.created,
      @JsonKey(fromJson: getDateTimeLocal) required this.updated})
      : _permissions = permissions,
        super._();

  factory _$ExGrantedPermissionsModelImpl.fromJson(Map<String, dynamic> json) =>
      _$$ExGrantedPermissionsModelImplFromJson(json);

  @override
  final String id;
  @override
  final String user;
  @override
  final String exApp;
  final List<String> _permissions;
  @override
  List<String> get permissions {
    if (_permissions is EqualUnmodifiableListView) return _permissions;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_permissions);
  }

  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime created;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime updated;

  @override
  String toString() {
    return 'ExGrantedPermissionsModel(id: $id, user: $user, exApp: $exApp, permissions: $permissions, created: $created, updated: $updated)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$ExGrantedPermissionsModelImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.user, user) || other.user == user) &&
            (identical(other.exApp, exApp) || other.exApp == exApp) &&
            const DeepCollectionEquality()
                .equals(other._permissions, _permissions) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode => Object.hash(runtimeType, id, user, exApp,
      const DeepCollectionEquality().hash(_permissions), created, updated);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$ExGrantedPermissionsModelImplCopyWith<_$ExGrantedPermissionsModelImpl>
      get copyWith => __$$ExGrantedPermissionsModelImplCopyWithImpl<
          _$ExGrantedPermissionsModelImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$ExGrantedPermissionsModelImplToJson(
      this,
    );
  }
}

abstract class _ExGrantedPermissionsModel extends ExGrantedPermissionsModel {
  const factory _ExGrantedPermissionsModel(
      {required final String id,
      required final String user,
      required final String exApp,
      required final List<String> permissions,
      @JsonKey(fromJson: getDateTimeLocal) required final DateTime created,
      @JsonKey(fromJson: getDateTimeLocal)
      required final DateTime updated}) = _$ExGrantedPermissionsModelImpl;
  const _ExGrantedPermissionsModel._() : super._();

  factory _ExGrantedPermissionsModel.fromJson(Map<String, dynamic> json) =
      _$ExGrantedPermissionsModelImpl.fromJson;

  @override
  String get id;
  @override
  String get user;
  @override
  String get exApp;
  @override
  List<String> get permissions;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get created;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get updated;
  @override
  @JsonKey(ignore: true)
  _$$ExGrantedPermissionsModelImplCopyWith<_$ExGrantedPermissionsModelImpl>
      get copyWith => throw _privateConstructorUsedError;
}
