// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'event_owner.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$EventOwnerModel {
  String get id;
  String get name;
  String get email;
  String get avatar;

  /// Create a copy of EventOwnerModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  $EventOwnerModelCopyWith<EventOwnerModel> get copyWith =>
      _$EventOwnerModelCopyWithImpl<EventOwnerModel>(
          this as EventOwnerModel, _$identity);

  /// Serializes this EventOwnerModel to a JSON map.
  Map<String, dynamic> toJson();

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is EventOwnerModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.email, email) || other.email == email) &&
            (identical(other.avatar, avatar) || other.avatar == avatar));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, name, email, avatar);

  @override
  String toString() {
    return 'EventOwnerModel(id: $id, name: $name, email: $email, avatar: $avatar)';
  }
}

/// @nodoc
abstract mixin class $EventOwnerModelCopyWith<$Res> {
  factory $EventOwnerModelCopyWith(
          EventOwnerModel value, $Res Function(EventOwnerModel) _then) =
      _$EventOwnerModelCopyWithImpl;
  @useResult
  $Res call({String id, String name, String email, String avatar});
}

/// @nodoc
class _$EventOwnerModelCopyWithImpl<$Res>
    implements $EventOwnerModelCopyWith<$Res> {
  _$EventOwnerModelCopyWithImpl(this._self, this._then);

  final EventOwnerModel _self;
  final $Res Function(EventOwnerModel) _then;

  /// Create a copy of EventOwnerModel
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? email = null,
    Object? avatar = null,
  }) {
    return _then(_self.copyWith(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _self.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      email: null == email
          ? _self.email
          : email // ignore: cast_nullable_to_non_nullable
              as String,
      avatar: null == avatar
          ? _self.avatar
          : avatar // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

/// Adds pattern-matching-related methods to [EventOwnerModel].
extension EventOwnerModelPatterns on EventOwnerModel {
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
    TResult Function(_EventOwnerModel value)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _EventOwnerModel() when $default != null:
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
    TResult Function(_EventOwnerModel value) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _EventOwnerModel():
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
    TResult? Function(_EventOwnerModel value)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _EventOwnerModel() when $default != null:
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
    TResult Function(String id, String name, String email, String avatar)?
        $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _EventOwnerModel() when $default != null:
        return $default(_that.id, _that.name, _that.email, _that.avatar);
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
    TResult Function(String id, String name, String email, String avatar)
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _EventOwnerModel():
        return $default(_that.id, _that.name, _that.email, _that.avatar);
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
    TResult? Function(String id, String name, String email, String avatar)?
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _EventOwnerModel() when $default != null:
        return $default(_that.id, _that.name, _that.email, _that.avatar);
      case _:
        return null;
    }
  }
}

/// @nodoc
@JsonSerializable()
class _EventOwnerModel implements EventOwnerModel {
  const _EventOwnerModel(
      {required this.id,
      required this.name,
      required this.email,
      required this.avatar});
  factory _EventOwnerModel.fromJson(Map<String, dynamic> json) =>
      _$EventOwnerModelFromJson(json);

  @override
  final String id;
  @override
  final String name;
  @override
  final String email;
  @override
  final String avatar;

  /// Create a copy of EventOwnerModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  _$EventOwnerModelCopyWith<_EventOwnerModel> get copyWith =>
      __$EventOwnerModelCopyWithImpl<_EventOwnerModel>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$EventOwnerModelToJson(
      this,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _EventOwnerModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.email, email) || other.email == email) &&
            (identical(other.avatar, avatar) || other.avatar == avatar));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, name, email, avatar);

  @override
  String toString() {
    return 'EventOwnerModel(id: $id, name: $name, email: $email, avatar: $avatar)';
  }
}

/// @nodoc
abstract mixin class _$EventOwnerModelCopyWith<$Res>
    implements $EventOwnerModelCopyWith<$Res> {
  factory _$EventOwnerModelCopyWith(
          _EventOwnerModel value, $Res Function(_EventOwnerModel) _then) =
      __$EventOwnerModelCopyWithImpl;
  @override
  @useResult
  $Res call({String id, String name, String email, String avatar});
}

/// @nodoc
class __$EventOwnerModelCopyWithImpl<$Res>
    implements _$EventOwnerModelCopyWith<$Res> {
  __$EventOwnerModelCopyWithImpl(this._self, this._then);

  final _EventOwnerModel _self;
  final $Res Function(_EventOwnerModel) _then;

  /// Create a copy of EventOwnerModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? email = null,
    Object? avatar = null,
  }) {
    return _then(_EventOwnerModel(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _self.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      email: null == email
          ? _self.email
          : email // ignore: cast_nullable_to_non_nullable
              as String,
      avatar: null == avatar
          ? _self.avatar
          : avatar // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

// dart format on
