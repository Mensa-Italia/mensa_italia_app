// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'document.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$DocumentModel {
  String get id;
  String get name;
  String? get description;
  String get file;
  String get uploadedBy;
  String get category;
  String get elaborated;

  /// Create a copy of DocumentModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  $DocumentModelCopyWith<DocumentModel> get copyWith =>
      _$DocumentModelCopyWithImpl<DocumentModel>(
          this as DocumentModel, _$identity);

  /// Serializes this DocumentModel to a JSON map.
  Map<String, dynamic> toJson();

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is DocumentModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.file, file) || other.file == file) &&
            (identical(other.uploadedBy, uploadedBy) ||
                other.uploadedBy == uploadedBy) &&
            (identical(other.category, category) ||
                other.category == category) &&
            (identical(other.elaborated, elaborated) ||
                other.elaborated == elaborated));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, name, description, file,
      uploadedBy, category, elaborated);

  @override
  String toString() {
    return 'DocumentModel(id: $id, name: $name, description: $description, file: $file, uploadedBy: $uploadedBy, category: $category, elaborated: $elaborated)';
  }
}

/// @nodoc
abstract mixin class $DocumentModelCopyWith<$Res> {
  factory $DocumentModelCopyWith(
          DocumentModel value, $Res Function(DocumentModel) _then) =
      _$DocumentModelCopyWithImpl;
  @useResult
  $Res call(
      {String id,
      String name,
      String? description,
      String file,
      String uploadedBy,
      String category,
      String elaborated});
}

/// @nodoc
class _$DocumentModelCopyWithImpl<$Res>
    implements $DocumentModelCopyWith<$Res> {
  _$DocumentModelCopyWithImpl(this._self, this._then);

  final DocumentModel _self;
  final $Res Function(DocumentModel) _then;

  /// Create a copy of DocumentModel
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? description = freezed,
    Object? file = null,
    Object? uploadedBy = null,
    Object? category = null,
    Object? elaborated = null,
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
      description: freezed == description
          ? _self.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      file: null == file
          ? _self.file
          : file // ignore: cast_nullable_to_non_nullable
              as String,
      uploadedBy: null == uploadedBy
          ? _self.uploadedBy
          : uploadedBy // ignore: cast_nullable_to_non_nullable
              as String,
      category: null == category
          ? _self.category
          : category // ignore: cast_nullable_to_non_nullable
              as String,
      elaborated: null == elaborated
          ? _self.elaborated
          : elaborated // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

/// Adds pattern-matching-related methods to [DocumentModel].
extension DocumentModelPatterns on DocumentModel {
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
    TResult Function(_DocumentModel value)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _DocumentModel() when $default != null:
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
    TResult Function(_DocumentModel value) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _DocumentModel():
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
    TResult? Function(_DocumentModel value)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _DocumentModel() when $default != null:
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
    TResult Function(String id, String name, String? description, String file,
            String uploadedBy, String category, String elaborated)?
        $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _DocumentModel() when $default != null:
        return $default(_that.id, _that.name, _that.description, _that.file,
            _that.uploadedBy, _that.category, _that.elaborated);
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
    TResult Function(String id, String name, String? description, String file,
            String uploadedBy, String category, String elaborated)
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _DocumentModel():
        return $default(_that.id, _that.name, _that.description, _that.file,
            _that.uploadedBy, _that.category, _that.elaborated);
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
    TResult? Function(String id, String name, String? description, String file,
            String uploadedBy, String category, String elaborated)?
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _DocumentModel() when $default != null:
        return $default(_that.id, _that.name, _that.description, _that.file,
            _that.uploadedBy, _that.category, _that.elaborated);
      case _:
        return null;
    }
  }
}

/// @nodoc
@JsonSerializable()
class _DocumentModel implements DocumentModel {
  const _DocumentModel(
      {required this.id,
      required this.name,
      required this.description,
      required this.file,
      required this.uploadedBy,
      required this.category,
      required this.elaborated});
  factory _DocumentModel.fromJson(Map<String, dynamic> json) =>
      _$DocumentModelFromJson(json);

  @override
  final String id;
  @override
  final String name;
  @override
  final String? description;
  @override
  final String file;
  @override
  final String uploadedBy;
  @override
  final String category;
  @override
  final String elaborated;

  /// Create a copy of DocumentModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  _$DocumentModelCopyWith<_DocumentModel> get copyWith =>
      __$DocumentModelCopyWithImpl<_DocumentModel>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$DocumentModelToJson(
      this,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _DocumentModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.file, file) || other.file == file) &&
            (identical(other.uploadedBy, uploadedBy) ||
                other.uploadedBy == uploadedBy) &&
            (identical(other.category, category) ||
                other.category == category) &&
            (identical(other.elaborated, elaborated) ||
                other.elaborated == elaborated));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, name, description, file,
      uploadedBy, category, elaborated);

  @override
  String toString() {
    return 'DocumentModel(id: $id, name: $name, description: $description, file: $file, uploadedBy: $uploadedBy, category: $category, elaborated: $elaborated)';
  }
}

/// @nodoc
abstract mixin class _$DocumentModelCopyWith<$Res>
    implements $DocumentModelCopyWith<$Res> {
  factory _$DocumentModelCopyWith(
          _DocumentModel value, $Res Function(_DocumentModel) _then) =
      __$DocumentModelCopyWithImpl;
  @override
  @useResult
  $Res call(
      {String id,
      String name,
      String? description,
      String file,
      String uploadedBy,
      String category,
      String elaborated});
}

/// @nodoc
class __$DocumentModelCopyWithImpl<$Res>
    implements _$DocumentModelCopyWith<$Res> {
  __$DocumentModelCopyWithImpl(this._self, this._then);

  final _DocumentModel _self;
  final $Res Function(_DocumentModel) _then;

  /// Create a copy of DocumentModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? description = freezed,
    Object? file = null,
    Object? uploadedBy = null,
    Object? category = null,
    Object? elaborated = null,
  }) {
    return _then(_DocumentModel(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _self.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _self.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      file: null == file
          ? _self.file
          : file // ignore: cast_nullable_to_non_nullable
              as String,
      uploadedBy: null == uploadedBy
          ? _self.uploadedBy
          : uploadedBy // ignore: cast_nullable_to_non_nullable
              as String,
      category: null == category
          ? _self.category
          : category // ignore: cast_nullable_to_non_nullable
              as String,
      elaborated: null == elaborated
          ? _self.elaborated
          : elaborated // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

// dart format on
