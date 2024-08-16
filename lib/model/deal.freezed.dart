// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'deal.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

DealModel _$DealModelFromJson(Map<String, dynamic> json) {
  return _DealModel.fromJson(json);
}

/// @nodoc
mixin _$DealModel {
  String get id => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String get commercialSector => throw _privateConstructorUsedError;
  @JsonKey(readValue: getDataFromExpanded)
  LocationModel? get position => throw _privateConstructorUsedError;
  bool get isLocal => throw _privateConstructorUsedError;
  String? get details => throw _privateConstructorUsedError;
  String? get who => throw _privateConstructorUsedError;
  DateTime? get starting => throw _privateConstructorUsedError;
  DateTime? get ending => throw _privateConstructorUsedError;
  String? get howToGet => throw _privateConstructorUsedError;
  String? get link => throw _privateConstructorUsedError;
  String? get owner => throw _privateConstructorUsedError;
  String? get attachment => throw _privateConstructorUsedError;
  bool get isActive => throw _privateConstructorUsedError;
  String? get vatNumber => throw _privateConstructorUsedError;
  DateTime get created => throw _privateConstructorUsedError;
  DateTime get updated => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $DealModelCopyWith<DealModel> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $DealModelCopyWith<$Res> {
  factory $DealModelCopyWith(DealModel value, $Res Function(DealModel) then) =
      _$DealModelCopyWithImpl<$Res, DealModel>;
  @useResult
  $Res call(
      {String id,
      String name,
      String commercialSector,
      @JsonKey(readValue: getDataFromExpanded) LocationModel? position,
      bool isLocal,
      String? details,
      String? who,
      DateTime? starting,
      DateTime? ending,
      String? howToGet,
      String? link,
      String? owner,
      String? attachment,
      bool isActive,
      String? vatNumber,
      DateTime created,
      DateTime updated});

  $LocationModelCopyWith<$Res>? get position;
}

/// @nodoc
class _$DealModelCopyWithImpl<$Res, $Val extends DealModel>
    implements $DealModelCopyWith<$Res> {
  _$DealModelCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

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
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      commercialSector: null == commercialSector
          ? _value.commercialSector
          : commercialSector // ignore: cast_nullable_to_non_nullable
              as String,
      position: freezed == position
          ? _value.position
          : position // ignore: cast_nullable_to_non_nullable
              as LocationModel?,
      isLocal: null == isLocal
          ? _value.isLocal
          : isLocal // ignore: cast_nullable_to_non_nullable
              as bool,
      details: freezed == details
          ? _value.details
          : details // ignore: cast_nullable_to_non_nullable
              as String?,
      who: freezed == who
          ? _value.who
          : who // ignore: cast_nullable_to_non_nullable
              as String?,
      starting: freezed == starting
          ? _value.starting
          : starting // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      ending: freezed == ending
          ? _value.ending
          : ending // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      howToGet: freezed == howToGet
          ? _value.howToGet
          : howToGet // ignore: cast_nullable_to_non_nullable
              as String?,
      link: freezed == link
          ? _value.link
          : link // ignore: cast_nullable_to_non_nullable
              as String?,
      owner: freezed == owner
          ? _value.owner
          : owner // ignore: cast_nullable_to_non_nullable
              as String?,
      attachment: freezed == attachment
          ? _value.attachment
          : attachment // ignore: cast_nullable_to_non_nullable
              as String?,
      isActive: null == isActive
          ? _value.isActive
          : isActive // ignore: cast_nullable_to_non_nullable
              as bool,
      vatNumber: freezed == vatNumber
          ? _value.vatNumber
          : vatNumber // ignore: cast_nullable_to_non_nullable
              as String?,
      created: null == created
          ? _value.created
          : created // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updated: null == updated
          ? _value.updated
          : updated // ignore: cast_nullable_to_non_nullable
              as DateTime,
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
abstract class _$$DealModelImplCopyWith<$Res>
    implements $DealModelCopyWith<$Res> {
  factory _$$DealModelImplCopyWith(
          _$DealModelImpl value, $Res Function(_$DealModelImpl) then) =
      __$$DealModelImplCopyWithImpl<$Res>;
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
      DateTime? starting,
      DateTime? ending,
      String? howToGet,
      String? link,
      String? owner,
      String? attachment,
      bool isActive,
      String? vatNumber,
      DateTime created,
      DateTime updated});

  @override
  $LocationModelCopyWith<$Res>? get position;
}

/// @nodoc
class __$$DealModelImplCopyWithImpl<$Res>
    extends _$DealModelCopyWithImpl<$Res, _$DealModelImpl>
    implements _$$DealModelImplCopyWith<$Res> {
  __$$DealModelImplCopyWithImpl(
      _$DealModelImpl _value, $Res Function(_$DealModelImpl) _then)
      : super(_value, _then);

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
    return _then(_$DealModelImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      commercialSector: null == commercialSector
          ? _value.commercialSector
          : commercialSector // ignore: cast_nullable_to_non_nullable
              as String,
      position: freezed == position
          ? _value.position
          : position // ignore: cast_nullable_to_non_nullable
              as LocationModel?,
      isLocal: null == isLocal
          ? _value.isLocal
          : isLocal // ignore: cast_nullable_to_non_nullable
              as bool,
      details: freezed == details
          ? _value.details
          : details // ignore: cast_nullable_to_non_nullable
              as String?,
      who: freezed == who
          ? _value.who
          : who // ignore: cast_nullable_to_non_nullable
              as String?,
      starting: freezed == starting
          ? _value.starting
          : starting // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      ending: freezed == ending
          ? _value.ending
          : ending // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      howToGet: freezed == howToGet
          ? _value.howToGet
          : howToGet // ignore: cast_nullable_to_non_nullable
              as String?,
      link: freezed == link
          ? _value.link
          : link // ignore: cast_nullable_to_non_nullable
              as String?,
      owner: freezed == owner
          ? _value.owner
          : owner // ignore: cast_nullable_to_non_nullable
              as String?,
      attachment: freezed == attachment
          ? _value.attachment
          : attachment // ignore: cast_nullable_to_non_nullable
              as String?,
      isActive: null == isActive
          ? _value.isActive
          : isActive // ignore: cast_nullable_to_non_nullable
              as bool,
      vatNumber: freezed == vatNumber
          ? _value.vatNumber
          : vatNumber // ignore: cast_nullable_to_non_nullable
              as String?,
      created: null == created
          ? _value.created
          : created // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updated: null == updated
          ? _value.updated
          : updated // ignore: cast_nullable_to_non_nullable
              as DateTime,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$DealModelImpl extends _DealModel {
  const _$DealModelImpl(
      {required this.id,
      required this.name,
      required this.commercialSector,
      @JsonKey(readValue: getDataFromExpanded) required this.position,
      required this.isLocal,
      this.details,
      this.who,
      this.starting,
      this.ending,
      this.howToGet,
      this.link,
      this.owner,
      this.attachment,
      required this.isActive,
      this.vatNumber,
      required this.created,
      required this.updated})
      : super._();

  factory _$DealModelImpl.fromJson(Map<String, dynamic> json) =>
      _$$DealModelImplFromJson(json);

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
  final DateTime? starting;
  @override
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
  final DateTime created;
  @override
  final DateTime updated;

  @override
  String toString() {
    return 'DealModel(id: $id, name: $name, commercialSector: $commercialSector, position: $position, isLocal: $isLocal, details: $details, who: $who, starting: $starting, ending: $ending, howToGet: $howToGet, link: $link, owner: $owner, attachment: $attachment, isActive: $isActive, vatNumber: $vatNumber, created: $created, updated: $updated)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$DealModelImpl &&
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

  @JsonKey(ignore: true)
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

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$DealModelImplCopyWith<_$DealModelImpl> get copyWith =>
      __$$DealModelImplCopyWithImpl<_$DealModelImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$DealModelImplToJson(
      this,
    );
  }
}

abstract class _DealModel extends DealModel {
  const factory _DealModel(
      {required final String id,
      required final String name,
      required final String commercialSector,
      @JsonKey(readValue: getDataFromExpanded)
      required final LocationModel? position,
      required final bool isLocal,
      final String? details,
      final String? who,
      final DateTime? starting,
      final DateTime? ending,
      final String? howToGet,
      final String? link,
      final String? owner,
      final String? attachment,
      required final bool isActive,
      final String? vatNumber,
      required final DateTime created,
      required final DateTime updated}) = _$DealModelImpl;
  const _DealModel._() : super._();

  factory _DealModel.fromJson(Map<String, dynamic> json) =
      _$DealModelImpl.fromJson;

  @override
  String get id;
  @override
  String get name;
  @override
  String get commercialSector;
  @override
  @JsonKey(readValue: getDataFromExpanded)
  LocationModel? get position;
  @override
  bool get isLocal;
  @override
  String? get details;
  @override
  String? get who;
  @override
  DateTime? get starting;
  @override
  DateTime? get ending;
  @override
  String? get howToGet;
  @override
  String? get link;
  @override
  String? get owner;
  @override
  String? get attachment;
  @override
  bool get isActive;
  @override
  String? get vatNumber;
  @override
  DateTime get created;
  @override
  DateTime get updated;
  @override
  @JsonKey(ignore: true)
  _$$DealModelImplCopyWith<_$DealModelImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
