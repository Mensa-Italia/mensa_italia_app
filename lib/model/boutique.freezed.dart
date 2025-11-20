// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'boutique.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$BoutiqueModel {
  String get id;
  String get uid;
  String get name;
  String get description;
  List<String> get image;
  int get amount;
  String get alternativeOf;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get created;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get updated;

  /// Create a copy of BoutiqueModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  $BoutiqueModelCopyWith<BoutiqueModel> get copyWith =>
      _$BoutiqueModelCopyWithImpl<BoutiqueModel>(
          this as BoutiqueModel, _$identity);

  /// Serializes this BoutiqueModel to a JSON map.
  Map<String, dynamic> toJson();

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is BoutiqueModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.uid, uid) || other.uid == uid) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            const DeepCollectionEquality().equals(other.image, image) &&
            (identical(other.amount, amount) || other.amount == amount) &&
            (identical(other.alternativeOf, alternativeOf) ||
                other.alternativeOf == alternativeOf) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      uid,
      name,
      description,
      const DeepCollectionEquality().hash(image),
      amount,
      alternativeOf,
      created,
      updated);

  @override
  String toString() {
    return 'BoutiqueModel(id: $id, uid: $uid, name: $name, description: $description, image: $image, amount: $amount, alternativeOf: $alternativeOf, created: $created, updated: $updated)';
  }
}

/// @nodoc
abstract mixin class $BoutiqueModelCopyWith<$Res> {
  factory $BoutiqueModelCopyWith(
          BoutiqueModel value, $Res Function(BoutiqueModel) _then) =
      _$BoutiqueModelCopyWithImpl;
  @useResult
  $Res call(
      {String id,
      String uid,
      String name,
      String description,
      List<String> image,
      int amount,
      String alternativeOf,
      @JsonKey(fromJson: getDateTimeLocal) DateTime created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime updated});
}

/// @nodoc
class _$BoutiqueModelCopyWithImpl<$Res>
    implements $BoutiqueModelCopyWith<$Res> {
  _$BoutiqueModelCopyWithImpl(this._self, this._then);

  final BoutiqueModel _self;
  final $Res Function(BoutiqueModel) _then;

  /// Create a copy of BoutiqueModel
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? uid = null,
    Object? name = null,
    Object? description = null,
    Object? image = null,
    Object? amount = null,
    Object? alternativeOf = null,
    Object? created = null,
    Object? updated = null,
  }) {
    return _then(_self.copyWith(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      uid: null == uid
          ? _self.uid
          : uid // ignore: cast_nullable_to_non_nullable
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
              as List<String>,
      amount: null == amount
          ? _self.amount
          : amount // ignore: cast_nullable_to_non_nullable
              as int,
      alternativeOf: null == alternativeOf
          ? _self.alternativeOf
          : alternativeOf // ignore: cast_nullable_to_non_nullable
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

/// Adds pattern-matching-related methods to [BoutiqueModel].
extension BoutiqueModelPatterns on BoutiqueModel {
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
    TResult Function(_BoutiqueModel value)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _BoutiqueModel() when $default != null:
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
    TResult Function(_BoutiqueModel value) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _BoutiqueModel():
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
    TResult? Function(_BoutiqueModel value)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _BoutiqueModel() when $default != null:
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
            String uid,
            String name,
            String description,
            List<String> image,
            int amount,
            String alternativeOf,
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated)?
        $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _BoutiqueModel() when $default != null:
        return $default(
            _that.id,
            _that.uid,
            _that.name,
            _that.description,
            _that.image,
            _that.amount,
            _that.alternativeOf,
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
            String id,
            String uid,
            String name,
            String description,
            List<String> image,
            int amount,
            String alternativeOf,
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated)
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _BoutiqueModel():
        return $default(
            _that.id,
            _that.uid,
            _that.name,
            _that.description,
            _that.image,
            _that.amount,
            _that.alternativeOf,
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
            String id,
            String uid,
            String name,
            String description,
            List<String> image,
            int amount,
            String alternativeOf,
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated)?
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _BoutiqueModel() when $default != null:
        return $default(
            _that.id,
            _that.uid,
            _that.name,
            _that.description,
            _that.image,
            _that.amount,
            _that.alternativeOf,
            _that.created,
            _that.updated);
      case _:
        return null;
    }
  }
}

/// @nodoc
@JsonSerializable()
class _BoutiqueModel extends BoutiqueModel {
  _BoutiqueModel(
      {required this.id,
      required this.uid,
      required this.name,
      required this.description,
      required final List<String> image,
      required this.amount,
      required this.alternativeOf,
      @JsonKey(fromJson: getDateTimeLocal) required this.created,
      @JsonKey(fromJson: getDateTimeLocal) required this.updated})
      : _image = image,
        super._();
  factory _BoutiqueModel.fromJson(Map<String, dynamic> json) =>
      _$BoutiqueModelFromJson(json);

  @override
  final String id;
  @override
  final String uid;
  @override
  final String name;
  @override
  final String description;
  final List<String> _image;
  @override
  List<String> get image {
    if (_image is EqualUnmodifiableListView) return _image;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_image);
  }

  @override
  final int amount;
  @override
  final String alternativeOf;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime created;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime updated;

  /// Create a copy of BoutiqueModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  _$BoutiqueModelCopyWith<_BoutiqueModel> get copyWith =>
      __$BoutiqueModelCopyWithImpl<_BoutiqueModel>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$BoutiqueModelToJson(
      this,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _BoutiqueModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.uid, uid) || other.uid == uid) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            const DeepCollectionEquality().equals(other._image, _image) &&
            (identical(other.amount, amount) || other.amount == amount) &&
            (identical(other.alternativeOf, alternativeOf) ||
                other.alternativeOf == alternativeOf) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      uid,
      name,
      description,
      const DeepCollectionEquality().hash(_image),
      amount,
      alternativeOf,
      created,
      updated);

  @override
  String toString() {
    return 'BoutiqueModel(id: $id, uid: $uid, name: $name, description: $description, image: $image, amount: $amount, alternativeOf: $alternativeOf, created: $created, updated: $updated)';
  }
}

/// @nodoc
abstract mixin class _$BoutiqueModelCopyWith<$Res>
    implements $BoutiqueModelCopyWith<$Res> {
  factory _$BoutiqueModelCopyWith(
          _BoutiqueModel value, $Res Function(_BoutiqueModel) _then) =
      __$BoutiqueModelCopyWithImpl;
  @override
  @useResult
  $Res call(
      {String id,
      String uid,
      String name,
      String description,
      List<String> image,
      int amount,
      String alternativeOf,
      @JsonKey(fromJson: getDateTimeLocal) DateTime created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime updated});
}

/// @nodoc
class __$BoutiqueModelCopyWithImpl<$Res>
    implements _$BoutiqueModelCopyWith<$Res> {
  __$BoutiqueModelCopyWithImpl(this._self, this._then);

  final _BoutiqueModel _self;
  final $Res Function(_BoutiqueModel) _then;

  /// Create a copy of BoutiqueModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $Res call({
    Object? id = null,
    Object? uid = null,
    Object? name = null,
    Object? description = null,
    Object? image = null,
    Object? amount = null,
    Object? alternativeOf = null,
    Object? created = null,
    Object? updated = null,
  }) {
    return _then(_BoutiqueModel(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      uid: null == uid
          ? _self.uid
          : uid // ignore: cast_nullable_to_non_nullable
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
          ? _self._image
          : image // ignore: cast_nullable_to_non_nullable
              as List<String>,
      amount: null == amount
          ? _self.amount
          : amount // ignore: cast_nullable_to_non_nullable
              as int,
      alternativeOf: null == alternativeOf
          ? _self.alternativeOf
          : alternativeOf // ignore: cast_nullable_to_non_nullable
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
