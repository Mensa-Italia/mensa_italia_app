// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'user.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

UserModel _$UserModelFromJson(Map<String, dynamic> json) {
  return _UserModel.fromJson(json);
}

/// @nodoc
mixin _$UserModel {
  String get id => throw _privateConstructorUsedError;
  String get username => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String get avatar => throw _privateConstructorUsedError;
  String get email => throw _privateConstructorUsedError;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get expireMembership => throw _privateConstructorUsedError;
  List<String> get powers => throw _privateConstructorUsedError;
  List<String> get addons => throw _privateConstructorUsedError;
  bool get isMembershipActive => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $UserModelCopyWith<UserModel> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $UserModelCopyWith<$Res> {
  factory $UserModelCopyWith(UserModel value, $Res Function(UserModel) then) =
      _$UserModelCopyWithImpl<$Res, UserModel>;
  @useResult
  $Res call(
      {String id,
      String username,
      String name,
      String avatar,
      String email,
      @JsonKey(fromJson: getDateTimeLocal) DateTime expireMembership,
      List<String> powers,
      List<String> addons,
      bool isMembershipActive});
}

/// @nodoc
class _$UserModelCopyWithImpl<$Res, $Val extends UserModel>
    implements $UserModelCopyWith<$Res> {
  _$UserModelCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? username = null,
    Object? name = null,
    Object? avatar = null,
    Object? email = null,
    Object? expireMembership = null,
    Object? powers = null,
    Object? addons = null,
    Object? isMembershipActive = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      username: null == username
          ? _value.username
          : username // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      avatar: null == avatar
          ? _value.avatar
          : avatar // ignore: cast_nullable_to_non_nullable
              as String,
      email: null == email
          ? _value.email
          : email // ignore: cast_nullable_to_non_nullable
              as String,
      expireMembership: null == expireMembership
          ? _value.expireMembership
          : expireMembership // ignore: cast_nullable_to_non_nullable
              as DateTime,
      powers: null == powers
          ? _value.powers
          : powers // ignore: cast_nullable_to_non_nullable
              as List<String>,
      addons: null == addons
          ? _value.addons
          : addons // ignore: cast_nullable_to_non_nullable
              as List<String>,
      isMembershipActive: null == isMembershipActive
          ? _value.isMembershipActive
          : isMembershipActive // ignore: cast_nullable_to_non_nullable
              as bool,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$UserModelImplCopyWith<$Res>
    implements $UserModelCopyWith<$Res> {
  factory _$$UserModelImplCopyWith(
          _$UserModelImpl value, $Res Function(_$UserModelImpl) then) =
      __$$UserModelImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      String username,
      String name,
      String avatar,
      String email,
      @JsonKey(fromJson: getDateTimeLocal) DateTime expireMembership,
      List<String> powers,
      List<String> addons,
      bool isMembershipActive});
}

/// @nodoc
class __$$UserModelImplCopyWithImpl<$Res>
    extends _$UserModelCopyWithImpl<$Res, _$UserModelImpl>
    implements _$$UserModelImplCopyWith<$Res> {
  __$$UserModelImplCopyWithImpl(
      _$UserModelImpl _value, $Res Function(_$UserModelImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? username = null,
    Object? name = null,
    Object? avatar = null,
    Object? email = null,
    Object? expireMembership = null,
    Object? powers = null,
    Object? addons = null,
    Object? isMembershipActive = null,
  }) {
    return _then(_$UserModelImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      username: null == username
          ? _value.username
          : username // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      avatar: null == avatar
          ? _value.avatar
          : avatar // ignore: cast_nullable_to_non_nullable
              as String,
      email: null == email
          ? _value.email
          : email // ignore: cast_nullable_to_non_nullable
              as String,
      expireMembership: null == expireMembership
          ? _value.expireMembership
          : expireMembership // ignore: cast_nullable_to_non_nullable
              as DateTime,
      powers: null == powers
          ? _value._powers
          : powers // ignore: cast_nullable_to_non_nullable
              as List<String>,
      addons: null == addons
          ? _value._addons
          : addons // ignore: cast_nullable_to_non_nullable
              as List<String>,
      isMembershipActive: null == isMembershipActive
          ? _value.isMembershipActive
          : isMembershipActive // ignore: cast_nullable_to_non_nullable
              as bool,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$UserModelImpl implements _UserModel {
  const _$UserModelImpl(
      {required this.id,
      required this.username,
      required this.name,
      required this.avatar,
      required this.email,
      @JsonKey(fromJson: getDateTimeLocal) required this.expireMembership,
      required final List<String> powers,
      required final List<String> addons,
      required this.isMembershipActive})
      : _powers = powers,
        _addons = addons;

  factory _$UserModelImpl.fromJson(Map<String, dynamic> json) =>
      _$$UserModelImplFromJson(json);

  @override
  final String id;
  @override
  final String username;
  @override
  final String name;
  @override
  final String avatar;
  @override
  final String email;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime expireMembership;
  final List<String> _powers;
  @override
  List<String> get powers {
    if (_powers is EqualUnmodifiableListView) return _powers;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_powers);
  }

  final List<String> _addons;
  @override
  List<String> get addons {
    if (_addons is EqualUnmodifiableListView) return _addons;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_addons);
  }

  @override
  final bool isMembershipActive;

  @override
  String toString() {
    return 'UserModel(id: $id, username: $username, name: $name, avatar: $avatar, email: $email, expireMembership: $expireMembership, powers: $powers, addons: $addons, isMembershipActive: $isMembershipActive)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$UserModelImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.username, username) ||
                other.username == username) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.avatar, avatar) || other.avatar == avatar) &&
            (identical(other.email, email) || other.email == email) &&
            (identical(other.expireMembership, expireMembership) ||
                other.expireMembership == expireMembership) &&
            const DeepCollectionEquality().equals(other._powers, _powers) &&
            const DeepCollectionEquality().equals(other._addons, _addons) &&
            (identical(other.isMembershipActive, isMembershipActive) ||
                other.isMembershipActive == isMembershipActive));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      username,
      name,
      avatar,
      email,
      expireMembership,
      const DeepCollectionEquality().hash(_powers),
      const DeepCollectionEquality().hash(_addons),
      isMembershipActive);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$UserModelImplCopyWith<_$UserModelImpl> get copyWith =>
      __$$UserModelImplCopyWithImpl<_$UserModelImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$UserModelImplToJson(
      this,
    );
  }
}

abstract class _UserModel implements UserModel {
  const factory _UserModel(
      {required final String id,
      required final String username,
      required final String name,
      required final String avatar,
      required final String email,
      @JsonKey(fromJson: getDateTimeLocal)
      required final DateTime expireMembership,
      required final List<String> powers,
      required final List<String> addons,
      required final bool isMembershipActive}) = _$UserModelImpl;

  factory _UserModel.fromJson(Map<String, dynamic> json) =
      _$UserModelImpl.fromJson;

  @override
  String get id;
  @override
  String get username;
  @override
  String get name;
  @override
  String get avatar;
  @override
  String get email;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get expireMembership;
  @override
  List<String> get powers;
  @override
  List<String> get addons;
  @override
  bool get isMembershipActive;
  @override
  @JsonKey(ignore: true)
  _$$UserModelImplCopyWith<_$UserModelImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
