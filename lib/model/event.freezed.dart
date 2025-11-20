// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'event.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$EventModel {
  String get id;
  String get name;
  String get image;
  String get description;
  String get infoLink;
  String get bookingLink;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get whenStart;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get whenEnd;
  String get contact;
  bool get isNational;
  bool get isSpot;
  String get owner;
  @JsonKey(readValue: getDataFromExpanded)
  LocationModel? get position;

  /// Create a copy of EventModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  $EventModelCopyWith<EventModel> get copyWith =>
      _$EventModelCopyWithImpl<EventModel>(this as EventModel, _$identity);

  /// Serializes this EventModel to a JSON map.
  Map<String, dynamic> toJson();

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is EventModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.image, image) || other.image == image) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.infoLink, infoLink) ||
                other.infoLink == infoLink) &&
            (identical(other.bookingLink, bookingLink) ||
                other.bookingLink == bookingLink) &&
            (identical(other.whenStart, whenStart) ||
                other.whenStart == whenStart) &&
            (identical(other.whenEnd, whenEnd) || other.whenEnd == whenEnd) &&
            (identical(other.contact, contact) || other.contact == contact) &&
            (identical(other.isNational, isNational) ||
                other.isNational == isNational) &&
            (identical(other.isSpot, isSpot) || other.isSpot == isSpot) &&
            (identical(other.owner, owner) || other.owner == owner) &&
            (identical(other.position, position) ||
                other.position == position));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      name,
      image,
      description,
      infoLink,
      bookingLink,
      whenStart,
      whenEnd,
      contact,
      isNational,
      isSpot,
      owner,
      position);

  @override
  String toString() {
    return 'EventModel(id: $id, name: $name, image: $image, description: $description, infoLink: $infoLink, bookingLink: $bookingLink, whenStart: $whenStart, whenEnd: $whenEnd, contact: $contact, isNational: $isNational, isSpot: $isSpot, owner: $owner, position: $position)';
  }
}

/// @nodoc
abstract mixin class $EventModelCopyWith<$Res> {
  factory $EventModelCopyWith(
          EventModel value, $Res Function(EventModel) _then) =
      _$EventModelCopyWithImpl;
  @useResult
  $Res call(
      {String id,
      String name,
      String image,
      String description,
      String infoLink,
      String bookingLink,
      @JsonKey(fromJson: getDateTimeLocal) DateTime whenStart,
      @JsonKey(fromJson: getDateTimeLocal) DateTime whenEnd,
      String contact,
      bool isNational,
      bool isSpot,
      String owner,
      @JsonKey(readValue: getDataFromExpanded) LocationModel? position});

  $LocationModelCopyWith<$Res>? get position;
}

/// @nodoc
class _$EventModelCopyWithImpl<$Res> implements $EventModelCopyWith<$Res> {
  _$EventModelCopyWithImpl(this._self, this._then);

  final EventModel _self;
  final $Res Function(EventModel) _then;

  /// Create a copy of EventModel
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? image = null,
    Object? description = null,
    Object? infoLink = null,
    Object? bookingLink = null,
    Object? whenStart = null,
    Object? whenEnd = null,
    Object? contact = null,
    Object? isNational = null,
    Object? isSpot = null,
    Object? owner = null,
    Object? position = freezed,
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
      image: null == image
          ? _self.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
      description: null == description
          ? _self.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      infoLink: null == infoLink
          ? _self.infoLink
          : infoLink // ignore: cast_nullable_to_non_nullable
              as String,
      bookingLink: null == bookingLink
          ? _self.bookingLink
          : bookingLink // ignore: cast_nullable_to_non_nullable
              as String,
      whenStart: null == whenStart
          ? _self.whenStart
          : whenStart // ignore: cast_nullable_to_non_nullable
              as DateTime,
      whenEnd: null == whenEnd
          ? _self.whenEnd
          : whenEnd // ignore: cast_nullable_to_non_nullable
              as DateTime,
      contact: null == contact
          ? _self.contact
          : contact // ignore: cast_nullable_to_non_nullable
              as String,
      isNational: null == isNational
          ? _self.isNational
          : isNational // ignore: cast_nullable_to_non_nullable
              as bool,
      isSpot: null == isSpot
          ? _self.isSpot
          : isSpot // ignore: cast_nullable_to_non_nullable
              as bool,
      owner: null == owner
          ? _self.owner
          : owner // ignore: cast_nullable_to_non_nullable
              as String,
      position: freezed == position
          ? _self.position
          : position // ignore: cast_nullable_to_non_nullable
              as LocationModel?,
    ));
  }

  /// Create a copy of EventModel
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

/// Adds pattern-matching-related methods to [EventModel].
extension EventModelPatterns on EventModel {
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
    TResult Function(_EventModel value)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _EventModel() when $default != null:
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
    TResult Function(_EventModel value) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _EventModel():
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
    TResult? Function(_EventModel value)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _EventModel() when $default != null:
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
            String image,
            String description,
            String infoLink,
            String bookingLink,
            @JsonKey(fromJson: getDateTimeLocal) DateTime whenStart,
            @JsonKey(fromJson: getDateTimeLocal) DateTime whenEnd,
            String contact,
            bool isNational,
            bool isSpot,
            String owner,
            @JsonKey(readValue: getDataFromExpanded) LocationModel? position)?
        $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _EventModel() when $default != null:
        return $default(
            _that.id,
            _that.name,
            _that.image,
            _that.description,
            _that.infoLink,
            _that.bookingLink,
            _that.whenStart,
            _that.whenEnd,
            _that.contact,
            _that.isNational,
            _that.isSpot,
            _that.owner,
            _that.position);
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
            String image,
            String description,
            String infoLink,
            String bookingLink,
            @JsonKey(fromJson: getDateTimeLocal) DateTime whenStart,
            @JsonKey(fromJson: getDateTimeLocal) DateTime whenEnd,
            String contact,
            bool isNational,
            bool isSpot,
            String owner,
            @JsonKey(readValue: getDataFromExpanded) LocationModel? position)
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _EventModel():
        return $default(
            _that.id,
            _that.name,
            _that.image,
            _that.description,
            _that.infoLink,
            _that.bookingLink,
            _that.whenStart,
            _that.whenEnd,
            _that.contact,
            _that.isNational,
            _that.isSpot,
            _that.owner,
            _that.position);
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
            String image,
            String description,
            String infoLink,
            String bookingLink,
            @JsonKey(fromJson: getDateTimeLocal) DateTime whenStart,
            @JsonKey(fromJson: getDateTimeLocal) DateTime whenEnd,
            String contact,
            bool isNational,
            bool isSpot,
            String owner,
            @JsonKey(readValue: getDataFromExpanded) LocationModel? position)?
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _EventModel() when $default != null:
        return $default(
            _that.id,
            _that.name,
            _that.image,
            _that.description,
            _that.infoLink,
            _that.bookingLink,
            _that.whenStart,
            _that.whenEnd,
            _that.contact,
            _that.isNational,
            _that.isSpot,
            _that.owner,
            _that.position);
      case _:
        return null;
    }
  }
}

/// @nodoc
@JsonSerializable()
class _EventModel implements EventModel {
  const _EventModel(
      {required this.id,
      required this.name,
      required this.image,
      required this.description,
      required this.infoLink,
      required this.bookingLink,
      @JsonKey(fromJson: getDateTimeLocal) required this.whenStart,
      @JsonKey(fromJson: getDateTimeLocal) required this.whenEnd,
      required this.contact,
      required this.isNational,
      required this.isSpot,
      required this.owner,
      @JsonKey(readValue: getDataFromExpanded) required this.position});
  factory _EventModel.fromJson(Map<String, dynamic> json) =>
      _$EventModelFromJson(json);

  @override
  final String id;
  @override
  final String name;
  @override
  final String image;
  @override
  final String description;
  @override
  final String infoLink;
  @override
  final String bookingLink;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime whenStart;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime whenEnd;
  @override
  final String contact;
  @override
  final bool isNational;
  @override
  final bool isSpot;
  @override
  final String owner;
  @override
  @JsonKey(readValue: getDataFromExpanded)
  final LocationModel? position;

  /// Create a copy of EventModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  _$EventModelCopyWith<_EventModel> get copyWith =>
      __$EventModelCopyWithImpl<_EventModel>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$EventModelToJson(
      this,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _EventModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.image, image) || other.image == image) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.infoLink, infoLink) ||
                other.infoLink == infoLink) &&
            (identical(other.bookingLink, bookingLink) ||
                other.bookingLink == bookingLink) &&
            (identical(other.whenStart, whenStart) ||
                other.whenStart == whenStart) &&
            (identical(other.whenEnd, whenEnd) || other.whenEnd == whenEnd) &&
            (identical(other.contact, contact) || other.contact == contact) &&
            (identical(other.isNational, isNational) ||
                other.isNational == isNational) &&
            (identical(other.isSpot, isSpot) || other.isSpot == isSpot) &&
            (identical(other.owner, owner) || other.owner == owner) &&
            (identical(other.position, position) ||
                other.position == position));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      name,
      image,
      description,
      infoLink,
      bookingLink,
      whenStart,
      whenEnd,
      contact,
      isNational,
      isSpot,
      owner,
      position);

  @override
  String toString() {
    return 'EventModel(id: $id, name: $name, image: $image, description: $description, infoLink: $infoLink, bookingLink: $bookingLink, whenStart: $whenStart, whenEnd: $whenEnd, contact: $contact, isNational: $isNational, isSpot: $isSpot, owner: $owner, position: $position)';
  }
}

/// @nodoc
abstract mixin class _$EventModelCopyWith<$Res>
    implements $EventModelCopyWith<$Res> {
  factory _$EventModelCopyWith(
          _EventModel value, $Res Function(_EventModel) _then) =
      __$EventModelCopyWithImpl;
  @override
  @useResult
  $Res call(
      {String id,
      String name,
      String image,
      String description,
      String infoLink,
      String bookingLink,
      @JsonKey(fromJson: getDateTimeLocal) DateTime whenStart,
      @JsonKey(fromJson: getDateTimeLocal) DateTime whenEnd,
      String contact,
      bool isNational,
      bool isSpot,
      String owner,
      @JsonKey(readValue: getDataFromExpanded) LocationModel? position});

  @override
  $LocationModelCopyWith<$Res>? get position;
}

/// @nodoc
class __$EventModelCopyWithImpl<$Res> implements _$EventModelCopyWith<$Res> {
  __$EventModelCopyWithImpl(this._self, this._then);

  final _EventModel _self;
  final $Res Function(_EventModel) _then;

  /// Create a copy of EventModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? image = null,
    Object? description = null,
    Object? infoLink = null,
    Object? bookingLink = null,
    Object? whenStart = null,
    Object? whenEnd = null,
    Object? contact = null,
    Object? isNational = null,
    Object? isSpot = null,
    Object? owner = null,
    Object? position = freezed,
  }) {
    return _then(_EventModel(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _self.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      image: null == image
          ? _self.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
      description: null == description
          ? _self.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      infoLink: null == infoLink
          ? _self.infoLink
          : infoLink // ignore: cast_nullable_to_non_nullable
              as String,
      bookingLink: null == bookingLink
          ? _self.bookingLink
          : bookingLink // ignore: cast_nullable_to_non_nullable
              as String,
      whenStart: null == whenStart
          ? _self.whenStart
          : whenStart // ignore: cast_nullable_to_non_nullable
              as DateTime,
      whenEnd: null == whenEnd
          ? _self.whenEnd
          : whenEnd // ignore: cast_nullable_to_non_nullable
              as DateTime,
      contact: null == contact
          ? _self.contact
          : contact // ignore: cast_nullable_to_non_nullable
              as String,
      isNational: null == isNational
          ? _self.isNational
          : isNational // ignore: cast_nullable_to_non_nullable
              as bool,
      isSpot: null == isSpot
          ? _self.isSpot
          : isSpot // ignore: cast_nullable_to_non_nullable
              as bool,
      owner: null == owner
          ? _self.owner
          : owner // ignore: cast_nullable_to_non_nullable
              as String,
      position: freezed == position
          ? _self.position
          : position // ignore: cast_nullable_to_non_nullable
              as LocationModel?,
    ));
  }

  /// Create a copy of EventModel
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
