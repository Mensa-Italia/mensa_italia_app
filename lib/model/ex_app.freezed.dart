// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'ex_app.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

ExAppModel _$ExAppModelFromJson(Map<String, dynamic> json) {
  return _ExAppModel.fromJson(json);
}

/// @nodoc
mixin _$ExAppModel {
  String? get collectionId => throw _privateConstructorUsedError;
  String? get collectionName => throw _privateConstructorUsedError;
  String? get id => throw _privateConstructorUsedError;
  String? get name => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  String? get image => throw _privateConstructorUsedError;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime? get created => throw _privateConstructorUsedError;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime? get updated => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $ExAppModelCopyWith<ExAppModel> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $ExAppModelCopyWith<$Res> {
  factory $ExAppModelCopyWith(
          ExAppModel value, $Res Function(ExAppModel) then) =
      _$ExAppModelCopyWithImpl<$Res, ExAppModel>;
  @useResult
  $Res call(
      {String? collectionId,
      String? collectionName,
      String? id,
      String? name,
      String? description,
      String? image,
      @JsonKey(fromJson: getDateTimeLocal) DateTime? created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime? updated});
}

/// @nodoc
class _$ExAppModelCopyWithImpl<$Res, $Val extends ExAppModel>
    implements $ExAppModelCopyWith<$Res> {
  _$ExAppModelCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? collectionId = freezed,
    Object? collectionName = freezed,
    Object? id = freezed,
    Object? name = freezed,
    Object? description = freezed,
    Object? image = freezed,
    Object? created = freezed,
    Object? updated = freezed,
  }) {
    return _then(_value.copyWith(
      collectionId: freezed == collectionId
          ? _value.collectionId
          : collectionId // ignore: cast_nullable_to_non_nullable
              as String?,
      collectionName: freezed == collectionName
          ? _value.collectionName
          : collectionName // ignore: cast_nullable_to_non_nullable
              as String?,
      id: freezed == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String?,
      name: freezed == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String?,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      image: freezed == image
          ? _value.image
          : image // ignore: cast_nullable_to_non_nullable
              as String?,
      created: freezed == created
          ? _value.created
          : created // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      updated: freezed == updated
          ? _value.updated
          : updated // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$ExAppModelImplCopyWith<$Res>
    implements $ExAppModelCopyWith<$Res> {
  factory _$$ExAppModelImplCopyWith(
          _$ExAppModelImpl value, $Res Function(_$ExAppModelImpl) then) =
      __$$ExAppModelImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String? collectionId,
      String? collectionName,
      String? id,
      String? name,
      String? description,
      String? image,
      @JsonKey(fromJson: getDateTimeLocal) DateTime? created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime? updated});
}

/// @nodoc
class __$$ExAppModelImplCopyWithImpl<$Res>
    extends _$ExAppModelCopyWithImpl<$Res, _$ExAppModelImpl>
    implements _$$ExAppModelImplCopyWith<$Res> {
  __$$ExAppModelImplCopyWithImpl(
      _$ExAppModelImpl _value, $Res Function(_$ExAppModelImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? collectionId = freezed,
    Object? collectionName = freezed,
    Object? id = freezed,
    Object? name = freezed,
    Object? description = freezed,
    Object? image = freezed,
    Object? created = freezed,
    Object? updated = freezed,
  }) {
    return _then(_$ExAppModelImpl(
      collectionId: freezed == collectionId
          ? _value.collectionId
          : collectionId // ignore: cast_nullable_to_non_nullable
              as String?,
      collectionName: freezed == collectionName
          ? _value.collectionName
          : collectionName // ignore: cast_nullable_to_non_nullable
              as String?,
      id: freezed == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String?,
      name: freezed == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String?,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      image: freezed == image
          ? _value.image
          : image // ignore: cast_nullable_to_non_nullable
              as String?,
      created: freezed == created
          ? _value.created
          : created // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      updated: freezed == updated
          ? _value.updated
          : updated // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$ExAppModelImpl implements _ExAppModel {
  _$ExAppModelImpl(
      {this.collectionId,
      this.collectionName,
      this.id,
      this.name,
      this.description,
      this.image,
      @JsonKey(fromJson: getDateTimeLocal) this.created,
      @JsonKey(fromJson: getDateTimeLocal) this.updated});

  factory _$ExAppModelImpl.fromJson(Map<String, dynamic> json) =>
      _$$ExAppModelImplFromJson(json);

  @override
  final String? collectionId;
  @override
  final String? collectionName;
  @override
  final String? id;
  @override
  final String? name;
  @override
  final String? description;
  @override
  final String? image;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime? created;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime? updated;

  @override
  String toString() {
    return 'ExAppModel(collectionId: $collectionId, collectionName: $collectionName, id: $id, name: $name, description: $description, image: $image, created: $created, updated: $updated)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$ExAppModelImpl &&
            (identical(other.collectionId, collectionId) ||
                other.collectionId == collectionId) &&
            (identical(other.collectionName, collectionName) ||
                other.collectionName == collectionName) &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.image, image) || other.image == image) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode => Object.hash(runtimeType, collectionId, collectionName, id,
      name, description, image, created, updated);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$ExAppModelImplCopyWith<_$ExAppModelImpl> get copyWith =>
      __$$ExAppModelImplCopyWithImpl<_$ExAppModelImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$ExAppModelImplToJson(
      this,
    );
  }
}

abstract class _ExAppModel implements ExAppModel {
  factory _ExAppModel(
          {final String? collectionId,
          final String? collectionName,
          final String? id,
          final String? name,
          final String? description,
          final String? image,
          @JsonKey(fromJson: getDateTimeLocal) final DateTime? created,
          @JsonKey(fromJson: getDateTimeLocal) final DateTime? updated}) =
      _$ExAppModelImpl;

  factory _ExAppModel.fromJson(Map<String, dynamic> json) =
      _$ExAppModelImpl.fromJson;

  @override
  String? get collectionId;
  @override
  String? get collectionName;
  @override
  String? get id;
  @override
  String? get name;
  @override
  String? get description;
  @override
  String? get image;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime? get created;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime? get updated;
  @override
  @JsonKey(ignore: true)
  _$$ExAppModelImplCopyWith<_$ExAppModelImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
