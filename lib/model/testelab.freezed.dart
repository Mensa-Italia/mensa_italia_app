// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'testelab.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$TestelabModel {
  String get id;
  String get fullname;
  String get typeOfTest;
  String get modality;
  String get status;
  String get state;

  /// Create a copy of TestelabModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  $TestelabModelCopyWith<TestelabModel> get copyWith =>
      _$TestelabModelCopyWithImpl<TestelabModel>(
          this as TestelabModel, _$identity);

  /// Serializes this TestelabModel to a JSON map.
  Map<String, dynamic> toJson();

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is TestelabModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.fullname, fullname) ||
                other.fullname == fullname) &&
            (identical(other.typeOfTest, typeOfTest) ||
                other.typeOfTest == typeOfTest) &&
            (identical(other.modality, modality) ||
                other.modality == modality) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.state, state) || other.state == state));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType, id, fullname, typeOfTest, modality, status, state);

  @override
  String toString() {
    return 'TestelabModel(id: $id, fullname: $fullname, typeOfTest: $typeOfTest, modality: $modality, status: $status, state: $state)';
  }
}

/// @nodoc
abstract mixin class $TestelabModelCopyWith<$Res> {
  factory $TestelabModelCopyWith(
          TestelabModel value, $Res Function(TestelabModel) _then) =
      _$TestelabModelCopyWithImpl;
  @useResult
  $Res call(
      {String id,
      String fullname,
      String typeOfTest,
      String modality,
      String status,
      String state});
}

/// @nodoc
class _$TestelabModelCopyWithImpl<$Res>
    implements $TestelabModelCopyWith<$Res> {
  _$TestelabModelCopyWithImpl(this._self, this._then);

  final TestelabModel _self;
  final $Res Function(TestelabModel) _then;

  /// Create a copy of TestelabModel
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? fullname = null,
    Object? typeOfTest = null,
    Object? modality = null,
    Object? status = null,
    Object? state = null,
  }) {
    return _then(_self.copyWith(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      fullname: null == fullname
          ? _self.fullname
          : fullname // ignore: cast_nullable_to_non_nullable
              as String,
      typeOfTest: null == typeOfTest
          ? _self.typeOfTest
          : typeOfTest // ignore: cast_nullable_to_non_nullable
              as String,
      modality: null == modality
          ? _self.modality
          : modality // ignore: cast_nullable_to_non_nullable
              as String,
      status: null == status
          ? _self.status
          : status // ignore: cast_nullable_to_non_nullable
              as String,
      state: null == state
          ? _self.state
          : state // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

/// Adds pattern-matching-related methods to [TestelabModel].
extension TestelabModelPatterns on TestelabModel {
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
    TResult Function(_TestelabModel value)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _TestelabModel() when $default != null:
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
    TResult Function(_TestelabModel value) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _TestelabModel():
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
    TResult? Function(_TestelabModel value)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _TestelabModel() when $default != null:
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
    TResult Function(String id, String fullname, String typeOfTest,
            String modality, String status, String state)?
        $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _TestelabModel() when $default != null:
        return $default(_that.id, _that.fullname, _that.typeOfTest,
            _that.modality, _that.status, _that.state);
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
    TResult Function(String id, String fullname, String typeOfTest,
            String modality, String status, String state)
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _TestelabModel():
        return $default(_that.id, _that.fullname, _that.typeOfTest,
            _that.modality, _that.status, _that.state);
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
    TResult? Function(String id, String fullname, String typeOfTest,
            String modality, String status, String state)?
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _TestelabModel() when $default != null:
        return $default(_that.id, _that.fullname, _that.typeOfTest,
            _that.modality, _that.status, _that.state);
      case _:
        return null;
    }
  }
}

/// @nodoc
@JsonSerializable()
class _TestelabModel extends TestelabModel {
  const _TestelabModel(
      {required this.id,
      required this.fullname,
      required this.typeOfTest,
      required this.modality,
      required this.status,
      required this.state})
      : super._();
  factory _TestelabModel.fromJson(Map<String, dynamic> json) =>
      _$TestelabModelFromJson(json);

  @override
  final String id;
  @override
  final String fullname;
  @override
  final String typeOfTest;
  @override
  final String modality;
  @override
  final String status;
  @override
  final String state;

  /// Create a copy of TestelabModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  _$TestelabModelCopyWith<_TestelabModel> get copyWith =>
      __$TestelabModelCopyWithImpl<_TestelabModel>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$TestelabModelToJson(
      this,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _TestelabModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.fullname, fullname) ||
                other.fullname == fullname) &&
            (identical(other.typeOfTest, typeOfTest) ||
                other.typeOfTest == typeOfTest) &&
            (identical(other.modality, modality) ||
                other.modality == modality) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.state, state) || other.state == state));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType, id, fullname, typeOfTest, modality, status, state);

  @override
  String toString() {
    return 'TestelabModel(id: $id, fullname: $fullname, typeOfTest: $typeOfTest, modality: $modality, status: $status, state: $state)';
  }
}

/// @nodoc
abstract mixin class _$TestelabModelCopyWith<$Res>
    implements $TestelabModelCopyWith<$Res> {
  factory _$TestelabModelCopyWith(
          _TestelabModel value, $Res Function(_TestelabModel) _then) =
      __$TestelabModelCopyWithImpl;
  @override
  @useResult
  $Res call(
      {String id,
      String fullname,
      String typeOfTest,
      String modality,
      String status,
      String state});
}

/// @nodoc
class __$TestelabModelCopyWithImpl<$Res>
    implements _$TestelabModelCopyWith<$Res> {
  __$TestelabModelCopyWithImpl(this._self, this._then);

  final _TestelabModel _self;
  final $Res Function(_TestelabModel) _then;

  /// Create a copy of TestelabModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $Res call({
    Object? id = null,
    Object? fullname = null,
    Object? typeOfTest = null,
    Object? modality = null,
    Object? status = null,
    Object? state = null,
  }) {
    return _then(_TestelabModel(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      fullname: null == fullname
          ? _self.fullname
          : fullname // ignore: cast_nullable_to_non_nullable
              as String,
      typeOfTest: null == typeOfTest
          ? _self.typeOfTest
          : typeOfTest // ignore: cast_nullable_to_non_nullable
              as String,
      modality: null == modality
          ? _self.modality
          : modality // ignore: cast_nullable_to_non_nullable
              as String,
      status: null == status
          ? _self.status
          : status // ignore: cast_nullable_to_non_nullable
              as String,
      state: null == state
          ? _self.state
          : state // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

// dart format on
