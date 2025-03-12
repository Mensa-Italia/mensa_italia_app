// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'document_elaborated.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

DocumentElaboratedModel _$DocumentElaboratedModelFromJson(
    Map<String, dynamic> json) {
  return _DocumentElaboratedModel.fromJson(json);
}

/// @nodoc
mixin _$DocumentElaboratedModel {
  String get id => throw _privateConstructorUsedError;
  String get document => throw _privateConstructorUsedError;
  String get iaResume => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $DocumentElaboratedModelCopyWith<DocumentElaboratedModel> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $DocumentElaboratedModelCopyWith<$Res> {
  factory $DocumentElaboratedModelCopyWith(DocumentElaboratedModel value,
          $Res Function(DocumentElaboratedModel) then) =
      _$DocumentElaboratedModelCopyWithImpl<$Res, DocumentElaboratedModel>;
  @useResult
  $Res call({String id, String document, String iaResume});
}

/// @nodoc
class _$DocumentElaboratedModelCopyWithImpl<$Res,
        $Val extends DocumentElaboratedModel>
    implements $DocumentElaboratedModelCopyWith<$Res> {
  _$DocumentElaboratedModelCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? document = null,
    Object? iaResume = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      document: null == document
          ? _value.document
          : document // ignore: cast_nullable_to_non_nullable
              as String,
      iaResume: null == iaResume
          ? _value.iaResume
          : iaResume // ignore: cast_nullable_to_non_nullable
              as String,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$DocumentElaboratedModelImplCopyWith<$Res>
    implements $DocumentElaboratedModelCopyWith<$Res> {
  factory _$$DocumentElaboratedModelImplCopyWith(
          _$DocumentElaboratedModelImpl value,
          $Res Function(_$DocumentElaboratedModelImpl) then) =
      __$$DocumentElaboratedModelImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({String id, String document, String iaResume});
}

/// @nodoc
class __$$DocumentElaboratedModelImplCopyWithImpl<$Res>
    extends _$DocumentElaboratedModelCopyWithImpl<$Res,
        _$DocumentElaboratedModelImpl>
    implements _$$DocumentElaboratedModelImplCopyWith<$Res> {
  __$$DocumentElaboratedModelImplCopyWithImpl(
      _$DocumentElaboratedModelImpl _value,
      $Res Function(_$DocumentElaboratedModelImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? document = null,
    Object? iaResume = null,
  }) {
    return _then(_$DocumentElaboratedModelImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      document: null == document
          ? _value.document
          : document // ignore: cast_nullable_to_non_nullable
              as String,
      iaResume: null == iaResume
          ? _value.iaResume
          : iaResume // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$DocumentElaboratedModelImpl implements _DocumentElaboratedModel {
  const _$DocumentElaboratedModelImpl(
      {required this.id, required this.document, required this.iaResume});

  factory _$DocumentElaboratedModelImpl.fromJson(Map<String, dynamic> json) =>
      _$$DocumentElaboratedModelImplFromJson(json);

  @override
  final String id;
  @override
  final String document;
  @override
  final String iaResume;

  @override
  String toString() {
    return 'DocumentElaboratedModel(id: $id, document: $document, iaResume: $iaResume)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$DocumentElaboratedModelImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.document, document) ||
                other.document == document) &&
            (identical(other.iaResume, iaResume) ||
                other.iaResume == iaResume));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode => Object.hash(runtimeType, id, document, iaResume);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$DocumentElaboratedModelImplCopyWith<_$DocumentElaboratedModelImpl>
      get copyWith => __$$DocumentElaboratedModelImplCopyWithImpl<
          _$DocumentElaboratedModelImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$DocumentElaboratedModelImplToJson(
      this,
    );
  }
}

abstract class _DocumentElaboratedModel implements DocumentElaboratedModel {
  const factory _DocumentElaboratedModel(
      {required final String id,
      required final String document,
      required final String iaResume}) = _$DocumentElaboratedModelImpl;

  factory _DocumentElaboratedModel.fromJson(Map<String, dynamic> json) =
      _$DocumentElaboratedModelImpl.fromJson;

  @override
  String get id;
  @override
  String get document;
  @override
  String get iaResume;
  @override
  @JsonKey(ignore: true)
  _$$DocumentElaboratedModelImplCopyWith<_$DocumentElaboratedModelImpl>
      get copyWith => throw _privateConstructorUsedError;
}
