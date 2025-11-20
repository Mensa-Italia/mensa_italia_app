// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'ex_granted_permissions.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$ExGrantedPermissionsModel {
  String get id;
  String get user;
  String get exApp;
  List<String> get permissions;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get created;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get updated;

  /// Create a copy of ExGrantedPermissionsModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  $ExGrantedPermissionsModelCopyWith<ExGrantedPermissionsModel> get copyWith =>
      _$ExGrantedPermissionsModelCopyWithImpl<ExGrantedPermissionsModel>(
          this as ExGrantedPermissionsModel, _$identity);

  /// Serializes this ExGrantedPermissionsModel to a JSON map.
  Map<String, dynamic> toJson();

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is ExGrantedPermissionsModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.user, user) || other.user == user) &&
            (identical(other.exApp, exApp) || other.exApp == exApp) &&
            const DeepCollectionEquality()
                .equals(other.permissions, permissions) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, user, exApp,
      const DeepCollectionEquality().hash(permissions), created, updated);

  @override
  String toString() {
    return 'ExGrantedPermissionsModel(id: $id, user: $user, exApp: $exApp, permissions: $permissions, created: $created, updated: $updated)';
  }
}

/// @nodoc
abstract mixin class $ExGrantedPermissionsModelCopyWith<$Res> {
  factory $ExGrantedPermissionsModelCopyWith(ExGrantedPermissionsModel value,
          $Res Function(ExGrantedPermissionsModel) _then) =
      _$ExGrantedPermissionsModelCopyWithImpl;
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
class _$ExGrantedPermissionsModelCopyWithImpl<$Res>
    implements $ExGrantedPermissionsModelCopyWith<$Res> {
  _$ExGrantedPermissionsModelCopyWithImpl(this._self, this._then);

  final ExGrantedPermissionsModel _self;
  final $Res Function(ExGrantedPermissionsModel) _then;

  /// Create a copy of ExGrantedPermissionsModel
  /// with the given fields replaced by the non-null parameter values.
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
    return _then(_self.copyWith(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      user: null == user
          ? _self.user
          : user // ignore: cast_nullable_to_non_nullable
              as String,
      exApp: null == exApp
          ? _self.exApp
          : exApp // ignore: cast_nullable_to_non_nullable
              as String,
      permissions: null == permissions
          ? _self.permissions
          : permissions // ignore: cast_nullable_to_non_nullable
              as List<String>,
      created: null == created
          ? _self.created
          : created // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updated: null == updated
          ? _self.updated
          : updated // ignore: cast_nullable_to_non_nullable
              as DateTime,
    ));
  }
}

/// Adds pattern-matching-related methods to [ExGrantedPermissionsModel].
extension ExGrantedPermissionsModelPatterns on ExGrantedPermissionsModel {
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
    TResult Function(_ExGrantedPermissionsModel value)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _ExGrantedPermissionsModel() when $default != null:
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
    TResult Function(_ExGrantedPermissionsModel value) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _ExGrantedPermissionsModel():
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
    TResult? Function(_ExGrantedPermissionsModel value)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _ExGrantedPermissionsModel() when $default != null:
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
            String user,
            String exApp,
            List<String> permissions,
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated)?
        $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _ExGrantedPermissionsModel() when $default != null:
        return $default(_that.id, _that.user, _that.exApp, _that.permissions,
            _that.created, _that.updated);
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
            String user,
            String exApp,
            List<String> permissions,
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated)
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _ExGrantedPermissionsModel():
        return $default(_that.id, _that.user, _that.exApp, _that.permissions,
            _that.created, _that.updated);
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
            String user,
            String exApp,
            List<String> permissions,
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated)?
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _ExGrantedPermissionsModel() when $default != null:
        return $default(_that.id, _that.user, _that.exApp, _that.permissions,
            _that.created, _that.updated);
      case _:
        return null;
    }
  }
}

/// @nodoc
@JsonSerializable()
class _ExGrantedPermissionsModel extends ExGrantedPermissionsModel {
  const _ExGrantedPermissionsModel(
      {required this.id,
      required this.user,
      required this.exApp,
      required final List<String> permissions,
      @JsonKey(fromJson: getDateTimeLocal) required this.created,
      @JsonKey(fromJson: getDateTimeLocal) required this.updated})
      : _permissions = permissions,
        super._();
  factory _ExGrantedPermissionsModel.fromJson(Map<String, dynamic> json) =>
      _$ExGrantedPermissionsModelFromJson(json);

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

  /// Create a copy of ExGrantedPermissionsModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  _$ExGrantedPermissionsModelCopyWith<_ExGrantedPermissionsModel>
      get copyWith =>
          __$ExGrantedPermissionsModelCopyWithImpl<_ExGrantedPermissionsModel>(
              this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$ExGrantedPermissionsModelToJson(
      this,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _ExGrantedPermissionsModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.user, user) || other.user == user) &&
            (identical(other.exApp, exApp) || other.exApp == exApp) &&
            const DeepCollectionEquality()
                .equals(other._permissions, _permissions) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, user, exApp,
      const DeepCollectionEquality().hash(_permissions), created, updated);

  @override
  String toString() {
    return 'ExGrantedPermissionsModel(id: $id, user: $user, exApp: $exApp, permissions: $permissions, created: $created, updated: $updated)';
  }
}

/// @nodoc
abstract mixin class _$ExGrantedPermissionsModelCopyWith<$Res>
    implements $ExGrantedPermissionsModelCopyWith<$Res> {
  factory _$ExGrantedPermissionsModelCopyWith(_ExGrantedPermissionsModel value,
          $Res Function(_ExGrantedPermissionsModel) _then) =
      __$ExGrantedPermissionsModelCopyWithImpl;
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
class __$ExGrantedPermissionsModelCopyWithImpl<$Res>
    implements _$ExGrantedPermissionsModelCopyWith<$Res> {
  __$ExGrantedPermissionsModelCopyWithImpl(this._self, this._then);

  final _ExGrantedPermissionsModel _self;
  final $Res Function(_ExGrantedPermissionsModel) _then;

  /// Create a copy of ExGrantedPermissionsModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $Res call({
    Object? id = null,
    Object? user = null,
    Object? exApp = null,
    Object? permissions = null,
    Object? created = null,
    Object? updated = null,
  }) {
    return _then(_ExGrantedPermissionsModel(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      user: null == user
          ? _self.user
          : user // ignore: cast_nullable_to_non_nullable
              as String,
      exApp: null == exApp
          ? _self.exApp
          : exApp // ignore: cast_nullable_to_non_nullable
              as String,
      permissions: null == permissions
          ? _self._permissions
          : permissions // ignore: cast_nullable_to_non_nullable
              as List<String>,
      created: null == created
          ? _self.created
          : created // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updated: null == updated
          ? _self.updated
          : updated // ignore: cast_nullable_to_non_nullable
              as DateTime,
    ));
  }
}

// dart format on
