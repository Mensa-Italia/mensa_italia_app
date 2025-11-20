// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'user.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$UserModel {
  String get id;
  String get username;
  String get name;
  String get avatar;
  String get email;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get expireMembership;
  List<String> get powers;
  List<String> get addons;
  bool get isMembershipActive;

  /// Create a copy of UserModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  $UserModelCopyWith<UserModel> get copyWith =>
      _$UserModelCopyWithImpl<UserModel>(this as UserModel, _$identity);

  /// Serializes this UserModel to a JSON map.
  Map<String, dynamic> toJson();

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is UserModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.username, username) ||
                other.username == username) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.avatar, avatar) || other.avatar == avatar) &&
            (identical(other.email, email) || other.email == email) &&
            (identical(other.expireMembership, expireMembership) ||
                other.expireMembership == expireMembership) &&
            const DeepCollectionEquality().equals(other.powers, powers) &&
            const DeepCollectionEquality().equals(other.addons, addons) &&
            (identical(other.isMembershipActive, isMembershipActive) ||
                other.isMembershipActive == isMembershipActive));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      username,
      name,
      avatar,
      email,
      expireMembership,
      const DeepCollectionEquality().hash(powers),
      const DeepCollectionEquality().hash(addons),
      isMembershipActive);

  @override
  String toString() {
    return 'UserModel(id: $id, username: $username, name: $name, avatar: $avatar, email: $email, expireMembership: $expireMembership, powers: $powers, addons: $addons, isMembershipActive: $isMembershipActive)';
  }
}

/// @nodoc
abstract mixin class $UserModelCopyWith<$Res> {
  factory $UserModelCopyWith(UserModel value, $Res Function(UserModel) _then) =
      _$UserModelCopyWithImpl;
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
class _$UserModelCopyWithImpl<$Res> implements $UserModelCopyWith<$Res> {
  _$UserModelCopyWithImpl(this._self, this._then);

  final UserModel _self;
  final $Res Function(UserModel) _then;

  /// Create a copy of UserModel
  /// with the given fields replaced by the non-null parameter values.
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
    return _then(_self.copyWith(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      username: null == username
          ? _self.username
          : username // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _self.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      avatar: null == avatar
          ? _self.avatar
          : avatar // ignore: cast_nullable_to_non_nullable
              as String,
      email: null == email
          ? _self.email
          : email // ignore: cast_nullable_to_non_nullable
              as String,
      expireMembership: null == expireMembership
          ? _self.expireMembership
          : expireMembership // ignore: cast_nullable_to_non_nullable
              as DateTime,
      powers: null == powers
          ? _self.powers
          : powers // ignore: cast_nullable_to_non_nullable
              as List<String>,
      addons: null == addons
          ? _self.addons
          : addons // ignore: cast_nullable_to_non_nullable
              as List<String>,
      isMembershipActive: null == isMembershipActive
          ? _self.isMembershipActive
          : isMembershipActive // ignore: cast_nullable_to_non_nullable
              as bool,
    ));
  }
}

/// Adds pattern-matching-related methods to [UserModel].
extension UserModelPatterns on UserModel {
  /// A variant of `map` that fallback to returning `orElse`.
  ///
  /// It is equivalent to doing:
  /// ```dart
  /// switch (sealedClass) {
  ///   case final Subclass value:
  ///     return ...;
  ///   case _:
  ///     return orElse();
  /// }
  /// ```

  @optionalTypeArgs
  TResult maybeMap<TResult extends Object?>(
    TResult Function(_UserModel value)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _UserModel() when $default != null:
        return $default(_that);
      case _:
        return orElse();
    }
  }

  /// A `switch`-like method, using callbacks.
  ///
  /// Callbacks receives the raw object, upcasted.
  /// It is equivalent to doing:
  /// ```dart
  /// switch (sealedClass) {
  ///   case final Subclass value:
  ///     return ...;
  ///   case final Subclass2 value:
  ///     return ...;
  /// }
  /// ```

  @optionalTypeArgs
  TResult map<TResult extends Object?>(
    TResult Function(_UserModel value) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _UserModel():
        return $default(_that);
      case _:
        throw StateError('Unexpected subclass');
    }
  }

  /// A variant of `map` that fallback to returning `null`.
  ///
  /// It is equivalent to doing:
  /// ```dart
  /// switch (sealedClass) {
  ///   case final Subclass value:
  ///     return ...;
  ///   case _:
  ///     return null;
  /// }
  /// ```

  @optionalTypeArgs
  TResult? mapOrNull<TResult extends Object?>(
    TResult? Function(_UserModel value)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _UserModel() when $default != null:
        return $default(_that);
      case _:
        return null;
    }
  }

  /// A variant of `when` that fallback to an `orElse` callback.
  ///
  /// It is equivalent to doing:
  /// ```dart
  /// switch (sealedClass) {
  ///   case Subclass(:final field):
  ///     return ...;
  ///   case _:
  ///     return orElse();
  /// }
  /// ```

  @optionalTypeArgs
  TResult maybeWhen<TResult extends Object?>(
    TResult Function(
            String id,
            String username,
            String name,
            String avatar,
            String email,
            @JsonKey(fromJson: getDateTimeLocal) DateTime expireMembership,
            List<String> powers,
            List<String> addons,
            bool isMembershipActive)?
        $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _UserModel() when $default != null:
        return $default(
            _that.id,
            _that.username,
            _that.name,
            _that.avatar,
            _that.email,
            _that.expireMembership,
            _that.powers,
            _that.addons,
            _that.isMembershipActive);
      case _:
        return orElse();
    }
  }

  /// A `switch`-like method, using callbacks.
  ///
  /// As opposed to `map`, this offers destructuring.
  /// It is equivalent to doing:
  /// ```dart
  /// switch (sealedClass) {
  ///   case Subclass(:final field):
  ///     return ...;
  ///   case Subclass2(:final field2):
  ///     return ...;
  /// }
  /// ```

  @optionalTypeArgs
  TResult when<TResult extends Object?>(
    TResult Function(
            String id,
            String username,
            String name,
            String avatar,
            String email,
            @JsonKey(fromJson: getDateTimeLocal) DateTime expireMembership,
            List<String> powers,
            List<String> addons,
            bool isMembershipActive)
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _UserModel():
        return $default(
            _that.id,
            _that.username,
            _that.name,
            _that.avatar,
            _that.email,
            _that.expireMembership,
            _that.powers,
            _that.addons,
            _that.isMembershipActive);
      case _:
        throw StateError('Unexpected subclass');
    }
  }

  /// A variant of `when` that fallback to returning `null`
  ///
  /// It is equivalent to doing:
  /// ```dart
  /// switch (sealedClass) {
  ///   case Subclass(:final field):
  ///     return ...;
  ///   case _:
  ///     return null;
  /// }
  /// ```

  @optionalTypeArgs
  TResult? whenOrNull<TResult extends Object?>(
    TResult? Function(
            String id,
            String username,
            String name,
            String avatar,
            String email,
            @JsonKey(fromJson: getDateTimeLocal) DateTime expireMembership,
            List<String> powers,
            List<String> addons,
            bool isMembershipActive)?
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _UserModel() when $default != null:
        return $default(
            _that.id,
            _that.username,
            _that.name,
            _that.avatar,
            _that.email,
            _that.expireMembership,
            _that.powers,
            _that.addons,
            _that.isMembershipActive);
      case _:
        return null;
    }
  }
}

/// @nodoc
@JsonSerializable()
class _UserModel implements UserModel {
  const _UserModel(
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
  factory _UserModel.fromJson(Map<String, dynamic> json) =>
      _$UserModelFromJson(json);

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

  /// Create a copy of UserModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  _$UserModelCopyWith<_UserModel> get copyWith =>
      __$UserModelCopyWithImpl<_UserModel>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$UserModelToJson(
      this,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _UserModel &&
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

  @JsonKey(includeFromJson: false, includeToJson: false)
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

  @override
  String toString() {
    return 'UserModel(id: $id, username: $username, name: $name, avatar: $avatar, email: $email, expireMembership: $expireMembership, powers: $powers, addons: $addons, isMembershipActive: $isMembershipActive)';
  }
}

/// @nodoc
abstract mixin class _$UserModelCopyWith<$Res>
    implements $UserModelCopyWith<$Res> {
  factory _$UserModelCopyWith(
          _UserModel value, $Res Function(_UserModel) _then) =
      __$UserModelCopyWithImpl;
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
class __$UserModelCopyWithImpl<$Res> implements _$UserModelCopyWith<$Res> {
  __$UserModelCopyWithImpl(this._self, this._then);

  final _UserModel _self;
  final $Res Function(_UserModel) _then;

  /// Create a copy of UserModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
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
    return _then(_UserModel(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      username: null == username
          ? _self.username
          : username // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _self.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      avatar: null == avatar
          ? _self.avatar
          : avatar // ignore: cast_nullable_to_non_nullable
              as String,
      email: null == email
          ? _self.email
          : email // ignore: cast_nullable_to_non_nullable
              as String,
      expireMembership: null == expireMembership
          ? _self.expireMembership
          : expireMembership // ignore: cast_nullable_to_non_nullable
              as DateTime,
      powers: null == powers
          ? _self._powers
          : powers // ignore: cast_nullable_to_non_nullable
              as List<String>,
      addons: null == addons
          ? _self._addons
          : addons // ignore: cast_nullable_to_non_nullable
              as List<String>,
      isMembershipActive: null == isMembershipActive
          ? _self.isMembershipActive
          : isMembershipActive // ignore: cast_nullable_to_non_nullable
              as bool,
    ));
  }
}

// dart format on
