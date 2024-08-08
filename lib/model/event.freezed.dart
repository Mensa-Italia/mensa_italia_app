// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'event.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

EventModel _$EventModelFromJson(Map<String, dynamic> json) {
  return _EventModel.fromJson(json);
}

/// @nodoc
mixin _$EventModel {
  String get id => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String get image => throw _privateConstructorUsedError;
  String get description => throw _privateConstructorUsedError;
  String get infoLink => throw _privateConstructorUsedError;
  String get bookingLink => throw _privateConstructorUsedError;
  DateTime get whenStart => throw _privateConstructorUsedError;
  DateTime get whenEnd => throw _privateConstructorUsedError;
  String get contact => throw _privateConstructorUsedError;
  bool get isNational => throw _privateConstructorUsedError;
  @JsonKey(readValue: getDataFromExpanded)
  LocationModel? get position => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $EventModelCopyWith<EventModel> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $EventModelCopyWith<$Res> {
  factory $EventModelCopyWith(
          EventModel value, $Res Function(EventModel) then) =
      _$EventModelCopyWithImpl<$Res, EventModel>;
  @useResult
  $Res call(
      {String id,
      String name,
      String image,
      String description,
      String infoLink,
      String bookingLink,
      DateTime whenStart,
      DateTime whenEnd,
      String contact,
      bool isNational,
      @JsonKey(readValue: getDataFromExpanded) LocationModel? position});

  $LocationModelCopyWith<$Res>? get position;
}

/// @nodoc
class _$EventModelCopyWithImpl<$Res, $Val extends EventModel>
    implements $EventModelCopyWith<$Res> {
  _$EventModelCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

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
    Object? position = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      image: null == image
          ? _value.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
      description: null == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      infoLink: null == infoLink
          ? _value.infoLink
          : infoLink // ignore: cast_nullable_to_non_nullable
              as String,
      bookingLink: null == bookingLink
          ? _value.bookingLink
          : bookingLink // ignore: cast_nullable_to_non_nullable
              as String,
      whenStart: null == whenStart
          ? _value.whenStart
          : whenStart // ignore: cast_nullable_to_non_nullable
              as DateTime,
      whenEnd: null == whenEnd
          ? _value.whenEnd
          : whenEnd // ignore: cast_nullable_to_non_nullable
              as DateTime,
      contact: null == contact
          ? _value.contact
          : contact // ignore: cast_nullable_to_non_nullable
              as String,
      isNational: null == isNational
          ? _value.isNational
          : isNational // ignore: cast_nullable_to_non_nullable
              as bool,
      position: freezed == position
          ? _value.position
          : position // ignore: cast_nullable_to_non_nullable
              as LocationModel?,
    ) as $Val);
  }

  @override
  @pragma('vm:prefer-inline')
  $LocationModelCopyWith<$Res>? get position {
    if (_value.position == null) {
      return null;
    }

    return $LocationModelCopyWith<$Res>(_value.position!, (value) {
      return _then(_value.copyWith(position: value) as $Val);
    });
  }
}

/// @nodoc
abstract class _$$EventModelImplCopyWith<$Res>
    implements $EventModelCopyWith<$Res> {
  factory _$$EventModelImplCopyWith(
          _$EventModelImpl value, $Res Function(_$EventModelImpl) then) =
      __$$EventModelImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      String name,
      String image,
      String description,
      String infoLink,
      String bookingLink,
      DateTime whenStart,
      DateTime whenEnd,
      String contact,
      bool isNational,
      @JsonKey(readValue: getDataFromExpanded) LocationModel? position});

  @override
  $LocationModelCopyWith<$Res>? get position;
}

/// @nodoc
class __$$EventModelImplCopyWithImpl<$Res>
    extends _$EventModelCopyWithImpl<$Res, _$EventModelImpl>
    implements _$$EventModelImplCopyWith<$Res> {
  __$$EventModelImplCopyWithImpl(
      _$EventModelImpl _value, $Res Function(_$EventModelImpl) _then)
      : super(_value, _then);

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
    Object? position = freezed,
  }) {
    return _then(_$EventModelImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      image: null == image
          ? _value.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
      description: null == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      infoLink: null == infoLink
          ? _value.infoLink
          : infoLink // ignore: cast_nullable_to_non_nullable
              as String,
      bookingLink: null == bookingLink
          ? _value.bookingLink
          : bookingLink // ignore: cast_nullable_to_non_nullable
              as String,
      whenStart: null == whenStart
          ? _value.whenStart
          : whenStart // ignore: cast_nullable_to_non_nullable
              as DateTime,
      whenEnd: null == whenEnd
          ? _value.whenEnd
          : whenEnd // ignore: cast_nullable_to_non_nullable
              as DateTime,
      contact: null == contact
          ? _value.contact
          : contact // ignore: cast_nullable_to_non_nullable
              as String,
      isNational: null == isNational
          ? _value.isNational
          : isNational // ignore: cast_nullable_to_non_nullable
              as bool,
      position: freezed == position
          ? _value.position
          : position // ignore: cast_nullable_to_non_nullable
              as LocationModel?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$EventModelImpl implements _EventModel {
  const _$EventModelImpl(
      {required this.id,
      required this.name,
      required this.image,
      required this.description,
      required this.infoLink,
      required this.bookingLink,
      required this.whenStart,
      required this.whenEnd,
      required this.contact,
      required this.isNational,
      @JsonKey(readValue: getDataFromExpanded) required this.position});

  factory _$EventModelImpl.fromJson(Map<String, dynamic> json) =>
      _$$EventModelImplFromJson(json);

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
  final DateTime whenStart;
  @override
  final DateTime whenEnd;
  @override
  final String contact;
  @override
  final bool isNational;
  @override
  @JsonKey(readValue: getDataFromExpanded)
  final LocationModel? position;

  @override
  String toString() {
    return 'EventModel(id: $id, name: $name, image: $image, description: $description, infoLink: $infoLink, bookingLink: $bookingLink, whenStart: $whenStart, whenEnd: $whenEnd, contact: $contact, isNational: $isNational, position: $position)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$EventModelImpl &&
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
            (identical(other.position, position) ||
                other.position == position));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode => Object.hash(runtimeType, id, name, image, description,
      infoLink, bookingLink, whenStart, whenEnd, contact, isNational, position);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$EventModelImplCopyWith<_$EventModelImpl> get copyWith =>
      __$$EventModelImplCopyWithImpl<_$EventModelImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$EventModelImplToJson(
      this,
    );
  }
}

abstract class _EventModel implements EventModel {
  const factory _EventModel(
      {required final String id,
      required final String name,
      required final String image,
      required final String description,
      required final String infoLink,
      required final String bookingLink,
      required final DateTime whenStart,
      required final DateTime whenEnd,
      required final String contact,
      required final bool isNational,
      @JsonKey(readValue: getDataFromExpanded)
      required final LocationModel? position}) = _$EventModelImpl;

  factory _EventModel.fromJson(Map<String, dynamic> json) =
      _$EventModelImpl.fromJson;

  @override
  String get id;
  @override
  String get name;
  @override
  String get image;
  @override
  String get description;
  @override
  String get infoLink;
  @override
  String get bookingLink;
  @override
  DateTime get whenStart;
  @override
  DateTime get whenEnd;
  @override
  String get contact;
  @override
  bool get isNational;
  @override
  @JsonKey(readValue: getDataFromExpanded)
  LocationModel? get position;
  @override
  @JsonKey(ignore: true)
  _$$EventModelImplCopyWith<_$EventModelImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
