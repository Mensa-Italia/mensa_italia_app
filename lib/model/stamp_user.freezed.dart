// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'stamp_user.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$StampUserModel {
  String get id;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get created;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get updated;
  @JsonKey(readValue: getDataFromExpanded)
  StampModel get stamp;
  String get user;

  /// Create a copy of StampUserModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  $StampUserModelCopyWith<StampUserModel> get copyWith =>
      _$StampUserModelCopyWithImpl<StampUserModel>(
          this as StampUserModel, _$identity);

  /// Serializes this StampUserModel to a JSON map.
  Map<String, dynamic> toJson();

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is StampUserModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated) &&
            (identical(other.stamp, stamp) || other.stamp == stamp) &&
            (identical(other.user, user) || other.user == user));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode =>
      Object.hash(runtimeType, id, created, updated, stamp, user);

  @override
  String toString() {
    return 'StampUserModel(id: $id, created: $created, updated: $updated, stamp: $stamp, user: $user)';
  }
}

/// @nodoc
abstract mixin class $StampUserModelCopyWith<$Res> {
  factory $StampUserModelCopyWith(
          StampUserModel value, $Res Function(StampUserModel) _then) =
      _$StampUserModelCopyWithImpl;
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
class _$StampUserModelCopyWithImpl<$Res>
    implements $StampUserModelCopyWith<$Res> {
  _$StampUserModelCopyWithImpl(this._self, this._then);

  final StampUserModel _self;
  final $Res Function(StampUserModel) _then;

  /// Create a copy of StampUserModel
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? created = null,
    Object? updated = null,
    Object? stamp = null,
    Object? user = null,
  }) {
    return _then(_self.copyWith(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      created: null == created
          ? _self.created
          : created // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updated: null == updated
          ? _self.updated
          : updated // ignore: cast_nullable_to_non_nullable
              as DateTime,
      stamp: null == stamp
          ? _self.stamp
          : stamp // ignore: cast_nullable_to_non_nullable
              as StampModel,
      user: null == user
          ? _self.user
          : user // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }

  /// Create a copy of StampUserModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $StampModelCopyWith<$Res> get stamp {
    return $StampModelCopyWith<$Res>(_self.stamp, (value) {
      return _then(_self.copyWith(stamp: value));
    });
  }
}

/// Adds pattern-matching-related methods to [StampUserModel].
extension StampUserModelPatterns on StampUserModel {
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
    TResult Function(_StampUserModel value)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _StampUserModel() when $default != null:
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
    TResult Function(_StampUserModel value) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _StampUserModel():
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
    TResult? Function(_StampUserModel value)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _StampUserModel() when $default != null:
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
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated,
            @JsonKey(readValue: getDataFromExpanded) StampModel stamp,
            String user)?
        $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _StampUserModel() when $default != null:
        return $default(
            _that.id, _that.created, _that.updated, _that.stamp, _that.user);
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
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated,
            @JsonKey(readValue: getDataFromExpanded) StampModel stamp,
            String user)
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _StampUserModel():
        return $default(
            _that.id, _that.created, _that.updated, _that.stamp, _that.user);
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
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated,
            @JsonKey(readValue: getDataFromExpanded) StampModel stamp,
            String user)?
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _StampUserModel() when $default != null:
        return $default(
            _that.id, _that.created, _that.updated, _that.stamp, _that.user);
      case _:
        return null;
    }
  }
}

/// @nodoc
@JsonSerializable()
class _StampUserModel extends StampUserModel {
  const _StampUserModel(
      {required this.id,
      @JsonKey(fromJson: getDateTimeLocal) required this.created,
      @JsonKey(fromJson: getDateTimeLocal) required this.updated,
      @JsonKey(readValue: getDataFromExpanded) required this.stamp,
      required this.user})
      : super._();
  factory _StampUserModel.fromJson(Map<String, dynamic> json) =>
      _$StampUserModelFromJson(json);

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

  /// Create a copy of StampUserModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  _$StampUserModelCopyWith<_StampUserModel> get copyWith =>
      __$StampUserModelCopyWithImpl<_StampUserModel>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$StampUserModelToJson(
      this,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _StampUserModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated) &&
            (identical(other.stamp, stamp) || other.stamp == stamp) &&
            (identical(other.user, user) || other.user == user));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode =>
      Object.hash(runtimeType, id, created, updated, stamp, user);

  @override
  String toString() {
    return 'StampUserModel(id: $id, created: $created, updated: $updated, stamp: $stamp, user: $user)';
  }
}

/// @nodoc
abstract mixin class _$StampUserModelCopyWith<$Res>
    implements $StampUserModelCopyWith<$Res> {
  factory _$StampUserModelCopyWith(
          _StampUserModel value, $Res Function(_StampUserModel) _then) =
      __$StampUserModelCopyWithImpl;
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
class __$StampUserModelCopyWithImpl<$Res>
    implements _$StampUserModelCopyWith<$Res> {
  __$StampUserModelCopyWithImpl(this._self, this._then);

  final _StampUserModel _self;
  final $Res Function(_StampUserModel) _then;

  /// Create a copy of StampUserModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $Res call({
    Object? id = null,
    Object? created = null,
    Object? updated = null,
    Object? stamp = null,
    Object? user = null,
  }) {
    return _then(_StampUserModel(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      created: null == created
          ? _self.created
          : created // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updated: null == updated
          ? _self.updated
          : updated // ignore: cast_nullable_to_non_nullable
              as DateTime,
      stamp: null == stamp
          ? _self.stamp
          : stamp // ignore: cast_nullable_to_non_nullable
              as StampModel,
      user: null == user
          ? _self.user
          : user // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }

  /// Create a copy of StampUserModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $StampModelCopyWith<$Res> get stamp {
    return $StampModelCopyWith<$Res>(_self.stamp, (value) {
      return _then(_self.copyWith(stamp: value));
    });
  }
}

// dart format on
