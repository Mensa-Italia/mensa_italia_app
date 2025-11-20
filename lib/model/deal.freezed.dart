// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'deal.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$DealModel {
  String get id;
  String get name;
  String get commercialSector;
  @JsonKey(readValue: getDataFromExpanded)
  LocationModel? get position;
  bool get isLocal;
  String? get details;
  String? get who;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime? get starting;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime? get ending;
  String? get howToGet;
  String? get link;
  String? get owner;
  String? get attachment;
  bool get isActive;
  String? get vatNumber;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get created;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get updated;

  /// Create a copy of DealModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  $DealModelCopyWith<DealModel> get copyWith =>
      _$DealModelCopyWithImpl<DealModel>(this as DealModel, _$identity);

  /// Serializes this DealModel to a JSON map.
  Map<String, dynamic> toJson();

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is DealModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.commercialSector, commercialSector) ||
                other.commercialSector == commercialSector) &&
            (identical(other.position, position) ||
                other.position == position) &&
            (identical(other.isLocal, isLocal) || other.isLocal == isLocal) &&
            (identical(other.details, details) || other.details == details) &&
            (identical(other.who, who) || other.who == who) &&
            (identical(other.starting, starting) ||
                other.starting == starting) &&
            (identical(other.ending, ending) || other.ending == ending) &&
            (identical(other.howToGet, howToGet) ||
                other.howToGet == howToGet) &&
            (identical(other.link, link) || other.link == link) &&
            (identical(other.owner, owner) || other.owner == owner) &&
            (identical(other.attachment, attachment) ||
                other.attachment == attachment) &&
            (identical(other.isActive, isActive) ||
                other.isActive == isActive) &&
            (identical(other.vatNumber, vatNumber) ||
                other.vatNumber == vatNumber) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      name,
      commercialSector,
      position,
      isLocal,
      details,
      who,
      starting,
      ending,
      howToGet,
      link,
      owner,
      attachment,
      isActive,
      vatNumber,
      created,
      updated);

  @override
  String toString() {
    return 'DealModel(id: $id, name: $name, commercialSector: $commercialSector, position: $position, isLocal: $isLocal, details: $details, who: $who, starting: $starting, ending: $ending, howToGet: $howToGet, link: $link, owner: $owner, attachment: $attachment, isActive: $isActive, vatNumber: $vatNumber, created: $created, updated: $updated)';
  }
}

/// @nodoc
abstract mixin class $DealModelCopyWith<$Res> {
  factory $DealModelCopyWith(DealModel value, $Res Function(DealModel) _then) =
      _$DealModelCopyWithImpl;
  @useResult
  $Res call(
      {String id,
      String name,
      String commercialSector,
      @JsonKey(readValue: getDataFromExpanded) LocationModel? position,
      bool isLocal,
      String? details,
      String? who,
      @JsonKey(fromJson: getDateTimeLocal) DateTime? starting,
      @JsonKey(fromJson: getDateTimeLocal) DateTime? ending,
      String? howToGet,
      String? link,
      String? owner,
      String? attachment,
      bool isActive,
      String? vatNumber,
      @JsonKey(fromJson: getDateTimeLocal) DateTime created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime updated});

  $LocationModelCopyWith<$Res>? get position;
}

/// @nodoc
class _$DealModelCopyWithImpl<$Res> implements $DealModelCopyWith<$Res> {
  _$DealModelCopyWithImpl(this._self, this._then);

  final DealModel _self;
  final $Res Function(DealModel) _then;

  /// Create a copy of DealModel
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? commercialSector = null,
    Object? position = freezed,
    Object? isLocal = null,
    Object? details = freezed,
    Object? who = freezed,
    Object? starting = freezed,
    Object? ending = freezed,
    Object? howToGet = freezed,
    Object? link = freezed,
    Object? owner = freezed,
    Object? attachment = freezed,
    Object? isActive = null,
    Object? vatNumber = freezed,
    Object? created = null,
    Object? updated = null,
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
      commercialSector: null == commercialSector
          ? _self.commercialSector
          : commercialSector // ignore: cast_nullable_to_non_nullable
              as String,
      position: freezed == position
          ? _self.position
          : position // ignore: cast_nullable_to_non_nullable
              as LocationModel?,
      isLocal: null == isLocal
          ? _self.isLocal
          : isLocal // ignore: cast_nullable_to_non_nullable
              as bool,
      details: freezed == details
          ? _self.details
          : details // ignore: cast_nullable_to_non_nullable
              as String?,
      who: freezed == who
          ? _self.who
          : who // ignore: cast_nullable_to_non_nullable
              as String?,
      starting: freezed == starting
          ? _self.starting
          : starting // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      ending: freezed == ending
          ? _self.ending
          : ending // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      howToGet: freezed == howToGet
          ? _self.howToGet
          : howToGet // ignore: cast_nullable_to_non_nullable
              as String?,
      link: freezed == link
          ? _self.link
          : link // ignore: cast_nullable_to_non_nullable
              as String?,
      owner: freezed == owner
          ? _self.owner
          : owner // ignore: cast_nullable_to_non_nullable
              as String?,
      attachment: freezed == attachment
          ? _self.attachment
          : attachment // ignore: cast_nullable_to_non_nullable
              as String?,
      isActive: null == isActive
          ? _self.isActive
          : isActive // ignore: cast_nullable_to_non_nullable
              as bool,
      vatNumber: freezed == vatNumber
          ? _self.vatNumber
          : vatNumber // ignore: cast_nullable_to_non_nullable
              as String?,
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

  /// Create a copy of DealModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $LocationModelCopyWith<$Res>? get position {
    if (_self.position == null) {
      return null;
    }

    return $LocationModelCopyWith<$Res>(_self.position!, (value) {
      return _then(_self.copyWith(position: value));
    });
  }
}

/// Adds pattern-matching-related methods to [DealModel].
extension DealModelPatterns on DealModel {
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
    TResult Function(_DealModel value)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _DealModel() when $default != null:
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
    TResult Function(_DealModel value) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _DealModel():
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
    TResult? Function(_DealModel value)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _DealModel() when $default != null:
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
            String name,
            String commercialSector,
            @JsonKey(readValue: getDataFromExpanded) LocationModel? position,
            bool isLocal,
            String? details,
            String? who,
            @JsonKey(fromJson: getDateTimeLocal) DateTime? starting,
            @JsonKey(fromJson: getDateTimeLocal) DateTime? ending,
            String? howToGet,
            String? link,
            String? owner,
            String? attachment,
            bool isActive,
            String? vatNumber,
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated)?
        $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _DealModel() when $default != null:
        return $default(
            _that.id,
            _that.name,
            _that.commercialSector,
            _that.position,
            _that.isLocal,
            _that.details,
            _that.who,
            _that.starting,
            _that.ending,
            _that.howToGet,
            _that.link,
            _that.owner,
            _that.attachment,
            _that.isActive,
            _that.vatNumber,
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
            String name,
            String commercialSector,
            @JsonKey(readValue: getDataFromExpanded) LocationModel? position,
            bool isLocal,
            String? details,
            String? who,
            @JsonKey(fromJson: getDateTimeLocal) DateTime? starting,
            @JsonKey(fromJson: getDateTimeLocal) DateTime? ending,
            String? howToGet,
            String? link,
            String? owner,
            String? attachment,
            bool isActive,
            String? vatNumber,
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated)
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _DealModel():
        return $default(
            _that.id,
            _that.name,
            _that.commercialSector,
            _that.position,
            _that.isLocal,
            _that.details,
            _that.who,
            _that.starting,
            _that.ending,
            _that.howToGet,
            _that.link,
            _that.owner,
            _that.attachment,
            _that.isActive,
            _that.vatNumber,
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
            String name,
            String commercialSector,
            @JsonKey(readValue: getDataFromExpanded) LocationModel? position,
            bool isLocal,
            String? details,
            String? who,
            @JsonKey(fromJson: getDateTimeLocal) DateTime? starting,
            @JsonKey(fromJson: getDateTimeLocal) DateTime? ending,
            String? howToGet,
            String? link,
            String? owner,
            String? attachment,
            bool isActive,
            String? vatNumber,
            @JsonKey(fromJson: getDateTimeLocal) DateTime created,
            @JsonKey(fromJson: getDateTimeLocal) DateTime updated)?
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _DealModel() when $default != null:
        return $default(
            _that.id,
            _that.name,
            _that.commercialSector,
            _that.position,
            _that.isLocal,
            _that.details,
            _that.who,
            _that.starting,
            _that.ending,
            _that.howToGet,
            _that.link,
            _that.owner,
            _that.attachment,
            _that.isActive,
            _that.vatNumber,
            _that.created,
            _that.updated);
      case _:
        return null;
    }
  }
}

/// @nodoc
@JsonSerializable()
class _DealModel extends DealModel {
  const _DealModel(
      {required this.id,
      required this.name,
      required this.commercialSector,
      @JsonKey(readValue: getDataFromExpanded) required this.position,
      required this.isLocal,
      this.details,
      this.who,
      @JsonKey(fromJson: getDateTimeLocal) this.starting,
      @JsonKey(fromJson: getDateTimeLocal) this.ending,
      this.howToGet,
      this.link,
      this.owner,
      this.attachment,
      required this.isActive,
      this.vatNumber,
      @JsonKey(fromJson: getDateTimeLocal) required this.created,
      @JsonKey(fromJson: getDateTimeLocal) required this.updated})
      : super._();
  factory _DealModel.fromJson(Map<String, dynamic> json) =>
      _$DealModelFromJson(json);

  @override
  final String id;
  @override
  final String name;
  @override
  final String commercialSector;
  @override
  @JsonKey(readValue: getDataFromExpanded)
  final LocationModel? position;
  @override
  final bool isLocal;
  @override
  final String? details;
  @override
  final String? who;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime? starting;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime? ending;
  @override
  final String? howToGet;
  @override
  final String? link;
  @override
  final String? owner;
  @override
  final String? attachment;
  @override
  final bool isActive;
  @override
  final String? vatNumber;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime created;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime updated;

  /// Create a copy of DealModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  _$DealModelCopyWith<_DealModel> get copyWith =>
      __$DealModelCopyWithImpl<_DealModel>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$DealModelToJson(
      this,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _DealModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.commercialSector, commercialSector) ||
                other.commercialSector == commercialSector) &&
            (identical(other.position, position) ||
                other.position == position) &&
            (identical(other.isLocal, isLocal) || other.isLocal == isLocal) &&
            (identical(other.details, details) || other.details == details) &&
            (identical(other.who, who) || other.who == who) &&
            (identical(other.starting, starting) ||
                other.starting == starting) &&
            (identical(other.ending, ending) || other.ending == ending) &&
            (identical(other.howToGet, howToGet) ||
                other.howToGet == howToGet) &&
            (identical(other.link, link) || other.link == link) &&
            (identical(other.owner, owner) || other.owner == owner) &&
            (identical(other.attachment, attachment) ||
                other.attachment == attachment) &&
            (identical(other.isActive, isActive) ||
                other.isActive == isActive) &&
            (identical(other.vatNumber, vatNumber) ||
                other.vatNumber == vatNumber) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      name,
      commercialSector,
      position,
      isLocal,
      details,
      who,
      starting,
      ending,
      howToGet,
      link,
      owner,
      attachment,
      isActive,
      vatNumber,
      created,
      updated);

  @override
  String toString() {
    return 'DealModel(id: $id, name: $name, commercialSector: $commercialSector, position: $position, isLocal: $isLocal, details: $details, who: $who, starting: $starting, ending: $ending, howToGet: $howToGet, link: $link, owner: $owner, attachment: $attachment, isActive: $isActive, vatNumber: $vatNumber, created: $created, updated: $updated)';
  }
}

/// @nodoc
abstract mixin class _$DealModelCopyWith<$Res>
    implements $DealModelCopyWith<$Res> {
  factory _$DealModelCopyWith(
          _DealModel value, $Res Function(_DealModel) _then) =
      __$DealModelCopyWithImpl;
  @override
  @useResult
  $Res call(
      {String id,
      String name,
      String commercialSector,
      @JsonKey(readValue: getDataFromExpanded) LocationModel? position,
      bool isLocal,
      String? details,
      String? who,
      @JsonKey(fromJson: getDateTimeLocal) DateTime? starting,
      @JsonKey(fromJson: getDateTimeLocal) DateTime? ending,
      String? howToGet,
      String? link,
      String? owner,
      String? attachment,
      bool isActive,
      String? vatNumber,
      @JsonKey(fromJson: getDateTimeLocal) DateTime created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime updated});

  @override
  $LocationModelCopyWith<$Res>? get position;
}

/// @nodoc
class __$DealModelCopyWithImpl<$Res> implements _$DealModelCopyWith<$Res> {
  __$DealModelCopyWithImpl(this._self, this._then);

  final _DealModel _self;
  final $Res Function(_DealModel) _then;

  /// Create a copy of DealModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? commercialSector = null,
    Object? position = freezed,
    Object? isLocal = null,
    Object? details = freezed,
    Object? who = freezed,
    Object? starting = freezed,
    Object? ending = freezed,
    Object? howToGet = freezed,
    Object? link = freezed,
    Object? owner = freezed,
    Object? attachment = freezed,
    Object? isActive = null,
    Object? vatNumber = freezed,
    Object? created = null,
    Object? updated = null,
  }) {
    return _then(_DealModel(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _self.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      commercialSector: null == commercialSector
          ? _self.commercialSector
          : commercialSector // ignore: cast_nullable_to_non_nullable
              as String,
      position: freezed == position
          ? _self.position
          : position // ignore: cast_nullable_to_non_nullable
              as LocationModel?,
      isLocal: null == isLocal
          ? _self.isLocal
          : isLocal // ignore: cast_nullable_to_non_nullable
              as bool,
      details: freezed == details
          ? _self.details
          : details // ignore: cast_nullable_to_non_nullable
              as String?,
      who: freezed == who
          ? _self.who
          : who // ignore: cast_nullable_to_non_nullable
              as String?,
      starting: freezed == starting
          ? _self.starting
          : starting // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      ending: freezed == ending
          ? _self.ending
          : ending // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      howToGet: freezed == howToGet
          ? _self.howToGet
          : howToGet // ignore: cast_nullable_to_non_nullable
              as String?,
      link: freezed == link
          ? _self.link
          : link // ignore: cast_nullable_to_non_nullable
              as String?,
      owner: freezed == owner
          ? _self.owner
          : owner // ignore: cast_nullable_to_non_nullable
              as String?,
      attachment: freezed == attachment
          ? _self.attachment
          : attachment // ignore: cast_nullable_to_non_nullable
              as String?,
      isActive: null == isActive
          ? _self.isActive
          : isActive // ignore: cast_nullable_to_non_nullable
              as bool,
      vatNumber: freezed == vatNumber
          ? _self.vatNumber
          : vatNumber // ignore: cast_nullable_to_non_nullable
              as String?,
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

  /// Create a copy of DealModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $LocationModelCopyWith<$Res>? get position {
    if (_self.position == null) {
      return null;
    }

    return $LocationModelCopyWith<$Res>(_self.position!, (value) {
      return _then(_self.copyWith(position: value));
    });
  }
}

// dart format on
