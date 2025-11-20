// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'device.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$DeviceModel {
  String get id;
  String get user;
  String get deviceName;
  String get firebaseId;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get created;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get updated;

  /// Create a copy of DeviceModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  $DeviceModelCopyWith<DeviceModel> get copyWith =>
      _$DeviceModelCopyWithImpl<DeviceModel>(this as DeviceModel, _$identity);

  /// Serializes this DeviceModel to a JSON map.
  Map<String, dynamic> toJson();

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is DeviceModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.user, user) || other.user == user) &&
            (identical(other.deviceName, deviceName) ||
                other.deviceName == deviceName) &&
            (identical(other.firebaseId, firebaseId) ||
                other.firebaseId == firebaseId) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType, id, user, deviceName, firebaseId, created, updated);

  @override
  String toString() {
    return 'DeviceModel(id: $id, user: $user, deviceName: $deviceName, firebaseId: $firebaseId, created: $created, updated: $updated)';
  }
}

/// @nodoc
abstract mixin class $DeviceModelCopyWith<$Res> {
  factory $DeviceModelCopyWith(
          DeviceModel value, $Res Function(DeviceModel) _then) =
      _$DeviceModelCopyWithImpl;
  @useResult
  $Res call(
      {String id,
      String user,
      String deviceName,
      String firebaseId,
      @JsonKey(fromJson: getDateTimeLocal) DateTime created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime updated});
}

/// @nodoc
class _$DeviceModelCopyWithImpl<$Res> implements $DeviceModelCopyWith<$Res> {
  _$DeviceModelCopyWithImpl(this._self, this._then);

  final DeviceModel _self;
  final $Res Function(DeviceModel) _then;

  /// Create a copy of DeviceModel
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? user = null,
    Object? deviceName = null,
    Object? firebaseId = null,
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
      deviceName: null == deviceName
          ? _self.deviceName
          : deviceName // ignore: cast_nullable_to_non_nullable
              as String,
      firebaseId: null == firebaseId
          ? _self.firebaseId
          : firebaseId // ignore: cast_nullable_to_non_nullable
              as String,
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

/// Adds pattern-matching-related methods to [DeviceModel].
extension DeviceModelPatterns on DeviceModel {
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
    TResult Function(_DeviceModel value)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _DeviceModel() when $default != null:
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
    TResult Function(_DeviceModel value) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _DeviceModel():
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
    TResult? Function(_DeviceModel value)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _DeviceModel() when $default != null:
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
            String deviceName,
            String firebaseId,
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated)?
        $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _DeviceModel() when $default != null:
        return $default(_that.id, _that.user, _that.deviceName,
            _that.firebaseId, _that.created, _that.updated);
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
            String deviceName,
            String firebaseId,
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated)
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _DeviceModel():
        return $default(_that.id, _that.user, _that.deviceName,
            _that.firebaseId, _that.created, _that.updated);
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
            String deviceName,
            String firebaseId,
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated)?
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _DeviceModel() when $default != null:
        return $default(_that.id, _that.user, _that.deviceName,
            _that.firebaseId, _that.created, _that.updated);
      case _:
        return null;
    }
  }
}

/// @nodoc
@JsonSerializable()
class _DeviceModel implements DeviceModel {
  const _DeviceModel(
      {required this.id,
      required this.user,
      required this.deviceName,
      required this.firebaseId,
      @JsonKey(fromJson: getDateTimeLocal) required this.created,
      @JsonKey(fromJson: getDateTimeLocal) required this.updated});
  factory _DeviceModel.fromJson(Map<String, dynamic> json) =>
      _$DeviceModelFromJson(json);

  @override
  final String id;
  @override
  final String user;
  @override
  final String deviceName;
  @override
  final String firebaseId;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime created;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime updated;

  /// Create a copy of DeviceModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  _$DeviceModelCopyWith<_DeviceModel> get copyWith =>
      __$DeviceModelCopyWithImpl<_DeviceModel>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$DeviceModelToJson(
      this,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _DeviceModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.user, user) || other.user == user) &&
            (identical(other.deviceName, deviceName) ||
                other.deviceName == deviceName) &&
            (identical(other.firebaseId, firebaseId) ||
                other.firebaseId == firebaseId) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType, id, user, deviceName, firebaseId, created, updated);

  @override
  String toString() {
    return 'DeviceModel(id: $id, user: $user, deviceName: $deviceName, firebaseId: $firebaseId, created: $created, updated: $updated)';
  }
}

/// @nodoc
abstract mixin class _$DeviceModelCopyWith<$Res>
    implements $DeviceModelCopyWith<$Res> {
  factory _$DeviceModelCopyWith(
          _DeviceModel value, $Res Function(_DeviceModel) _then) =
      __$DeviceModelCopyWithImpl;
  @override
  @useResult
  $Res call(
      {String id,
      String user,
      String deviceName,
      String firebaseId,
      @JsonKey(fromJson: getDateTimeLocal) DateTime created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime updated});
}

/// @nodoc
class __$DeviceModelCopyWithImpl<$Res> implements _$DeviceModelCopyWith<$Res> {
  __$DeviceModelCopyWithImpl(this._self, this._then);

  final _DeviceModel _self;
  final $Res Function(_DeviceModel) _then;

  /// Create a copy of DeviceModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $Res call({
    Object? id = null,
    Object? user = null,
    Object? deviceName = null,
    Object? firebaseId = null,
    Object? created = null,
    Object? updated = null,
  }) {
    return _then(_DeviceModel(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      user: null == user
          ? _self.user
          : user // ignore: cast_nullable_to_non_nullable
              as String,
      deviceName: null == deviceName
          ? _self.deviceName
          : deviceName // ignore: cast_nullable_to_non_nullable
              as String,
      firebaseId: null == firebaseId
          ? _self.firebaseId
          : firebaseId // ignore: cast_nullable_to_non_nullable
              as String,
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
