// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'testelab.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

TestelabModel _$TestelabModelFromJson(Map<String, dynamic> json) {
  return _TestelabModel.fromJson(json);
}

/// @nodoc
mixin _$TestelabModel {
  String get id => throw _privateConstructorUsedError;
  String get fullname => throw _privateConstructorUsedError;
  String get typeOfTest => throw _privateConstructorUsedError;
  String get modality => throw _privateConstructorUsedError;
  String get status => throw _privateConstructorUsedError;
  String get state => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $TestelabModelCopyWith<TestelabModel> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $TestelabModelCopyWith<$Res> {
  factory $TestelabModelCopyWith(
          TestelabModel value, $Res Function(TestelabModel) then) =
      _$TestelabModelCopyWithImpl<$Res, TestelabModel>;
  @useResult
  $Res call(
      {String id,
      String fullname,
      String typeOfTest,
      String modality,
      String status,
      String state});
}

/// @nodoc
class _$TestelabModelCopyWithImpl<$Res, $Val extends TestelabModel>
    implements $TestelabModelCopyWith<$Res> {
  _$TestelabModelCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? fullname = null,
    Object? typeOfTest = null,
    Object? modality = null,
    Object? status = null,
    Object? state = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      fullname: null == fullname
          ? _value.fullname
          : fullname // ignore: cast_nullable_to_non_nullable
              as String,
      typeOfTest: null == typeOfTest
          ? _value.typeOfTest
          : typeOfTest // ignore: cast_nullable_to_non_nullable
              as String,
      modality: null == modality
          ? _value.modality
          : modality // ignore: cast_nullable_to_non_nullable
              as String,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as String,
      state: null == state
          ? _value.state
          : state // ignore: cast_nullable_to_non_nullable
              as String,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$TestelabModelImplCopyWith<$Res>
    implements $TestelabModelCopyWith<$Res> {
  factory _$$TestelabModelImplCopyWith(
          _$TestelabModelImpl value, $Res Function(_$TestelabModelImpl) then) =
      __$$TestelabModelImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      String fullname,
      String typeOfTest,
      String modality,
      String status,
      String state});
}

/// @nodoc
class __$$TestelabModelImplCopyWithImpl<$Res>
    extends _$TestelabModelCopyWithImpl<$Res, _$TestelabModelImpl>
    implements _$$TestelabModelImplCopyWith<$Res> {
  __$$TestelabModelImplCopyWithImpl(
      _$TestelabModelImpl _value, $Res Function(_$TestelabModelImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? fullname = null,
    Object? typeOfTest = null,
    Object? modality = null,
    Object? status = null,
    Object? state = null,
  }) {
    return _then(_$TestelabModelImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      fullname: null == fullname
          ? _value.fullname
          : fullname // ignore: cast_nullable_to_non_nullable
              as String,
      typeOfTest: null == typeOfTest
          ? _value.typeOfTest
          : typeOfTest // ignore: cast_nullable_to_non_nullable
              as String,
      modality: null == modality
          ? _value.modality
          : modality // ignore: cast_nullable_to_non_nullable
              as String,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as String,
      state: null == state
          ? _value.state
          : state // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$TestelabModelImpl extends _TestelabModel {
  const _$TestelabModelImpl(
      {required this.id,
      required this.fullname,
      required this.typeOfTest,
      required this.modality,
      required this.status,
      required this.state})
      : super._();

  factory _$TestelabModelImpl.fromJson(Map<String, dynamic> json) =>
      _$$TestelabModelImplFromJson(json);

  @override
  final String id;
  @override
  final String fullname;
  @override
  final String typeOfTest;
  @override
  final String modality;
  @override
  final String status;
  @override
  final String state;

  @override
  String toString() {
    return 'TestelabModel(id: $id, fullname: $fullname, typeOfTest: $typeOfTest, modality: $modality, status: $status, state: $state)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$TestelabModelImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.fullname, fullname) ||
                other.fullname == fullname) &&
            (identical(other.typeOfTest, typeOfTest) ||
                other.typeOfTest == typeOfTest) &&
            (identical(other.modality, modality) ||
                other.modality == modality) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.state, state) || other.state == state));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode => Object.hash(
      runtimeType, id, fullname, typeOfTest, modality, status, state);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$TestelabModelImplCopyWith<_$TestelabModelImpl> get copyWith =>
      __$$TestelabModelImplCopyWithImpl<_$TestelabModelImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$TestelabModelImplToJson(
      this,
    );
  }
}

abstract class _TestelabModel extends TestelabModel {
  const factory _TestelabModel(
      {required final String id,
      required final String fullname,
      required final String typeOfTest,
      required final String modality,
      required final String status,
      required final String state}) = _$TestelabModelImpl;
  const _TestelabModel._() : super._();

  factory _TestelabModel.fromJson(Map<String, dynamic> json) =
      _$TestelabModelImpl.fromJson;

  @override
  String get id;
  @override
  String get fullname;
  @override
  String get typeOfTest;
  @override
  String get modality;
  @override
  String get status;
  @override
  String get state;
  @override
  @JsonKey(ignore: true)
  _$$TestelabModelImplCopyWith<_$TestelabModelImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
