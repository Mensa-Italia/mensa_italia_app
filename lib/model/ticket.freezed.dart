// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'ticket.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$TicketModel {
  String get id;
  String? get name;
  String? get description;
  @JsonKey(name: 'user_id')
  String? get userId;
  String? get link;
  String? get qr;
  @JsonKey(name: 'internal_ref_id')
  String? get internalRefId;
  @JsonKey(name: 'customer_data')
  String? get customerData;
  @JsonKey(fromJson: getDateTimeLocalNullabe)
  DateTime? get deadline;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get created;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get updated;

  /// Create a copy of TicketModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  $TicketModelCopyWith<TicketModel> get copyWith =>
      _$TicketModelCopyWithImpl<TicketModel>(this as TicketModel, _$identity);

  /// Serializes this TicketModel to a JSON map.
  Map<String, dynamic> toJson();

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is TicketModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.userId, userId) || other.userId == userId) &&
            (identical(other.link, link) || other.link == link) &&
            (identical(other.qr, qr) || other.qr == qr) &&
            (identical(other.internalRefId, internalRefId) ||
                other.internalRefId == internalRefId) &&
            (identical(other.customerData, customerData) ||
                other.customerData == customerData) &&
            (identical(other.deadline, deadline) ||
                other.deadline == deadline) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, name, description, userId,
      link, qr, internalRefId, customerData, deadline, created, updated);

  @override
  String toString() {
    return 'TicketModel(id: $id, name: $name, description: $description, userId: $userId, link: $link, qr: $qr, internalRefId: $internalRefId, customerData: $customerData, deadline: $deadline, created: $created, updated: $updated)';
  }
}

/// @nodoc
abstract mixin class $TicketModelCopyWith<$Res> {
  factory $TicketModelCopyWith(
          TicketModel value, $Res Function(TicketModel) _then) =
      _$TicketModelCopyWithImpl;
  @useResult
  $Res call(
      {String id,
      String? name,
      String? description,
      @JsonKey(name: 'user_id') String? userId,
      String? link,
      String? qr,
      @JsonKey(name: 'internal_ref_id') String? internalRefId,
      @JsonKey(name: 'customer_data') String? customerData,
      @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? deadline,
      @JsonKey(fromJson: getDateTimeLocal) DateTime created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime updated});
}

/// @nodoc
class _$TicketModelCopyWithImpl<$Res> implements $TicketModelCopyWith<$Res> {
  _$TicketModelCopyWithImpl(this._self, this._then);

  final TicketModel _self;
  final $Res Function(TicketModel) _then;

  /// Create a copy of TicketModel
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = freezed,
    Object? description = freezed,
    Object? userId = freezed,
    Object? link = freezed,
    Object? qr = freezed,
    Object? internalRefId = freezed,
    Object? customerData = freezed,
    Object? deadline = freezed,
    Object? created = null,
    Object? updated = null,
  }) {
    return _then(_self.copyWith(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      name: freezed == name
          ? _self.name
          : name // ignore: cast_nullable_to_non_nullable
              as String?,
      description: freezed == description
          ? _self.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      userId: freezed == userId
          ? _self.userId
          : userId // ignore: cast_nullable_to_non_nullable
              as String?,
      link: freezed == link
          ? _self.link
          : link // ignore: cast_nullable_to_non_nullable
              as String?,
      qr: freezed == qr
          ? _self.qr
          : qr // ignore: cast_nullable_to_non_nullable
              as String?,
      internalRefId: freezed == internalRefId
          ? _self.internalRefId
          : internalRefId // ignore: cast_nullable_to_non_nullable
              as String?,
      customerData: freezed == customerData
          ? _self.customerData
          : customerData // ignore: cast_nullable_to_non_nullable
              as String?,
      deadline: freezed == deadline
          ? _self.deadline
          : deadline // ignore: cast_nullable_to_non_nullable
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

/// Adds pattern-matching-related methods to [TicketModel].
extension TicketModelPatterns on TicketModel {
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
    TResult Function(_TicketModel value)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _TicketModel() when $default != null:
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
    TResult Function(_TicketModel value) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _TicketModel():
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
    TResult? Function(_TicketModel value)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _TicketModel() when $default != null:
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
            String? name,
            String? description,
            @JsonKey(name: 'user_id') String? userId,
            String? link,
            String? qr,
            @JsonKey(name: 'internal_ref_id') String? internalRefId,
            @JsonKey(name: 'customer_data') String? customerData,
            @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? deadline,
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated)?
        $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _TicketModel() when $default != null:
        return $default(
            _that.id,
            _that.name,
            _that.description,
            _that.userId,
            _that.link,
            _that.qr,
            _that.internalRefId,
            _that.customerData,
            _that.deadline,
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
            String? name,
            String? description,
            @JsonKey(name: 'user_id') String? userId,
            String? link,
            String? qr,
            @JsonKey(name: 'internal_ref_id') String? internalRefId,
            @JsonKey(name: 'customer_data') String? customerData,
            @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? deadline,
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated)
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _TicketModel():
        return $default(
            _that.id,
            _that.name,
            _that.description,
            _that.userId,
            _that.link,
            _that.qr,
            _that.internalRefId,
            _that.customerData,
            _that.deadline,
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
            String? name,
            String? description,
            @JsonKey(name: 'user_id') String? userId,
            String? link,
            String? qr,
            @JsonKey(name: 'internal_ref_id') String? internalRefId,
            @JsonKey(name: 'customer_data') String? customerData,
            @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? deadline,
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated)?
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _TicketModel() when $default != null:
        return $default(
            _that.id,
            _that.name,
            _that.description,
            _that.userId,
            _that.link,
            _that.qr,
            _that.internalRefId,
            _that.customerData,
            _that.deadline,
            _that.created,
            _that.updated);
      case _:
        return null;
    }
  }
}

/// @nodoc
@JsonSerializable()
class _TicketModel implements TicketModel {
  const _TicketModel(
      {required this.id,
      this.name,
      this.description,
      @JsonKey(name: 'user_id') this.userId,
      this.link,
      this.qr,
      @JsonKey(name: 'internal_ref_id') this.internalRefId,
      @JsonKey(name: 'customer_data') this.customerData,
      @JsonKey(fromJson: getDateTimeLocalNullabe) this.deadline,
      @JsonKey(fromJson: getDateTimeLocal) required this.created,
      @JsonKey(fromJson: getDateTimeLocal) required this.updated});
  factory _TicketModel.fromJson(Map<String, dynamic> json) =>
      _$TicketModelFromJson(json);

  @override
  final String id;
  @override
  final String? name;
  @override
  final String? description;
  @override
  @JsonKey(name: 'user_id')
  final String? userId;
  @override
  final String? link;
  @override
  final String? qr;
  @override
  @JsonKey(name: 'internal_ref_id')
  final String? internalRefId;
  @override
  @JsonKey(name: 'customer_data')
  final String? customerData;
  @override
  @JsonKey(fromJson: getDateTimeLocalNullabe)
  final DateTime? deadline;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime created;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime updated;

  /// Create a copy of TicketModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  _$TicketModelCopyWith<_TicketModel> get copyWith =>
      __$TicketModelCopyWithImpl<_TicketModel>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$TicketModelToJson(
      this,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _TicketModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.userId, userId) || other.userId == userId) &&
            (identical(other.link, link) || other.link == link) &&
            (identical(other.qr, qr) || other.qr == qr) &&
            (identical(other.internalRefId, internalRefId) ||
                other.internalRefId == internalRefId) &&
            (identical(other.customerData, customerData) ||
                other.customerData == customerData) &&
            (identical(other.deadline, deadline) ||
                other.deadline == deadline) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, name, description, userId,
      link, qr, internalRefId, customerData, deadline, created, updated);

  @override
  String toString() {
    return 'TicketModel(id: $id, name: $name, description: $description, userId: $userId, link: $link, qr: $qr, internalRefId: $internalRefId, customerData: $customerData, deadline: $deadline, created: $created, updated: $updated)';
  }
}

/// @nodoc
abstract mixin class _$TicketModelCopyWith<$Res>
    implements $TicketModelCopyWith<$Res> {
  factory _$TicketModelCopyWith(
          _TicketModel value, $Res Function(_TicketModel) _then) =
      __$TicketModelCopyWithImpl;
  @override
  @useResult
  $Res call(
      {String id,
      String? name,
      String? description,
      @JsonKey(name: 'user_id') String? userId,
      String? link,
      String? qr,
      @JsonKey(name: 'internal_ref_id') String? internalRefId,
      @JsonKey(name: 'customer_data') String? customerData,
      @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? deadline,
      @JsonKey(fromJson: getDateTimeLocal) DateTime created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime updated});
}

/// @nodoc
class __$TicketModelCopyWithImpl<$Res> implements _$TicketModelCopyWith<$Res> {
  __$TicketModelCopyWithImpl(this._self, this._then);

  final _TicketModel _self;
  final $Res Function(_TicketModel) _then;

  /// Create a copy of TicketModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $Res call({
    Object? id = null,
    Object? name = freezed,
    Object? description = freezed,
    Object? userId = freezed,
    Object? link = freezed,
    Object? qr = freezed,
    Object? internalRefId = freezed,
    Object? customerData = freezed,
    Object? deadline = freezed,
    Object? created = null,
    Object? updated = null,
  }) {
    return _then(_TicketModel(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      name: freezed == name
          ? _self.name
          : name // ignore: cast_nullable_to_non_nullable
              as String?,
      description: freezed == description
          ? _self.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      userId: freezed == userId
          ? _self.userId
          : userId // ignore: cast_nullable_to_non_nullable
              as String?,
      link: freezed == link
          ? _self.link
          : link // ignore: cast_nullable_to_non_nullable
              as String?,
      qr: freezed == qr
          ? _self.qr
          : qr // ignore: cast_nullable_to_non_nullable
              as String?,
      internalRefId: freezed == internalRefId
          ? _self.internalRefId
          : internalRefId // ignore: cast_nullable_to_non_nullable
              as String?,
      customerData: freezed == customerData
          ? _self.customerData
          : customerData // ignore: cast_nullable_to_non_nullable
              as String?,
      deadline: freezed == deadline
          ? _self.deadline
          : deadline // ignore: cast_nullable_to_non_nullable
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
