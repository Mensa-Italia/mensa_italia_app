// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'area_document.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

AreaDocumentModel _$AreaDocumentModelFromJson(Map<String, dynamic> json) {
  return _AreaDocumentModel.fromJson(json);
}

/// @nodoc
mixin _$AreaDocumentModel {
  String get description => throw _privateConstructorUsedError;
  String get image => throw _privateConstructorUsedError;
  String get dimension => throw _privateConstructorUsedError;
  String get link => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $AreaDocumentModelCopyWith<AreaDocumentModel> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $AreaDocumentModelCopyWith<$Res> {
  factory $AreaDocumentModelCopyWith(
          AreaDocumentModel value, $Res Function(AreaDocumentModel) then) =
      _$AreaDocumentModelCopyWithImpl<$Res, AreaDocumentModel>;
  @useResult
  $Res call({String description, String image, String dimension, String link});
}

/// @nodoc
class _$AreaDocumentModelCopyWithImpl<$Res, $Val extends AreaDocumentModel>
    implements $AreaDocumentModelCopyWith<$Res> {
  _$AreaDocumentModelCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? description = null,
    Object? image = null,
    Object? dimension = null,
    Object? link = null,
  }) {
    return _then(_value.copyWith(
      description: null == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      image: null == image
          ? _value.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
      dimension: null == dimension
          ? _value.dimension
          : dimension // ignore: cast_nullable_to_non_nullable
              as String,
      link: null == link
          ? _value.link
          : link // ignore: cast_nullable_to_non_nullable
              as String,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$AreaDocumentModelImplCopyWith<$Res>
    implements $AreaDocumentModelCopyWith<$Res> {
  factory _$$AreaDocumentModelImplCopyWith(_$AreaDocumentModelImpl value,
          $Res Function(_$AreaDocumentModelImpl) then) =
      __$$AreaDocumentModelImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({String description, String image, String dimension, String link});
}

/// @nodoc
class __$$AreaDocumentModelImplCopyWithImpl<$Res>
    extends _$AreaDocumentModelCopyWithImpl<$Res, _$AreaDocumentModelImpl>
    implements _$$AreaDocumentModelImplCopyWith<$Res> {
  __$$AreaDocumentModelImplCopyWithImpl(_$AreaDocumentModelImpl _value,
      $Res Function(_$AreaDocumentModelImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? description = null,
    Object? image = null,
    Object? dimension = null,
    Object? link = null,
  }) {
    return _then(_$AreaDocumentModelImpl(
      description: null == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      image: null == image
          ? _value.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
      dimension: null == dimension
          ? _value.dimension
          : dimension // ignore: cast_nullable_to_non_nullable
              as String,
      link: null == link
          ? _value.link
          : link // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$AreaDocumentModelImpl implements _AreaDocumentModel {
  _$AreaDocumentModelImpl(
      {required this.description,
      required this.image,
      required this.dimension,
      required this.link});

  factory _$AreaDocumentModelImpl.fromJson(Map<String, dynamic> json) =>
      _$$AreaDocumentModelImplFromJson(json);

  @override
  final String description;
  @override
  final String image;
  @override
  final String dimension;
  @override
  final String link;

  @override
  String toString() {
    return 'AreaDocumentModel(description: $description, image: $image, dimension: $dimension, link: $link)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$AreaDocumentModelImpl &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.image, image) || other.image == image) &&
            (identical(other.dimension, dimension) ||
                other.dimension == dimension) &&
            (identical(other.link, link) || other.link == link));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode =>
      Object.hash(runtimeType, description, image, dimension, link);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$AreaDocumentModelImplCopyWith<_$AreaDocumentModelImpl> get copyWith =>
      __$$AreaDocumentModelImplCopyWithImpl<_$AreaDocumentModelImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$AreaDocumentModelImplToJson(
      this,
    );
  }
}

abstract class _AreaDocumentModel implements AreaDocumentModel {
  factory _AreaDocumentModel(
      {required final String description,
      required final String image,
      required final String dimension,
      required final String link}) = _$AreaDocumentModelImpl;

  factory _AreaDocumentModel.fromJson(Map<String, dynamic> json) =
      _$AreaDocumentModelImpl.fromJson;

  @override
  String get description;
  @override
  String get image;
  @override
  String get dimension;
  @override
  String get link;
  @override
  @JsonKey(ignore: true)
  _$$AreaDocumentModelImplCopyWith<_$AreaDocumentModelImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
