// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'notification.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$NotificationModel {
  String get id;
  String get tr;
  Map<String, String> get trNamedParams;
  Map<String, dynamic>? get data;
  @JsonKey(fromJson: getDateTimeLocalNullabe)
  DateTime? get seen;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get created;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get updated;

  /// Create a copy of NotificationModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  $NotificationModelCopyWith<NotificationModel> get copyWith =>
      _$NotificationModelCopyWithImpl<NotificationModel>(
          this as NotificationModel, _$identity);

  /// Serializes this NotificationModel to a JSON map.
  Map<String, dynamic> toJson();

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is NotificationModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.tr, tr) || other.tr == tr) &&
            const DeepCollectionEquality()
                .equals(other.trNamedParams, trNamedParams) &&
            const DeepCollectionEquality().equals(other.data, data) &&
            (identical(other.seen, seen) || other.seen == seen) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      tr,
      const DeepCollectionEquality().hash(trNamedParams),
      const DeepCollectionEquality().hash(data),
      seen,
      created,
      updated);

  @override
  String toString() {
    return 'NotificationModel(id: $id, tr: $tr, trNamedParams: $trNamedParams, data: $data, seen: $seen, created: $created, updated: $updated)';
  }
}

/// @nodoc
abstract mixin class $NotificationModelCopyWith<$Res> {
  factory $NotificationModelCopyWith(
          NotificationModel value, $Res Function(NotificationModel) _then) =
      _$NotificationModelCopyWithImpl;
  @useResult
  $Res call(
      {String id,
      String tr,
      Map<String, String> trNamedParams,
      Map<String, dynamic>? data,
      @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? seen,
      @JsonKey(fromJson: getDateTimeLocal) DateTime created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime updated});
}

/// @nodoc
class _$NotificationModelCopyWithImpl<$Res>
    implements $NotificationModelCopyWith<$Res> {
  _$NotificationModelCopyWithImpl(this._self, this._then);

  final NotificationModel _self;
  final $Res Function(NotificationModel) _then;

  /// Create a copy of NotificationModel
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? tr = null,
    Object? trNamedParams = null,
    Object? data = freezed,
    Object? seen = freezed,
    Object? created = null,
    Object? updated = null,
  }) {
    return _then(_self.copyWith(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      tr: null == tr
          ? _self.tr
          : tr // ignore: cast_nullable_to_non_nullable
              as String,
      trNamedParams: null == trNamedParams
          ? _self.trNamedParams
          : trNamedParams // ignore: cast_nullable_to_non_nullable
              as Map<String, String>,
      data: freezed == data
          ? _self.data
          : data // ignore: cast_nullable_to_non_nullable
              as Map<String, dynamic>?,
      seen: freezed == seen
          ? _self.seen
          : seen // ignore: cast_nullable_to_non_nullable
              as DateTime?,
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

/// Adds pattern-matching-related methods to [NotificationModel].
extension NotificationModelPatterns on NotificationModel {
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
    TResult Function(_NotificationModel value)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _NotificationModel() when $default != null:
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
    TResult Function(_NotificationModel value) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _NotificationModel():
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
    TResult? Function(_NotificationModel value)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _NotificationModel() when $default != null:
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
            String tr,
            Map<String, String> trNamedParams,
            Map<String, dynamic>? data,
            @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? seen,
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated)?
        $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _NotificationModel() when $default != null:
        return $default(_that.id, _that.tr, _that.trNamedParams, _that.data,
            _that.seen, _that.created, _that.updated);
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
            String tr,
            Map<String, String> trNamedParams,
            Map<String, dynamic>? data,
            @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? seen,
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated)
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _NotificationModel():
        return $default(_that.id, _that.tr, _that.trNamedParams, _that.data,
            _that.seen, _that.created, _that.updated);
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
            String tr,
            Map<String, String> trNamedParams,
            Map<String, dynamic>? data,
            @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? seen,
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated)?
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _NotificationModel() when $default != null:
        return $default(_that.id, _that.tr, _that.trNamedParams, _that.data,
            _that.seen, _that.created, _that.updated);
      case _:
        return null;
    }
  }
}

/// @nodoc
@JsonSerializable()
class _NotificationModel extends NotificationModel {
  const _NotificationModel(
      {required this.id,
      required this.tr,
      required final Map<String, String> trNamedParams,
      final Map<String, dynamic>? data,
      @JsonKey(fromJson: getDateTimeLocalNullabe) required this.seen,
      @JsonKey(fromJson: getDateTimeLocal) required this.created,
      @JsonKey(fromJson: getDateTimeLocal) required this.updated})
      : _trNamedParams = trNamedParams,
        _data = data,
        super._();
  factory _NotificationModel.fromJson(Map<String, dynamic> json) =>
      _$NotificationModelFromJson(json);

  @override
  final String id;
  @override
  final String tr;
  final Map<String, String> _trNamedParams;
  @override
  Map<String, String> get trNamedParams {
    if (_trNamedParams is EqualUnmodifiableMapView) return _trNamedParams;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableMapView(_trNamedParams);
  }

  final Map<String, dynamic>? _data;
  @override
  Map<String, dynamic>? get data {
    final value = _data;
    if (value == null) return null;
    if (_data is EqualUnmodifiableMapView) return _data;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableMapView(value);
  }

  @override
  @JsonKey(fromJson: getDateTimeLocalNullabe)
  final DateTime? seen;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime created;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime updated;

  /// Create a copy of NotificationModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  _$NotificationModelCopyWith<_NotificationModel> get copyWith =>
      __$NotificationModelCopyWithImpl<_NotificationModel>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$NotificationModelToJson(
      this,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _NotificationModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.tr, tr) || other.tr == tr) &&
            const DeepCollectionEquality()
                .equals(other._trNamedParams, _trNamedParams) &&
            const DeepCollectionEquality().equals(other._data, _data) &&
            (identical(other.seen, seen) || other.seen == seen) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      tr,
      const DeepCollectionEquality().hash(_trNamedParams),
      const DeepCollectionEquality().hash(_data),
      seen,
      created,
      updated);

  @override
  String toString() {
    return 'NotificationModel(id: $id, tr: $tr, trNamedParams: $trNamedParams, data: $data, seen: $seen, created: $created, updated: $updated)';
  }
}

/// @nodoc
abstract mixin class _$NotificationModelCopyWith<$Res>
    implements $NotificationModelCopyWith<$Res> {
  factory _$NotificationModelCopyWith(
          _NotificationModel value, $Res Function(_NotificationModel) _then) =
      __$NotificationModelCopyWithImpl;
  @override
  @useResult
  $Res call(
      {String id,
      String tr,
      Map<String, String> trNamedParams,
      Map<String, dynamic>? data,
      @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? seen,
      @JsonKey(fromJson: getDateTimeLocal) DateTime created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime updated});
}

/// @nodoc
class __$NotificationModelCopyWithImpl<$Res>
    implements _$NotificationModelCopyWith<$Res> {
  __$NotificationModelCopyWithImpl(this._self, this._then);

  final _NotificationModel _self;
  final $Res Function(_NotificationModel) _then;

  /// Create a copy of NotificationModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $Res call({
    Object? id = null,
    Object? tr = null,
    Object? trNamedParams = null,
    Object? data = freezed,
    Object? seen = freezed,
    Object? created = null,
    Object? updated = null,
  }) {
    return _then(_NotificationModel(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      tr: null == tr
          ? _self.tr
          : tr // ignore: cast_nullable_to_non_nullable
              as String,
      trNamedParams: null == trNamedParams
          ? _self._trNamedParams
          : trNamedParams // ignore: cast_nullable_to_non_nullable
              as Map<String, String>,
      data: freezed == data
          ? _self._data
          : data // ignore: cast_nullable_to_non_nullable
              as Map<String, dynamic>?,
      seen: freezed == seen
          ? _self.seen
          : seen // ignore: cast_nullable_to_non_nullable
              as DateTime?,
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
