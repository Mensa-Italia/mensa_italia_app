// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'boutique.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

BoutiqueModel _$BoutiqueModelFromJson(Map<String, dynamic> json) {
  return _BoutiqueModel.fromJson(json);
}

/// @nodoc
mixin _$BoutiqueModel {
  String get id => throw _privateConstructorUsedError;
  String get uid => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String get description => throw _privateConstructorUsedError;
  List<String> get image => throw _privateConstructorUsedError;
  int get amount => throw _privateConstructorUsedError;
  String get alternativeOf => throw _privateConstructorUsedError;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get created => throw _privateConstructorUsedError;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get updated => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $BoutiqueModelCopyWith<BoutiqueModel> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $BoutiqueModelCopyWith<$Res> {
  factory $BoutiqueModelCopyWith(
          BoutiqueModel value, $Res Function(BoutiqueModel) then) =
      _$BoutiqueModelCopyWithImpl<$Res, BoutiqueModel>;
  @useResult
  $Res call(
      {String id,
      String uid,
      String name,
      String description,
      List<String> image,
      int amount,
      String alternativeOf,
      @JsonKey(fromJson: getDateTimeLocal) DateTime created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime updated});
}

/// @nodoc
class _$BoutiqueModelCopyWithImpl<$Res, $Val extends BoutiqueModel>
    implements $BoutiqueModelCopyWith<$Res> {
  _$BoutiqueModelCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? uid = null,
    Object? name = null,
    Object? description = null,
    Object? image = null,
    Object? amount = null,
    Object? alternativeOf = null,
    Object? created = null,
    Object? updated = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      uid: null == uid
          ? _value.uid
          : uid // ignore: cast_nullable_to_non_nullable
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
              as List<String>,
      amount: null == amount
          ? _value.amount
          : amount // ignore: cast_nullable_to_non_nullable
              as int,
      alternativeOf: null == alternativeOf
          ? _value.alternativeOf
          : alternativeOf // ignore: cast_nullable_to_non_nullable
              as String,
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
}

/// @nodoc
abstract class _$$BoutiqueModelImplCopyWith<$Res>
    implements $BoutiqueModelCopyWith<$Res> {
  factory _$$BoutiqueModelImplCopyWith(
          _$BoutiqueModelImpl value, $Res Function(_$BoutiqueModelImpl) then) =
      __$$BoutiqueModelImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      String uid,
      String name,
      String description,
      List<String> image,
      int amount,
      String alternativeOf,
      @JsonKey(fromJson: getDateTimeLocal) DateTime created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime updated});
}

/// @nodoc
class __$$BoutiqueModelImplCopyWithImpl<$Res>
    extends _$BoutiqueModelCopyWithImpl<$Res, _$BoutiqueModelImpl>
    implements _$$BoutiqueModelImplCopyWith<$Res> {
  __$$BoutiqueModelImplCopyWithImpl(
      _$BoutiqueModelImpl _value, $Res Function(_$BoutiqueModelImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? uid = null,
    Object? name = null,
    Object? description = null,
    Object? image = null,
    Object? amount = null,
    Object? alternativeOf = null,
    Object? created = null,
    Object? updated = null,
  }) {
    return _then(_$BoutiqueModelImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      uid: null == uid
          ? _value.uid
          : uid // ignore: cast_nullable_to_non_nullable
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
          ? _value._image
          : image // ignore: cast_nullable_to_non_nullable
              as List<String>,
      amount: null == amount
          ? _value.amount
          : amount // ignore: cast_nullable_to_non_nullable
              as int,
      alternativeOf: null == alternativeOf
          ? _value.alternativeOf
          : alternativeOf // ignore: cast_nullable_to_non_nullable
              as String,
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
class _$BoutiqueModelImpl extends _BoutiqueModel {
  _$BoutiqueModelImpl(
      {required this.id,
      required this.uid,
      required this.name,
      required this.description,
      required final List<String> image,
      required this.amount,
      required this.alternativeOf,
      @JsonKey(fromJson: getDateTimeLocal) required this.created,
      @JsonKey(fromJson: getDateTimeLocal) required this.updated})
      : _image = image,
        super._();

  factory _$BoutiqueModelImpl.fromJson(Map<String, dynamic> json) =>
      _$$BoutiqueModelImplFromJson(json);

  @override
  final String id;
  @override
  final String uid;
  @override
  final String name;
  @override
  final String description;
  final List<String> _image;
  @override
  List<String> get image {
    if (_image is EqualUnmodifiableListView) return _image;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_image);
  }

  @override
  final int amount;
  @override
  final String alternativeOf;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime created;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime updated;

  @override
  String toString() {
    return 'BoutiqueModel(id: $id, uid: $uid, name: $name, description: $description, image: $image, amount: $amount, alternativeOf: $alternativeOf, created: $created, updated: $updated)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$BoutiqueModelImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.uid, uid) || other.uid == uid) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            const DeepCollectionEquality().equals(other._image, _image) &&
            (identical(other.amount, amount) || other.amount == amount) &&
            (identical(other.alternativeOf, alternativeOf) ||
                other.alternativeOf == alternativeOf) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      uid,
      name,
      description,
      const DeepCollectionEquality().hash(_image),
      amount,
      alternativeOf,
      created,
      updated);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$BoutiqueModelImplCopyWith<_$BoutiqueModelImpl> get copyWith =>
      __$$BoutiqueModelImplCopyWithImpl<_$BoutiqueModelImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$BoutiqueModelImplToJson(
      this,
    );
  }
}

abstract class _BoutiqueModel extends BoutiqueModel {
  factory _BoutiqueModel(
      {required final String id,
      required final String uid,
      required final String name,
      required final String description,
      required final List<String> image,
      required final int amount,
      required final String alternativeOf,
      @JsonKey(fromJson: getDateTimeLocal) required final DateTime created,
      @JsonKey(fromJson: getDateTimeLocal)
      required final DateTime updated}) = _$BoutiqueModelImpl;
  _BoutiqueModel._() : super._();

  factory _BoutiqueModel.fromJson(Map<String, dynamic> json) =
      _$BoutiqueModelImpl.fromJson;

  @override
  String get id;
  @override
  String get uid;
  @override
  String get name;
  @override
  String get description;
  @override
  List<String> get image;
  @override
  int get amount;
  @override
  String get alternativeOf;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get created;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get updated;
  @override
  @JsonKey(ignore: true)
  _$$BoutiqueModelImplCopyWith<_$BoutiqueModelImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
