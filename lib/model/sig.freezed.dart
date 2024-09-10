// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'sig.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

SigModel _$SigModelFromJson(Map<String, dynamic> json) {
  return _SigModel.fromJson(json);
}

/// @nodoc
mixin _$SigModel {
  String get id => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String get description => throw _privateConstructorUsedError;
  String get image => throw _privateConstructorUsedError;
  String get link => throw _privateConstructorUsedError;
  String get groupType => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $SigModelCopyWith<SigModel> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $SigModelCopyWith<$Res> {
  factory $SigModelCopyWith(SigModel value, $Res Function(SigModel) then) =
      _$SigModelCopyWithImpl<$Res, SigModel>;
  @useResult
  $Res call(
      {String id,
      String name,
      String description,
      String image,
      String link,
      String groupType});
}

/// @nodoc
class _$SigModelCopyWithImpl<$Res, $Val extends SigModel>
    implements $SigModelCopyWith<$Res> {
  _$SigModelCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? description = null,
    Object? image = null,
    Object? link = null,
    Object? groupType = null,
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
      description: null == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      image: null == image
          ? _value.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
      link: null == link
          ? _value.link
          : link // ignore: cast_nullable_to_non_nullable
              as String,
      groupType: null == groupType
          ? _value.groupType
          : groupType // ignore: cast_nullable_to_non_nullable
              as String,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$SigModelImplCopyWith<$Res>
    implements $SigModelCopyWith<$Res> {
  factory _$$SigModelImplCopyWith(
          _$SigModelImpl value, $Res Function(_$SigModelImpl) then) =
      __$$SigModelImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      String name,
      String description,
      String image,
      String link,
      String groupType});
}

/// @nodoc
class __$$SigModelImplCopyWithImpl<$Res>
    extends _$SigModelCopyWithImpl<$Res, _$SigModelImpl>
    implements _$$SigModelImplCopyWith<$Res> {
  __$$SigModelImplCopyWithImpl(
      _$SigModelImpl _value, $Res Function(_$SigModelImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? description = null,
    Object? image = null,
    Object? link = null,
    Object? groupType = null,
  }) {
    return _then(_$SigModelImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      description: null == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      image: null == image
          ? _value.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
      link: null == link
          ? _value.link
          : link // ignore: cast_nullable_to_non_nullable
              as String,
      groupType: null == groupType
          ? _value.groupType
          : groupType // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$SigModelImpl implements _SigModel {
  const _$SigModelImpl(
      {required this.id,
      required this.name,
      required this.description,
      required this.image,
      required this.link,
      required this.groupType});

  factory _$SigModelImpl.fromJson(Map<String, dynamic> json) =>
      _$$SigModelImplFromJson(json);

  @override
  final String id;
  @override
  final String name;
  @override
  final String description;
  @override
  final String image;
  @override
  final String link;
  @override
  final String groupType;

  @override
  String toString() {
    return 'SigModel(id: $id, name: $name, description: $description, image: $image, link: $link, groupType: $groupType)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$SigModelImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.image, image) || other.image == image) &&
            (identical(other.link, link) || other.link == link) &&
            (identical(other.groupType, groupType) ||
                other.groupType == groupType));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode =>
      Object.hash(runtimeType, id, name, description, image, link, groupType);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$SigModelImplCopyWith<_$SigModelImpl> get copyWith =>
      __$$SigModelImplCopyWithImpl<_$SigModelImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$SigModelImplToJson(
      this,
    );
  }
}

abstract class _SigModel implements SigModel {
  const factory _SigModel(
      {required final String id,
      required final String name,
      required final String description,
      required final String image,
      required final String link,
      required final String groupType}) = _$SigModelImpl;

  factory _SigModel.fromJson(Map<String, dynamic> json) =
      _$SigModelImpl.fromJson;

  @override
  String get id;
  @override
  String get name;
  @override
  String get description;
  @override
  String get image;
  @override
  String get link;
  @override
  String get groupType;
  @override
  @JsonKey(ignore: true)
  _$$SigModelImplCopyWith<_$SigModelImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
