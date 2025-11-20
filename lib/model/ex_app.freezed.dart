// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'ex_app.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$ExAppModel {
  String? get collectionId;
  String? get collectionName;
  String? get id;
  String? get name;
  String? get description;
  String? get image;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime? get created;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime? get updated;

  /// Create a copy of ExAppModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  $ExAppModelCopyWith<ExAppModel> get copyWith =>
      _$ExAppModelCopyWithImpl<ExAppModel>(this as ExAppModel, _$identity);

  /// Serializes this ExAppModel to a JSON map.
  Map<String, dynamic> toJson();

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is ExAppModel &&
            (identical(other.collectionId, collectionId) ||
                other.collectionId == collectionId) &&
            (identical(other.collectionName, collectionName) ||
                other.collectionName == collectionName) &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.image, image) || other.image == image) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, collectionId, collectionName, id,
      name, description, image, created, updated);

  @override
  String toString() {
    return 'ExAppModel(collectionId: $collectionId, collectionName: $collectionName, id: $id, name: $name, description: $description, image: $image, created: $created, updated: $updated)';
  }
}

/// @nodoc
abstract mixin class $ExAppModelCopyWith<$Res> {
  factory $ExAppModelCopyWith(
          ExAppModel value, $Res Function(ExAppModel) _then) =
      _$ExAppModelCopyWithImpl;
  @useResult
  $Res call(
      {String? collectionId,
      String? collectionName,
      String? id,
      String? name,
      String? description,
      String? image,
      @JsonKey(fromJson: getDateTimeLocal) DateTime? created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime? updated});
}

/// @nodoc
class _$ExAppModelCopyWithImpl<$Res> implements $ExAppModelCopyWith<$Res> {
  _$ExAppModelCopyWithImpl(this._self, this._then);

  final ExAppModel _self;
  final $Res Function(ExAppModel) _then;

  /// Create a copy of ExAppModel
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? collectionId = freezed,
    Object? collectionName = freezed,
    Object? id = freezed,
    Object? name = freezed,
    Object? description = freezed,
    Object? image = freezed,
    Object? created = freezed,
    Object? updated = freezed,
  }) {
    return _then(_self.copyWith(
      collectionId: freezed == collectionId
          ? _self.collectionId
          : collectionId // ignore: cast_nullable_to_non_nullable
              as String?,
      collectionName: freezed == collectionName
          ? _self.collectionName
          : collectionName // ignore: cast_nullable_to_non_nullable
              as String?,
      id: freezed == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String?,
      name: freezed == name
          ? _self.name
          : name // ignore: cast_nullable_to_non_nullable
              as String?,
      description: freezed == description
          ? _self.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      image: freezed == image
          ? _self.image
          : image // ignore: cast_nullable_to_non_nullable
              as String?,
      created: freezed == created
          ? _self.created
          : created // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      updated: freezed == updated
          ? _self.updated
          : updated // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ));
  }
}

/// Adds pattern-matching-related methods to [ExAppModel].
extension ExAppModelPatterns on ExAppModel {
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
    TResult Function(_ExAppModel value)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _ExAppModel() when $default != null:
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
    TResult Function(_ExAppModel value) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _ExAppModel():
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
    TResult? Function(_ExAppModel value)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _ExAppModel() when $default != null:
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
            String? collectionId,
            String? collectionName,
            String? id,
            String? name,
            String? description,
            String? image,
            @JsonKey(fromJson: getDateTimeLocal) DateTime? created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime? updated)?
        $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _ExAppModel() when $default != null:
        return $default(
            _that.collectionId,
            _that.collectionName,
            _that.id,
            _that.name,
            _that.description,
            _that.image,
            _that.created,
            _that.updated);
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
            String? collectionId,
            String? collectionName,
            String? id,
            String? name,
            String? description,
            String? image,
            @JsonKey(fromJson: getDateTimeLocal) DateTime? created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime? updated)
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _ExAppModel():
        return $default(
            _that.collectionId,
            _that.collectionName,
            _that.id,
            _that.name,
            _that.description,
            _that.image,
            _that.created,
            _that.updated);
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
            String? collectionId,
            String? collectionName,
            String? id,
            String? name,
            String? description,
            String? image,
            @JsonKey(fromJson: getDateTimeLocal) DateTime? created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime? updated)?
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _ExAppModel() when $default != null:
        return $default(
            _that.collectionId,
            _that.collectionName,
            _that.id,
            _that.name,
            _that.description,
            _that.image,
            _that.created,
            _that.updated);
      case _:
        return null;
    }
  }
}

/// @nodoc
@JsonSerializable()
class _ExAppModel implements ExAppModel {
  _ExAppModel(
      {this.collectionId,
      this.collectionName,
      this.id,
      this.name,
      this.description,
      this.image,
      @JsonKey(fromJson: getDateTimeLocal) this.created,
      @JsonKey(fromJson: getDateTimeLocal) this.updated});
  factory _ExAppModel.fromJson(Map<String, dynamic> json) =>
      _$ExAppModelFromJson(json);

  @override
  final String? collectionId;
  @override
  final String? collectionName;
  @override
  final String? id;
  @override
  final String? name;
  @override
  final String? description;
  @override
  final String? image;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime? created;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime? updated;

  /// Create a copy of ExAppModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  _$ExAppModelCopyWith<_ExAppModel> get copyWith =>
      __$ExAppModelCopyWithImpl<_ExAppModel>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$ExAppModelToJson(
      this,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _ExAppModel &&
            (identical(other.collectionId, collectionId) ||
                other.collectionId == collectionId) &&
            (identical(other.collectionName, collectionName) ||
                other.collectionName == collectionName) &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.image, image) || other.image == image) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, collectionId, collectionName, id,
      name, description, image, created, updated);

  @override
  String toString() {
    return 'ExAppModel(collectionId: $collectionId, collectionName: $collectionName, id: $id, name: $name, description: $description, image: $image, created: $created, updated: $updated)';
  }
}

/// @nodoc
abstract mixin class _$ExAppModelCopyWith<$Res>
    implements $ExAppModelCopyWith<$Res> {
  factory _$ExAppModelCopyWith(
          _ExAppModel value, $Res Function(_ExAppModel) _then) =
      __$ExAppModelCopyWithImpl;
  @override
  @useResult
  $Res call(
      {String? collectionId,
      String? collectionName,
      String? id,
      String? name,
      String? description,
      String? image,
      @JsonKey(fromJson: getDateTimeLocal) DateTime? created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime? updated});
}

/// @nodoc
class __$ExAppModelCopyWithImpl<$Res> implements _$ExAppModelCopyWith<$Res> {
  __$ExAppModelCopyWithImpl(this._self, this._then);

  final _ExAppModel _self;
  final $Res Function(_ExAppModel) _then;

  /// Create a copy of ExAppModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $Res call({
    Object? collectionId = freezed,
    Object? collectionName = freezed,
    Object? id = freezed,
    Object? name = freezed,
    Object? description = freezed,
    Object? image = freezed,
    Object? created = freezed,
    Object? updated = freezed,
  }) {
    return _then(_ExAppModel(
      collectionId: freezed == collectionId
          ? _self.collectionId
          : collectionId // ignore: cast_nullable_to_non_nullable
              as String?,
      collectionName: freezed == collectionName
          ? _self.collectionName
          : collectionName // ignore: cast_nullable_to_non_nullable
              as String?,
      id: freezed == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String?,
      name: freezed == name
          ? _self.name
          : name // ignore: cast_nullable_to_non_nullable
              as String?,
      description: freezed == description
          ? _self.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      image: freezed == image
          ? _self.image
          : image // ignore: cast_nullable_to_non_nullable
              as String?,
      created: freezed == created
          ? _self.created
          : created // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      updated: freezed == updated
          ? _self.updated
          : updated // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ));
  }
}

// dart format on
