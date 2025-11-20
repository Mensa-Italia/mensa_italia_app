// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'document_elaborated.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$DocumentElaboratedModel {
  String get id;
  String get document;
  String get iaResume;

  /// Create a copy of DocumentElaboratedModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  $DocumentElaboratedModelCopyWith<DocumentElaboratedModel> get copyWith =>
      _$DocumentElaboratedModelCopyWithImpl<DocumentElaboratedModel>(
          this as DocumentElaboratedModel, _$identity);

  /// Serializes this DocumentElaboratedModel to a JSON map.
  Map<String, dynamic> toJson();

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is DocumentElaboratedModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.document, document) ||
                other.document == document) &&
            (identical(other.iaResume, iaResume) ||
                other.iaResume == iaResume));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, document, iaResume);

  @override
  String toString() {
    return 'DocumentElaboratedModel(id: $id, document: $document, iaResume: $iaResume)';
  }
}

/// @nodoc
abstract mixin class $DocumentElaboratedModelCopyWith<$Res> {
  factory $DocumentElaboratedModelCopyWith(DocumentElaboratedModel value,
          $Res Function(DocumentElaboratedModel) _then) =
      _$DocumentElaboratedModelCopyWithImpl;
  @useResult
  $Res call({String id, String document, String iaResume});
}

/// @nodoc
class _$DocumentElaboratedModelCopyWithImpl<$Res>
    implements $DocumentElaboratedModelCopyWith<$Res> {
  _$DocumentElaboratedModelCopyWithImpl(this._self, this._then);

  final DocumentElaboratedModel _self;
  final $Res Function(DocumentElaboratedModel) _then;

  /// Create a copy of DocumentElaboratedModel
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? document = null,
    Object? iaResume = null,
  }) {
    return _then(_self.copyWith(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      document: null == document
          ? _self.document
          : document // ignore: cast_nullable_to_non_nullable
              as String,
      iaResume: null == iaResume
          ? _self.iaResume
          : iaResume // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

/// Adds pattern-matching-related methods to [DocumentElaboratedModel].
extension DocumentElaboratedModelPatterns on DocumentElaboratedModel {
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
    TResult Function(_DocumentElaboratedModel value)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _DocumentElaboratedModel() when $default != null:
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
    TResult Function(_DocumentElaboratedModel value) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _DocumentElaboratedModel():
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
    TResult? Function(_DocumentElaboratedModel value)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _DocumentElaboratedModel() when $default != null:
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
    TResult Function(String id, String document, String iaResume)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _DocumentElaboratedModel() when $default != null:
        return $default(_that.id, _that.document, _that.iaResume);
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
    TResult Function(String id, String document, String iaResume) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _DocumentElaboratedModel():
        return $default(_that.id, _that.document, _that.iaResume);
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
    TResult? Function(String id, String document, String iaResume)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _DocumentElaboratedModel() when $default != null:
        return $default(_that.id, _that.document, _that.iaResume);
      case _:
        return null;
    }
  }
}

/// @nodoc
@JsonSerializable()
class _DocumentElaboratedModel implements DocumentElaboratedModel {
  const _DocumentElaboratedModel(
      {required this.id, required this.document, required this.iaResume});
  factory _DocumentElaboratedModel.fromJson(Map<String, dynamic> json) =>
      _$DocumentElaboratedModelFromJson(json);

  @override
  final String id;
  @override
  final String document;
  @override
  final String iaResume;

  /// Create a copy of DocumentElaboratedModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  _$DocumentElaboratedModelCopyWith<_DocumentElaboratedModel> get copyWith =>
      __$DocumentElaboratedModelCopyWithImpl<_DocumentElaboratedModel>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$DocumentElaboratedModelToJson(
      this,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _DocumentElaboratedModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.document, document) ||
                other.document == document) &&
            (identical(other.iaResume, iaResume) ||
                other.iaResume == iaResume));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, document, iaResume);

  @override
  String toString() {
    return 'DocumentElaboratedModel(id: $id, document: $document, iaResume: $iaResume)';
  }
}

/// @nodoc
abstract mixin class _$DocumentElaboratedModelCopyWith<$Res>
    implements $DocumentElaboratedModelCopyWith<$Res> {
  factory _$DocumentElaboratedModelCopyWith(_DocumentElaboratedModel value,
          $Res Function(_DocumentElaboratedModel) _then) =
      __$DocumentElaboratedModelCopyWithImpl;
  @override
  @useResult
  $Res call({String id, String document, String iaResume});
}

/// @nodoc
class __$DocumentElaboratedModelCopyWithImpl<$Res>
    implements _$DocumentElaboratedModelCopyWith<$Res> {
  __$DocumentElaboratedModelCopyWithImpl(this._self, this._then);

  final _DocumentElaboratedModel _self;
  final $Res Function(_DocumentElaboratedModel) _then;

  /// Create a copy of DocumentElaboratedModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $Res call({
    Object? id = null,
    Object? document = null,
    Object? iaResume = null,
  }) {
    return _then(_DocumentElaboratedModel(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      document: null == document
          ? _self.document
          : document // ignore: cast_nullable_to_non_nullable
              as String,
      iaResume: null == iaResume
          ? _self.iaResume
          : iaResume // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

// dart format on
