// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'res_soci.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

RegSociModel _$RegSociModelFromJson(Map<String, dynamic> json) {
  return _RegSociModel.fromJson(json);
}

/// @nodoc
mixin _$RegSociModel {
  String get id => throw _privateConstructorUsedError;
  String get image => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String get city => throw _privateConstructorUsedError;
  @JsonKey(fromJson: getDateTimeLocalNullabe)
  DateTime? get birthdate => throw _privateConstructorUsedError;
  String get state => throw _privateConstructorUsedError;
  Map<String, dynamic> get fullData => throw _privateConstructorUsedError;
  String? get fullProfileLink => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $RegSociModelCopyWith<RegSociModel> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $RegSociModelCopyWith<$Res> {
  factory $RegSociModelCopyWith(
          RegSociModel value, $Res Function(RegSociModel) then) =
      _$RegSociModelCopyWithImpl<$Res, RegSociModel>;
  @useResult
  $Res call(
      {String id,
      String image,
      String name,
      String city,
      @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? birthdate,
      String state,
      Map<String, dynamic> fullData,
      String? fullProfileLink});
}

/// @nodoc
class _$RegSociModelCopyWithImpl<$Res, $Val extends RegSociModel>
    implements $RegSociModelCopyWith<$Res> {
  _$RegSociModelCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? image = null,
    Object? name = null,
    Object? city = null,
    Object? birthdate = freezed,
    Object? state = null,
    Object? fullData = null,
    Object? fullProfileLink = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      image: null == image
          ? _value.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      city: null == city
          ? _value.city
          : city // ignore: cast_nullable_to_non_nullable
              as String,
      birthdate: freezed == birthdate
          ? _value.birthdate
          : birthdate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      state: null == state
          ? _value.state
          : state // ignore: cast_nullable_to_non_nullable
              as String,
      fullData: null == fullData
          ? _value.fullData
          : fullData // ignore: cast_nullable_to_non_nullable
              as Map<String, dynamic>,
      fullProfileLink: freezed == fullProfileLink
          ? _value.fullProfileLink
          : fullProfileLink // ignore: cast_nullable_to_non_nullable
              as String?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$RegSociModelImplCopyWith<$Res>
    implements $RegSociModelCopyWith<$Res> {
  factory _$$RegSociModelImplCopyWith(
          _$RegSociModelImpl value, $Res Function(_$RegSociModelImpl) then) =
      __$$RegSociModelImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      String image,
      String name,
      String city,
      @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? birthdate,
      String state,
      Map<String, dynamic> fullData,
      String? fullProfileLink});
}

/// @nodoc
class __$$RegSociModelImplCopyWithImpl<$Res>
    extends _$RegSociModelCopyWithImpl<$Res, _$RegSociModelImpl>
    implements _$$RegSociModelImplCopyWith<$Res> {
  __$$RegSociModelImplCopyWithImpl(
      _$RegSociModelImpl _value, $Res Function(_$RegSociModelImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? image = null,
    Object? name = null,
    Object? city = null,
    Object? birthdate = freezed,
    Object? state = null,
    Object? fullData = null,
    Object? fullProfileLink = freezed,
  }) {
    return _then(_$RegSociModelImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      image: null == image
          ? _value.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      city: null == city
          ? _value.city
          : city // ignore: cast_nullable_to_non_nullable
              as String,
      birthdate: freezed == birthdate
          ? _value.birthdate
          : birthdate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      state: null == state
          ? _value.state
          : state // ignore: cast_nullable_to_non_nullable
              as String,
      fullData: null == fullData
          ? _value._fullData
          : fullData // ignore: cast_nullable_to_non_nullable
              as Map<String, dynamic>,
      fullProfileLink: freezed == fullProfileLink
          ? _value.fullProfileLink
          : fullProfileLink // ignore: cast_nullable_to_non_nullable
              as String?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$RegSociModelImpl extends _RegSociModel {
  const _$RegSociModelImpl(
      {required this.id,
      required this.image,
      required this.name,
      required this.city,
      @JsonKey(fromJson: getDateTimeLocalNullabe) required this.birthdate,
      required this.state,
      required final Map<String, dynamic> fullData,
      required this.fullProfileLink})
      : _fullData = fullData,
        super._();

  factory _$RegSociModelImpl.fromJson(Map<String, dynamic> json) =>
      _$$RegSociModelImplFromJson(json);

  @override
  final String id;
  @override
  final String image;
  @override
  final String name;
  @override
  final String city;
  @override
  @JsonKey(fromJson: getDateTimeLocalNullabe)
  final DateTime? birthdate;
  @override
  final String state;
  final Map<String, dynamic> _fullData;
  @override
  Map<String, dynamic> get fullData {
    if (_fullData is EqualUnmodifiableMapView) return _fullData;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableMapView(_fullData);
  }

  @override
  final String? fullProfileLink;

  @override
  String toString() {
    return 'RegSociModel(id: $id, image: $image, name: $name, city: $city, birthdate: $birthdate, state: $state, fullData: $fullData, fullProfileLink: $fullProfileLink)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$RegSociModelImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.image, image) || other.image == image) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.city, city) || other.city == city) &&
            (identical(other.birthdate, birthdate) ||
                other.birthdate == birthdate) &&
            (identical(other.state, state) || other.state == state) &&
            const DeepCollectionEquality().equals(other._fullData, _fullData) &&
            (identical(other.fullProfileLink, fullProfileLink) ||
                other.fullProfileLink == fullProfileLink));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode => Object.hash(runtimeType, id, image, name, city, birthdate,
      state, const DeepCollectionEquality().hash(_fullData), fullProfileLink);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$RegSociModelImplCopyWith<_$RegSociModelImpl> get copyWith =>
      __$$RegSociModelImplCopyWithImpl<_$RegSociModelImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$RegSociModelImplToJson(
      this,
    );
  }
}

abstract class _RegSociModel extends RegSociModel {
  const factory _RegSociModel(
      {required final String id,
      required final String image,
      required final String name,
      required final String city,
      @JsonKey(fromJson: getDateTimeLocalNullabe)
      required final DateTime? birthdate,
      required final String state,
      required final Map<String, dynamic> fullData,
      required final String? fullProfileLink}) = _$RegSociModelImpl;
  const _RegSociModel._() : super._();

  factory _RegSociModel.fromJson(Map<String, dynamic> json) =
      _$RegSociModelImpl.fromJson;

  @override
  String get id;
  @override
  String get image;
  @override
  String get name;
  @override
  String get city;
  @override
  @JsonKey(fromJson: getDateTimeLocalNullabe)
  DateTime? get birthdate;
  @override
  String get state;
  @override
  Map<String, dynamic> get fullData;
  @override
  String? get fullProfileLink;
  @override
  @JsonKey(ignore: true)
  _$$RegSociModelImplCopyWith<_$RegSociModelImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

RegSociDBModel _$RegSociDBModelFromJson(Map<String, dynamic> json) {
  return _RegSociDBModel.fromJson(json);
}

/// @nodoc
mixin _$RegSociDBModel {
  int get uid => throw _privateConstructorUsedError;
  String get image => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String get city => throw _privateConstructorUsedError;
  @JsonKey(fromJson: getDateTimeLocalNullabe)
  DateTime? get birthdate => throw _privateConstructorUsedError;
  String get state => throw _privateConstructorUsedError;
  String get fullDataJson => throw _privateConstructorUsedError;
  String? get fullProfileLink => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $RegSociDBModelCopyWith<RegSociDBModel> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $RegSociDBModelCopyWith<$Res> {
  factory $RegSociDBModelCopyWith(
          RegSociDBModel value, $Res Function(RegSociDBModel) then) =
      _$RegSociDBModelCopyWithImpl<$Res, RegSociDBModel>;
  @useResult
  $Res call(
      {int uid,
      String image,
      String name,
      String city,
      @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? birthdate,
      String state,
      String fullDataJson,
      String? fullProfileLink});
}

/// @nodoc
class _$RegSociDBModelCopyWithImpl<$Res, $Val extends RegSociDBModel>
    implements $RegSociDBModelCopyWith<$Res> {
  _$RegSociDBModelCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? uid = null,
    Object? image = null,
    Object? name = null,
    Object? city = null,
    Object? birthdate = freezed,
    Object? state = null,
    Object? fullDataJson = null,
    Object? fullProfileLink = freezed,
  }) {
    return _then(_value.copyWith(
      uid: null == uid
          ? _value.uid
          : uid // ignore: cast_nullable_to_non_nullable
              as int,
      image: null == image
          ? _value.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      city: null == city
          ? _value.city
          : city // ignore: cast_nullable_to_non_nullable
              as String,
      birthdate: freezed == birthdate
          ? _value.birthdate
          : birthdate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      state: null == state
          ? _value.state
          : state // ignore: cast_nullable_to_non_nullable
              as String,
      fullDataJson: null == fullDataJson
          ? _value.fullDataJson
          : fullDataJson // ignore: cast_nullable_to_non_nullable
              as String,
      fullProfileLink: freezed == fullProfileLink
          ? _value.fullProfileLink
          : fullProfileLink // ignore: cast_nullable_to_non_nullable
              as String?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$RegSociDBModelImplCopyWith<$Res>
    implements $RegSociDBModelCopyWith<$Res> {
  factory _$$RegSociDBModelImplCopyWith(_$RegSociDBModelImpl value,
          $Res Function(_$RegSociDBModelImpl) then) =
      __$$RegSociDBModelImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int uid,
      String image,
      String name,
      String city,
      @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? birthdate,
      String state,
      String fullDataJson,
      String? fullProfileLink});
}

/// @nodoc
class __$$RegSociDBModelImplCopyWithImpl<$Res>
    extends _$RegSociDBModelCopyWithImpl<$Res, _$RegSociDBModelImpl>
    implements _$$RegSociDBModelImplCopyWith<$Res> {
  __$$RegSociDBModelImplCopyWithImpl(
      _$RegSociDBModelImpl _value, $Res Function(_$RegSociDBModelImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? uid = null,
    Object? image = null,
    Object? name = null,
    Object? city = null,
    Object? birthdate = freezed,
    Object? state = null,
    Object? fullDataJson = null,
    Object? fullProfileLink = freezed,
  }) {
    return _then(_$RegSociDBModelImpl(
      uid: null == uid
          ? _value.uid
          : uid // ignore: cast_nullable_to_non_nullable
              as int,
      image: null == image
          ? _value.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      city: null == city
          ? _value.city
          : city // ignore: cast_nullable_to_non_nullable
              as String,
      birthdate: freezed == birthdate
          ? _value.birthdate
          : birthdate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      state: null == state
          ? _value.state
          : state // ignore: cast_nullable_to_non_nullable
              as String,
      fullDataJson: null == fullDataJson
          ? _value.fullDataJson
          : fullDataJson // ignore: cast_nullable_to_non_nullable
              as String,
      fullProfileLink: freezed == fullProfileLink
          ? _value.fullProfileLink
          : fullProfileLink // ignore: cast_nullable_to_non_nullable
              as String?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$RegSociDBModelImpl extends _RegSociDBModel {
  const _$RegSociDBModelImpl(
      {required this.uid,
      required this.image,
      required this.name,
      required this.city,
      @JsonKey(fromJson: getDateTimeLocalNullabe) required this.birthdate,
      required this.state,
      required this.fullDataJson,
      required this.fullProfileLink})
      : super._();

  factory _$RegSociDBModelImpl.fromJson(Map<String, dynamic> json) =>
      _$$RegSociDBModelImplFromJson(json);

  @override
  final int uid;
  @override
  final String image;
  @override
  final String name;
  @override
  final String city;
  @override
  @JsonKey(fromJson: getDateTimeLocalNullabe)
  final DateTime? birthdate;
  @override
  final String state;
  @override
  final String fullDataJson;
  @override
  final String? fullProfileLink;

  @override
  String toString() {
    return 'RegSociDBModel(uid: $uid, image: $image, name: $name, city: $city, birthdate: $birthdate, state: $state, fullDataJson: $fullDataJson, fullProfileLink: $fullProfileLink)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$RegSociDBModelImpl &&
            (identical(other.uid, uid) || other.uid == uid) &&
            (identical(other.image, image) || other.image == image) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.city, city) || other.city == city) &&
            (identical(other.birthdate, birthdate) ||
                other.birthdate == birthdate) &&
            (identical(other.state, state) || other.state == state) &&
            (identical(other.fullDataJson, fullDataJson) ||
                other.fullDataJson == fullDataJson) &&
            (identical(other.fullProfileLink, fullProfileLink) ||
                other.fullProfileLink == fullProfileLink));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode => Object.hash(runtimeType, uid, image, name, city,
      birthdate, state, fullDataJson, fullProfileLink);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$RegSociDBModelImplCopyWith<_$RegSociDBModelImpl> get copyWith =>
      __$$RegSociDBModelImplCopyWithImpl<_$RegSociDBModelImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$RegSociDBModelImplToJson(
      this,
    );
  }
}

abstract class _RegSociDBModel extends RegSociDBModel {
  const factory _RegSociDBModel(
      {required final int uid,
      required final String image,
      required final String name,
      required final String city,
      @JsonKey(fromJson: getDateTimeLocalNullabe)
      required final DateTime? birthdate,
      required final String state,
      required final String fullDataJson,
      required final String? fullProfileLink}) = _$RegSociDBModelImpl;
  const _RegSociDBModel._() : super._();

  factory _RegSociDBModel.fromJson(Map<String, dynamic> json) =
      _$RegSociDBModelImpl.fromJson;

  @override
  int get uid;
  @override
  String get image;
  @override
  String get name;
  @override
  String get city;
  @override
  @JsonKey(fromJson: getDateTimeLocalNullabe)
  DateTime? get birthdate;
  @override
  String get state;
  @override
  String get fullDataJson;
  @override
  String? get fullProfileLink;
  @override
  @JsonKey(ignore: true)
  _$$RegSociDBModelImplCopyWith<_$RegSociDBModelImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
