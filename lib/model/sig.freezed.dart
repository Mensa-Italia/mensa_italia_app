// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'sig.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$SigModel {
  String get id;
  String get name;
  String get description;
  String get image;
  String get link;
  String get groupType;

  /// Create a copy of SigModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  $SigModelCopyWith<SigModel> get copyWith =>
      _$SigModelCopyWithImpl<SigModel>(this as SigModel, _$identity);

  /// Serializes this SigModel to a JSON map.
  Map<String, dynamic> toJson();

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is SigModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.image, image) || other.image == image) &&
            (identical(other.link, link) || other.link == link) &&
            (identical(other.groupType, groupType) ||
                other.groupType == groupType));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode =>
      Object.hash(runtimeType, id, name, description, image, link, groupType);

  @override
  String toString() {
    return 'SigModel(id: $id, name: $name, description: $description, image: $image, link: $link, groupType: $groupType)';
  }
}

/// @nodoc
abstract mixin class $SigModelCopyWith<$Res> {
  factory $SigModelCopyWith(SigModel value, $Res Function(SigModel) _then) =
      _$SigModelCopyWithImpl;
  @useResult
  $Res call(
      {String id,
      String name,
      String description,
      String image,
      String link,
      String groupType});
}

/// @nodoc
class _$SigModelCopyWithImpl<$Res> implements $SigModelCopyWith<$Res> {
  _$SigModelCopyWithImpl(this._self, this._then);

  final SigModel _self;
  final $Res Function(SigModel) _then;

  /// Create a copy of SigModel
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? description = null,
    Object? image = null,
    Object? link = null,
    Object? groupType = null,
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
      description: null == description
          ? _self.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      image: null == image
          ? _self.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
      link: null == link
          ? _self.link
          : link // ignore: cast_nullable_to_non_nullable
              as String,
      groupType: null == groupType
          ? _self.groupType
          : groupType // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

/// Adds pattern-matching-related methods to [SigModel].
extension SigModelPatterns on SigModel {
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
    TResult Function(_SigModel value)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _SigModel() when $default != null:
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
    TResult Function(_SigModel value) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _SigModel():
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
    TResult? Function(_SigModel value)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _SigModel() when $default != null:
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
    TResult Function(String id, String name, String description, String image,
            String link, String groupType)?
        $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _SigModel() when $default != null:
        return $default(_that.id, _that.name, _that.description, _that.image,
            _that.link, _that.groupType);
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
    TResult Function(String id, String name, String description, String image,
            String link, String groupType)
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _SigModel():
        return $default(_that.id, _that.name, _that.description, _that.image,
            _that.link, _that.groupType);
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
    TResult? Function(String id, String name, String description, String image,
            String link, String groupType)?
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _SigModel() when $default != null:
        return $default(_that.id, _that.name, _that.description, _that.image,
            _that.link, _that.groupType);
      case _:
        return null;
    }
  }
}

/// @nodoc
@JsonSerializable()
class _SigModel implements SigModel {
  const _SigModel(
      {required this.id,
      required this.name,
      required this.description,
      required this.image,
      required this.link,
      required this.groupType});
  factory _SigModel.fromJson(Map<String, dynamic> json) =>
      _$SigModelFromJson(json);

  @override
  final String id;
  @override
  final String name;
  @override
  final String description;
  @override
  final String image;
  @override
  final String link;
  @override
  final String groupType;

  /// Create a copy of SigModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  _$SigModelCopyWith<_SigModel> get copyWith =>
      __$SigModelCopyWithImpl<_SigModel>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$SigModelToJson(
      this,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _SigModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.image, image) || other.image == image) &&
            (identical(other.link, link) || other.link == link) &&
            (identical(other.groupType, groupType) ||
                other.groupType == groupType));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode =>
      Object.hash(runtimeType, id, name, description, image, link, groupType);

  @override
  String toString() {
    return 'SigModel(id: $id, name: $name, description: $description, image: $image, link: $link, groupType: $groupType)';
  }
}

/// @nodoc
abstract mixin class _$SigModelCopyWith<$Res>
    implements $SigModelCopyWith<$Res> {
  factory _$SigModelCopyWith(_SigModel value, $Res Function(_SigModel) _then) =
      __$SigModelCopyWithImpl;
  @override
  @useResult
  $Res call(
      {String id,
      String name,
      String description,
      String image,
      String link,
      String groupType});
}

/// @nodoc
class __$SigModelCopyWithImpl<$Res> implements _$SigModelCopyWith<$Res> {
  __$SigModelCopyWithImpl(this._self, this._then);

  final _SigModel _self;
  final $Res Function(_SigModel) _then;

  /// Create a copy of SigModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? description = null,
    Object? image = null,
    Object? link = null,
    Object? groupType = null,
  }) {
    return _then(_SigModel(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _self.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      description: null == description
          ? _self.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      image: null == image
          ? _self.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
      link: null == link
          ? _self.link
          : link // ignore: cast_nullable_to_non_nullable
              as String,
      groupType: null == groupType
          ? _self.groupType
          : groupType // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

// dart format on
