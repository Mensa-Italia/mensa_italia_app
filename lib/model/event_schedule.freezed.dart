// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'event_schedule.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$EventScheduleModel {
  String? get id;
  String get title;
  String? get event;
  String get description;
  String? get image;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get whenStart;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get whenEnd;
  int get maxExternalGuests;
  double get price;
  String get infoLink;
  bool get isSubscriptable;

  /// Create a copy of EventScheduleModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  $EventScheduleModelCopyWith<EventScheduleModel> get copyWith =>
      _$EventScheduleModelCopyWithImpl<EventScheduleModel>(
          this as EventScheduleModel, _$identity);

  /// Serializes this EventScheduleModel to a JSON map.
  Map<String, dynamic> toJson();

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is EventScheduleModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.title, title) || other.title == title) &&
            (identical(other.event, event) || other.event == event) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.image, image) || other.image == image) &&
            (identical(other.whenStart, whenStart) ||
                other.whenStart == whenStart) &&
            (identical(other.whenEnd, whenEnd) || other.whenEnd == whenEnd) &&
            (identical(other.maxExternalGuests, maxExternalGuests) ||
                other.maxExternalGuests == maxExternalGuests) &&
            (identical(other.price, price) || other.price == price) &&
            (identical(other.infoLink, infoLink) ||
                other.infoLink == infoLink) &&
            (identical(other.isSubscriptable, isSubscriptable) ||
                other.isSubscriptable == isSubscriptable));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      title,
      event,
      description,
      image,
      whenStart,
      whenEnd,
      maxExternalGuests,
      price,
      infoLink,
      isSubscriptable);

  @override
  String toString() {
    return 'EventScheduleModel(id: $id, title: $title, event: $event, description: $description, image: $image, whenStart: $whenStart, whenEnd: $whenEnd, maxExternalGuests: $maxExternalGuests, price: $price, infoLink: $infoLink, isSubscriptable: $isSubscriptable)';
  }
}

/// @nodoc
abstract mixin class $EventScheduleModelCopyWith<$Res> {
  factory $EventScheduleModelCopyWith(
          EventScheduleModel value, $Res Function(EventScheduleModel) _then) =
      _$EventScheduleModelCopyWithImpl;
  @useResult
  $Res call(
      {String? id,
      String title,
      String? event,
      String description,
      String? image,
      @JsonKey(fromJson: getDateTimeLocal) DateTime whenStart,
      @JsonKey(fromJson: getDateTimeLocal) DateTime whenEnd,
      int maxExternalGuests,
      double price,
      String infoLink,
      bool isSubscriptable});
}

/// @nodoc
class _$EventScheduleModelCopyWithImpl<$Res>
    implements $EventScheduleModelCopyWith<$Res> {
  _$EventScheduleModelCopyWithImpl(this._self, this._then);

  final EventScheduleModel _self;
  final $Res Function(EventScheduleModel) _then;

  /// Create a copy of EventScheduleModel
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = freezed,
    Object? title = null,
    Object? event = freezed,
    Object? description = null,
    Object? image = freezed,
    Object? whenStart = null,
    Object? whenEnd = null,
    Object? maxExternalGuests = null,
    Object? price = null,
    Object? infoLink = null,
    Object? isSubscriptable = null,
  }) {
    return _then(_self.copyWith(
      id: freezed == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String?,
      title: null == title
          ? _self.title
          : title // ignore: cast_nullable_to_non_nullable
              as String,
      event: freezed == event
          ? _self.event
          : event // ignore: cast_nullable_to_non_nullable
              as String?,
      description: null == description
          ? _self.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      image: freezed == image
          ? _self.image
          : image // ignore: cast_nullable_to_non_nullable
              as String?,
      whenStart: null == whenStart
          ? _self.whenStart
          : whenStart // ignore: cast_nullable_to_non_nullable
              as DateTime,
      whenEnd: null == whenEnd
          ? _self.whenEnd
          : whenEnd // ignore: cast_nullable_to_non_nullable
              as DateTime,
      maxExternalGuests: null == maxExternalGuests
          ? _self.maxExternalGuests
          : maxExternalGuests // ignore: cast_nullable_to_non_nullable
              as int,
      price: null == price
          ? _self.price
          : price // ignore: cast_nullable_to_non_nullable
              as double,
      infoLink: null == infoLink
          ? _self.infoLink
          : infoLink // ignore: cast_nullable_to_non_nullable
              as String,
      isSubscriptable: null == isSubscriptable
          ? _self.isSubscriptable
          : isSubscriptable // ignore: cast_nullable_to_non_nullable
              as bool,
    ));
  }
}

/// Adds pattern-matching-related methods to [EventScheduleModel].
extension EventScheduleModelPatterns on EventScheduleModel {
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
    TResult Function(_EventScheduleModel value)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _EventScheduleModel() when $default != null:
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
    TResult Function(_EventScheduleModel value) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _EventScheduleModel():
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
    TResult? Function(_EventScheduleModel value)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _EventScheduleModel() when $default != null:
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
            String? id,
            String title,
            String? event,
            String description,
            String? image,
            @JsonKey(fromJson: getDateTimeLocal) DateTime whenStart,
            @JsonKey(fromJson: getDateTimeLocal) DateTime whenEnd,
            int maxExternalGuests,
            double price,
            String infoLink,
            bool isSubscriptable)?
        $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _EventScheduleModel() when $default != null:
        return $default(
            _that.id,
            _that.title,
            _that.event,
            _that.description,
            _that.image,
            _that.whenStart,
            _that.whenEnd,
            _that.maxExternalGuests,
            _that.price,
            _that.infoLink,
            _that.isSubscriptable);
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
            String? id,
            String title,
            String? event,
            String description,
            String? image,
            @JsonKey(fromJson: getDateTimeLocal) DateTime whenStart,
            @JsonKey(fromJson: getDateTimeLocal) DateTime whenEnd,
            int maxExternalGuests,
            double price,
            String infoLink,
            bool isSubscriptable)
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _EventScheduleModel():
        return $default(
            _that.id,
            _that.title,
            _that.event,
            _that.description,
            _that.image,
            _that.whenStart,
            _that.whenEnd,
            _that.maxExternalGuests,
            _that.price,
            _that.infoLink,
            _that.isSubscriptable);
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
            String? id,
            String title,
            String? event,
            String description,
            String? image,
            @JsonKey(fromJson: getDateTimeLocal) DateTime whenStart,
            @JsonKey(fromJson: getDateTimeLocal) DateTime whenEnd,
            int maxExternalGuests,
            double price,
            String infoLink,
            bool isSubscriptable)?
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _EventScheduleModel() when $default != null:
        return $default(
            _that.id,
            _that.title,
            _that.event,
            _that.description,
            _that.image,
            _that.whenStart,
            _that.whenEnd,
            _that.maxExternalGuests,
            _that.price,
            _that.infoLink,
            _that.isSubscriptable);
      case _:
        return null;
    }
  }
}

/// @nodoc
@JsonSerializable()
class _EventScheduleModel implements EventScheduleModel {
  const _EventScheduleModel(
      {this.id,
      required this.title,
      this.event,
      required this.description,
      this.image,
      @JsonKey(fromJson: getDateTimeLocal) required this.whenStart,
      @JsonKey(fromJson: getDateTimeLocal) required this.whenEnd,
      required this.maxExternalGuests,
      required this.price,
      required this.infoLink,
      required this.isSubscriptable});
  factory _EventScheduleModel.fromJson(Map<String, dynamic> json) =>
      _$EventScheduleModelFromJson(json);

  @override
  final String? id;
  @override
  final String title;
  @override
  final String? event;
  @override
  final String description;
  @override
  final String? image;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime whenStart;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime whenEnd;
  @override
  final int maxExternalGuests;
  @override
  final double price;
  @override
  final String infoLink;
  @override
  final bool isSubscriptable;

  /// Create a copy of EventScheduleModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  _$EventScheduleModelCopyWith<_EventScheduleModel> get copyWith =>
      __$EventScheduleModelCopyWithImpl<_EventScheduleModel>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$EventScheduleModelToJson(
      this,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _EventScheduleModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.title, title) || other.title == title) &&
            (identical(other.event, event) || other.event == event) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.image, image) || other.image == image) &&
            (identical(other.whenStart, whenStart) ||
                other.whenStart == whenStart) &&
            (identical(other.whenEnd, whenEnd) || other.whenEnd == whenEnd) &&
            (identical(other.maxExternalGuests, maxExternalGuests) ||
                other.maxExternalGuests == maxExternalGuests) &&
            (identical(other.price, price) || other.price == price) &&
            (identical(other.infoLink, infoLink) ||
                other.infoLink == infoLink) &&
            (identical(other.isSubscriptable, isSubscriptable) ||
                other.isSubscriptable == isSubscriptable));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      title,
      event,
      description,
      image,
      whenStart,
      whenEnd,
      maxExternalGuests,
      price,
      infoLink,
      isSubscriptable);

  @override
  String toString() {
    return 'EventScheduleModel(id: $id, title: $title, event: $event, description: $description, image: $image, whenStart: $whenStart, whenEnd: $whenEnd, maxExternalGuests: $maxExternalGuests, price: $price, infoLink: $infoLink, isSubscriptable: $isSubscriptable)';
  }
}

/// @nodoc
abstract mixin class _$EventScheduleModelCopyWith<$Res>
    implements $EventScheduleModelCopyWith<$Res> {
  factory _$EventScheduleModelCopyWith(
          _EventScheduleModel value, $Res Function(_EventScheduleModel) _then) =
      __$EventScheduleModelCopyWithImpl;
  @override
  @useResult
  $Res call(
      {String? id,
      String title,
      String? event,
      String description,
      String? image,
      @JsonKey(fromJson: getDateTimeLocal) DateTime whenStart,
      @JsonKey(fromJson: getDateTimeLocal) DateTime whenEnd,
      int maxExternalGuests,
      double price,
      String infoLink,
      bool isSubscriptable});
}

/// @nodoc
class __$EventScheduleModelCopyWithImpl<$Res>
    implements _$EventScheduleModelCopyWith<$Res> {
  __$EventScheduleModelCopyWithImpl(this._self, this._then);

  final _EventScheduleModel _self;
  final $Res Function(_EventScheduleModel) _then;

  /// Create a copy of EventScheduleModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $Res call({
    Object? id = freezed,
    Object? title = null,
    Object? event = freezed,
    Object? description = null,
    Object? image = freezed,
    Object? whenStart = null,
    Object? whenEnd = null,
    Object? maxExternalGuests = null,
    Object? price = null,
    Object? infoLink = null,
    Object? isSubscriptable = null,
  }) {
    return _then(_EventScheduleModel(
      id: freezed == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String?,
      title: null == title
          ? _self.title
          : title // ignore: cast_nullable_to_non_nullable
              as String,
      event: freezed == event
          ? _self.event
          : event // ignore: cast_nullable_to_non_nullable
              as String?,
      description: null == description
          ? _self.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      image: freezed == image
          ? _self.image
          : image // ignore: cast_nullable_to_non_nullable
              as String?,
      whenStart: null == whenStart
          ? _self.whenStart
          : whenStart // ignore: cast_nullable_to_non_nullable
              as DateTime,
      whenEnd: null == whenEnd
          ? _self.whenEnd
          : whenEnd // ignore: cast_nullable_to_non_nullable
              as DateTime,
      maxExternalGuests: null == maxExternalGuests
          ? _self.maxExternalGuests
          : maxExternalGuests // ignore: cast_nullable_to_non_nullable
              as int,
      price: null == price
          ? _self.price
          : price // ignore: cast_nullable_to_non_nullable
              as double,
      infoLink: null == infoLink
          ? _self.infoLink
          : infoLink // ignore: cast_nullable_to_non_nullable
              as String,
      isSubscriptable: null == isSubscriptable
          ? _self.isSubscriptable
          : isSubscriptable // ignore: cast_nullable_to_non_nullable
              as bool,
    ));
  }
}

// dart format on
