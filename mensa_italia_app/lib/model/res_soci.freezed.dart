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
  String get state => throw _privateConstructorUsedError;
  String get linkToFullProfile => throw _privateConstructorUsedError;

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
      String state,
      String linkToFullProfile});
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
    Object? state = null,
    Object? linkToFullProfile = null,
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
      state: null == state
          ? _value.state
          : state // ignore: cast_nullable_to_non_nullable
              as String,
      linkToFullProfile: null == linkToFullProfile
          ? _value.linkToFullProfile
          : linkToFullProfile // ignore: cast_nullable_to_non_nullable
              as String,
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
      String state,
      String linkToFullProfile});
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
    Object? state = null,
    Object? linkToFullProfile = null,
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
      state: null == state
          ? _value.state
          : state // ignore: cast_nullable_to_non_nullable
              as String,
      linkToFullProfile: null == linkToFullProfile
          ? _value.linkToFullProfile
          : linkToFullProfile // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$RegSociModelImpl implements _RegSociModel {
  const _$RegSociModelImpl(
      {required this.id,
      required this.image,
      required this.name,
      required this.city,
      required this.state,
      required this.linkToFullProfile});

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
  final String state;
  @override
  final String linkToFullProfile;

  @override
  String toString() {
    return 'RegSociModel(id: $id, image: $image, name: $name, city: $city, state: $state, linkToFullProfile: $linkToFullProfile)';
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
            (identical(other.state, state) || other.state == state) &&
            (identical(other.linkToFullProfile, linkToFullProfile) ||
                other.linkToFullProfile == linkToFullProfile));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode =>
      Object.hash(runtimeType, id, image, name, city, state, linkToFullProfile);

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

abstract class _RegSociModel implements RegSociModel {
  const factory _RegSociModel(
      {required final String id,
      required final String image,
      required final String name,
      required final String city,
      required final String state,
      required final String linkToFullProfile}) = _$RegSociModelImpl;

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
  String get state;
  @override
  String get linkToFullProfile;
  @override
  @JsonKey(ignore: true)
  _$$RegSociModelImplCopyWith<_$RegSociModelImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
