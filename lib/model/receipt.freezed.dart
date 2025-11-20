// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'receipt.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$ReceiptModel {
  String get id;
  String? get description;
  String get user;
  String get stripeCode;
  String get status;
  int get amount;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get created;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get updated;

  /// Create a copy of ReceiptModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  $ReceiptModelCopyWith<ReceiptModel> get copyWith =>
      _$ReceiptModelCopyWithImpl<ReceiptModel>(
          this as ReceiptModel, _$identity);

  /// Serializes this ReceiptModel to a JSON map.
  Map<String, dynamic> toJson();

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is ReceiptModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.user, user) || other.user == user) &&
            (identical(other.stripeCode, stripeCode) ||
                other.stripeCode == stripeCode) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.amount, amount) || other.amount == amount) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, description, user,
      stripeCode, status, amount, created, updated);

  @override
  String toString() {
    return 'ReceiptModel(id: $id, description: $description, user: $user, stripeCode: $stripeCode, status: $status, amount: $amount, created: $created, updated: $updated)';
  }
}

/// @nodoc
abstract mixin class $ReceiptModelCopyWith<$Res> {
  factory $ReceiptModelCopyWith(
          ReceiptModel value, $Res Function(ReceiptModel) _then) =
      _$ReceiptModelCopyWithImpl;
  @useResult
  $Res call(
      {String id,
      String? description,
      String user,
      String stripeCode,
      String status,
      int amount,
      @JsonKey(fromJson: getDateTimeLocal) DateTime created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime updated});
}

/// @nodoc
class _$ReceiptModelCopyWithImpl<$Res> implements $ReceiptModelCopyWith<$Res> {
  _$ReceiptModelCopyWithImpl(this._self, this._then);

  final ReceiptModel _self;
  final $Res Function(ReceiptModel) _then;

  /// Create a copy of ReceiptModel
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? description = freezed,
    Object? user = null,
    Object? stripeCode = null,
    Object? status = null,
    Object? amount = null,
    Object? created = null,
    Object? updated = null,
  }) {
    return _then(_self.copyWith(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _self.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      user: null == user
          ? _self.user
          : user // ignore: cast_nullable_to_non_nullable
              as String,
      stripeCode: null == stripeCode
          ? _self.stripeCode
          : stripeCode // ignore: cast_nullable_to_non_nullable
              as String,
      status: null == status
          ? _self.status
          : status // ignore: cast_nullable_to_non_nullable
              as String,
      amount: null == amount
          ? _self.amount
          : amount // ignore: cast_nullable_to_non_nullable
              as int,
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

/// Adds pattern-matching-related methods to [ReceiptModel].
extension ReceiptModelPatterns on ReceiptModel {
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
    TResult Function(_ReceiptModel value)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _ReceiptModel() when $default != null:
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
    TResult Function(_ReceiptModel value) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _ReceiptModel():
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
    TResult? Function(_ReceiptModel value)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _ReceiptModel() when $default != null:
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
            String? description,
            String user,
            String stripeCode,
            String status,
            int amount,
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated)?
        $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _ReceiptModel() when $default != null:
        return $default(
            _that.id,
            _that.description,
            _that.user,
            _that.stripeCode,
            _that.status,
            _that.amount,
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
            String? description,
            String user,
            String stripeCode,
            String status,
            int amount,
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated)
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _ReceiptModel():
        return $default(
            _that.id,
            _that.description,
            _that.user,
            _that.stripeCode,
            _that.status,
            _that.amount,
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
            String? description,
            String user,
            String stripeCode,
            String status,
            int amount,
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated)?
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _ReceiptModel() when $default != null:
        return $default(
            _that.id,
            _that.description,
            _that.user,
            _that.stripeCode,
            _that.status,
            _that.amount,
            _that.created,
            _that.updated);
      case _:
        return null;
    }
  }
}

/// @nodoc
@JsonSerializable()
class _ReceiptModel implements ReceiptModel {
  const _ReceiptModel(
      {required this.id,
      required this.description,
      required this.user,
      required this.stripeCode,
      required this.status,
      required this.amount,
      @JsonKey(fromJson: getDateTimeLocal) required this.created,
      @JsonKey(fromJson: getDateTimeLocal) required this.updated});
  factory _ReceiptModel.fromJson(Map<String, dynamic> json) =>
      _$ReceiptModelFromJson(json);

  @override
  final String id;
  @override
  final String? description;
  @override
  final String user;
  @override
  final String stripeCode;
  @override
  final String status;
  @override
  final int amount;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime created;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime updated;

  /// Create a copy of ReceiptModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  _$ReceiptModelCopyWith<_ReceiptModel> get copyWith =>
      __$ReceiptModelCopyWithImpl<_ReceiptModel>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$ReceiptModelToJson(
      this,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _ReceiptModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.user, user) || other.user == user) &&
            (identical(other.stripeCode, stripeCode) ||
                other.stripeCode == stripeCode) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.amount, amount) || other.amount == amount) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, description, user,
      stripeCode, status, amount, created, updated);

  @override
  String toString() {
    return 'ReceiptModel(id: $id, description: $description, user: $user, stripeCode: $stripeCode, status: $status, amount: $amount, created: $created, updated: $updated)';
  }
}

/// @nodoc
abstract mixin class _$ReceiptModelCopyWith<$Res>
    implements $ReceiptModelCopyWith<$Res> {
  factory _$ReceiptModelCopyWith(
          _ReceiptModel value, $Res Function(_ReceiptModel) _then) =
      __$ReceiptModelCopyWithImpl;
  @override
  @useResult
  $Res call(
      {String id,
      String? description,
      String user,
      String stripeCode,
      String status,
      int amount,
      @JsonKey(fromJson: getDateTimeLocal) DateTime created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime updated});
}

/// @nodoc
class __$ReceiptModelCopyWithImpl<$Res>
    implements _$ReceiptModelCopyWith<$Res> {
  __$ReceiptModelCopyWithImpl(this._self, this._then);

  final _ReceiptModel _self;
  final $Res Function(_ReceiptModel) _then;

  /// Create a copy of ReceiptModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $Res call({
    Object? id = null,
    Object? description = freezed,
    Object? user = null,
    Object? stripeCode = null,
    Object? status = null,
    Object? amount = null,
    Object? created = null,
    Object? updated = null,
  }) {
    return _then(_ReceiptModel(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _self.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      user: null == user
          ? _self.user
          : user // ignore: cast_nullable_to_non_nullable
              as String,
      stripeCode: null == stripeCode
          ? _self.stripeCode
          : stripeCode // ignore: cast_nullable_to_non_nullable
              as String,
      status: null == status
          ? _self.status
          : status // ignore: cast_nullable_to_non_nullable
              as String,
      amount: null == amount
          ? _self.amount
          : amount // ignore: cast_nullable_to_non_nullable
              as int,
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
