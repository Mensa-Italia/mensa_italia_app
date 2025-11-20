// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'calendar_link.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$CalendarLinkModel {
  String get id;
  String get user;
  String get hash;
  List<String> get state;

  /// Create a copy of CalendarLinkModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  $CalendarLinkModelCopyWith<CalendarLinkModel> get copyWith =>
      _$CalendarLinkModelCopyWithImpl<CalendarLinkModel>(
          this as CalendarLinkModel, _$identity);

  /// Serializes this CalendarLinkModel to a JSON map.
  Map<String, dynamic> toJson();

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is CalendarLinkModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.user, user) || other.user == user) &&
            (identical(other.hash, hash) || other.hash == hash) &&
            const DeepCollectionEquality().equals(other.state, state));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType, id, user, hash, const DeepCollectionEquality().hash(state));

  @override
  String toString() {
    return 'CalendarLinkModel(id: $id, user: $user, hash: $hash, state: $state)';
  }
}

/// @nodoc
abstract mixin class $CalendarLinkModelCopyWith<$Res> {
  factory $CalendarLinkModelCopyWith(
          CalendarLinkModel value, $Res Function(CalendarLinkModel) _then) =
      _$CalendarLinkModelCopyWithImpl;
  @useResult
  $Res call({String id, String user, String hash, List<String> state});
}

/// @nodoc
class _$CalendarLinkModelCopyWithImpl<$Res>
    implements $CalendarLinkModelCopyWith<$Res> {
  _$CalendarLinkModelCopyWithImpl(this._self, this._then);

  final CalendarLinkModel _self;
  final $Res Function(CalendarLinkModel) _then;

  /// Create a copy of CalendarLinkModel
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? user = null,
    Object? hash = null,
    Object? state = null,
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
      hash: null == hash
          ? _self.hash
          : hash // ignore: cast_nullable_to_non_nullable
              as String,
      state: null == state
          ? _self.state
          : state // ignore: cast_nullable_to_non_nullable
              as List<String>,
    ));
  }
}

/// Adds pattern-matching-related methods to [CalendarLinkModel].
extension CalendarLinkModelPatterns on CalendarLinkModel {
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
    TResult Function(_CalendarLinkModel value)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _CalendarLinkModel() when $default != null:
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
    TResult Function(_CalendarLinkModel value) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _CalendarLinkModel():
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
    TResult? Function(_CalendarLinkModel value)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _CalendarLinkModel() when $default != null:
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
    TResult Function(String id, String user, String hash, List<String> state)?
        $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _CalendarLinkModel() when $default != null:
        return $default(_that.id, _that.user, _that.hash, _that.state);
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
    TResult Function(String id, String user, String hash, List<String> state)
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _CalendarLinkModel():
        return $default(_that.id, _that.user, _that.hash, _that.state);
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
    TResult? Function(String id, String user, String hash, List<String> state)?
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _CalendarLinkModel() when $default != null:
        return $default(_that.id, _that.user, _that.hash, _that.state);
      case _:
        return null;
    }
  }
}

/// @nodoc
@JsonSerializable()
class _CalendarLinkModel implements CalendarLinkModel {
  _CalendarLinkModel(
      {required this.id,
      required this.user,
      required this.hash,
      required final List<String> state})
      : _state = state;
  factory _CalendarLinkModel.fromJson(Map<String, dynamic> json) =>
      _$CalendarLinkModelFromJson(json);

  @override
  final String id;
  @override
  final String user;
  @override
  final String hash;
  final List<String> _state;
  @override
  List<String> get state {
    if (_state is EqualUnmodifiableListView) return _state;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_state);
  }

  /// Create a copy of CalendarLinkModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  _$CalendarLinkModelCopyWith<_CalendarLinkModel> get copyWith =>
      __$CalendarLinkModelCopyWithImpl<_CalendarLinkModel>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$CalendarLinkModelToJson(
      this,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _CalendarLinkModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.user, user) || other.user == user) &&
            (identical(other.hash, hash) || other.hash == hash) &&
            const DeepCollectionEquality().equals(other._state, _state));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType, id, user, hash, const DeepCollectionEquality().hash(_state));

  @override
  String toString() {
    return 'CalendarLinkModel(id: $id, user: $user, hash: $hash, state: $state)';
  }
}

/// @nodoc
abstract mixin class _$CalendarLinkModelCopyWith<$Res>
    implements $CalendarLinkModelCopyWith<$Res> {
  factory _$CalendarLinkModelCopyWith(
          _CalendarLinkModel value, $Res Function(_CalendarLinkModel) _then) =
      __$CalendarLinkModelCopyWithImpl;
  @override
  @useResult
  $Res call({String id, String user, String hash, List<String> state});
}

/// @nodoc
class __$CalendarLinkModelCopyWithImpl<$Res>
    implements _$CalendarLinkModelCopyWith<$Res> {
  __$CalendarLinkModelCopyWithImpl(this._self, this._then);

  final _CalendarLinkModel _self;
  final $Res Function(_CalendarLinkModel) _then;

  /// Create a copy of CalendarLinkModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $Res call({
    Object? id = null,
    Object? user = null,
    Object? hash = null,
    Object? state = null,
  }) {
    return _then(_CalendarLinkModel(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      user: null == user
          ? _self.user
          : user // ignore: cast_nullable_to_non_nullable
              as String,
      hash: null == hash
          ? _self.hash
          : hash // ignore: cast_nullable_to_non_nullable
              as String,
      state: null == state
          ? _self._state
          : state // ignore: cast_nullable_to_non_nullable
              as List<String>,
    ));
  }
}

// dart format on
