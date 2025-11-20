// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'area_document.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$AreaDocumentModel {
  String get description;
  String get image;
  String get dimension;
  String get link;

  /// Create a copy of AreaDocumentModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  $AreaDocumentModelCopyWith<AreaDocumentModel> get copyWith =>
      _$AreaDocumentModelCopyWithImpl<AreaDocumentModel>(
          this as AreaDocumentModel, _$identity);

  /// Serializes this AreaDocumentModel to a JSON map.
  Map<String, dynamic> toJson();

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is AreaDocumentModel &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.image, image) || other.image == image) &&
            (identical(other.dimension, dimension) ||
                other.dimension == dimension) &&
            (identical(other.link, link) || other.link == link));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode =>
      Object.hash(runtimeType, description, image, dimension, link);

  @override
  String toString() {
    return 'AreaDocumentModel(description: $description, image: $image, dimension: $dimension, link: $link)';
  }
}

/// @nodoc
abstract mixin class $AreaDocumentModelCopyWith<$Res> {
  factory $AreaDocumentModelCopyWith(
          AreaDocumentModel value, $Res Function(AreaDocumentModel) _then) =
      _$AreaDocumentModelCopyWithImpl;
  @useResult
  $Res call({String description, String image, String dimension, String link});
}

/// @nodoc
class _$AreaDocumentModelCopyWithImpl<$Res>
    implements $AreaDocumentModelCopyWith<$Res> {
  _$AreaDocumentModelCopyWithImpl(this._self, this._then);

  final AreaDocumentModel _self;
  final $Res Function(AreaDocumentModel) _then;

  /// Create a copy of AreaDocumentModel
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? description = null,
    Object? image = null,
    Object? dimension = null,
    Object? link = null,
  }) {
    return _then(_self.copyWith(
      description: null == description
          ? _self.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      image: null == image
          ? _self.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
      dimension: null == dimension
          ? _self.dimension
          : dimension // ignore: cast_nullable_to_non_nullable
              as String,
      link: null == link
          ? _self.link
          : link // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

/// Adds pattern-matching-related methods to [AreaDocumentModel].
extension AreaDocumentModelPatterns on AreaDocumentModel {
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
    TResult Function(_AreaDocumentModel value)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _AreaDocumentModel() when $default != null:
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
    TResult Function(_AreaDocumentModel value) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _AreaDocumentModel():
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
    TResult? Function(_AreaDocumentModel value)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _AreaDocumentModel() when $default != null:
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
            String description, String image, String dimension, String link)?
        $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _AreaDocumentModel() when $default != null:
        return $default(
            _that.description, _that.image, _that.dimension, _that.link);
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
            String description, String image, String dimension, String link)
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _AreaDocumentModel():
        return $default(
            _that.description, _that.image, _that.dimension, _that.link);
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
            String description, String image, String dimension, String link)?
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _AreaDocumentModel() when $default != null:
        return $default(
            _that.description, _that.image, _that.dimension, _that.link);
      case _:
        return null;
    }
  }
}

/// @nodoc
@JsonSerializable()
class _AreaDocumentModel implements AreaDocumentModel {
  _AreaDocumentModel(
      {required this.description,
      required this.image,
      required this.dimension,
      required this.link});
  factory _AreaDocumentModel.fromJson(Map<String, dynamic> json) =>
      _$AreaDocumentModelFromJson(json);

  @override
  final String description;
  @override
  final String image;
  @override
  final String dimension;
  @override
  final String link;

  /// Create a copy of AreaDocumentModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  _$AreaDocumentModelCopyWith<_AreaDocumentModel> get copyWith =>
      __$AreaDocumentModelCopyWithImpl<_AreaDocumentModel>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$AreaDocumentModelToJson(
      this,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _AreaDocumentModel &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.image, image) || other.image == image) &&
            (identical(other.dimension, dimension) ||
                other.dimension == dimension) &&
            (identical(other.link, link) || other.link == link));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode =>
      Object.hash(runtimeType, description, image, dimension, link);

  @override
  String toString() {
    return 'AreaDocumentModel(description: $description, image: $image, dimension: $dimension, link: $link)';
  }
}

/// @nodoc
abstract mixin class _$AreaDocumentModelCopyWith<$Res>
    implements $AreaDocumentModelCopyWith<$Res> {
  factory _$AreaDocumentModelCopyWith(
          _AreaDocumentModel value, $Res Function(_AreaDocumentModel) _then) =
      __$AreaDocumentModelCopyWithImpl;
  @override
  @useResult
  $Res call({String description, String image, String dimension, String link});
}

/// @nodoc
class __$AreaDocumentModelCopyWithImpl<$Res>
    implements _$AreaDocumentModelCopyWith<$Res> {
  __$AreaDocumentModelCopyWithImpl(this._self, this._then);

  final _AreaDocumentModel _self;
  final $Res Function(_AreaDocumentModel) _then;

  /// Create a copy of AreaDocumentModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $Res call({
    Object? description = null,
    Object? image = null,
    Object? dimension = null,
    Object? link = null,
  }) {
    return _then(_AreaDocumentModel(
      description: null == description
          ? _self.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      image: null == image
          ? _self.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
      dimension: null == dimension
          ? _self.dimension
          : dimension // ignore: cast_nullable_to_non_nullable
              as String,
      link: null == link
          ? _self.link
          : link // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

// dart format on
