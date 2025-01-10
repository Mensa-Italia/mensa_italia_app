// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'receipt.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

ReceiptModel _$ReceiptModelFromJson(Map<String, dynamic> json) {
  return _ReceiptModel.fromJson(json);
}

/// @nodoc
mixin _$ReceiptModel {
  String get id => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  String get user => throw _privateConstructorUsedError;
  String get stripeCode => throw _privateConstructorUsedError;
  String get status => throw _privateConstructorUsedError;
  int get amount => throw _privateConstructorUsedError;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get created => throw _privateConstructorUsedError;
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get updated => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $ReceiptModelCopyWith<ReceiptModel> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $ReceiptModelCopyWith<$Res> {
  factory $ReceiptModelCopyWith(
          ReceiptModel value, $Res Function(ReceiptModel) then) =
      _$ReceiptModelCopyWithImpl<$Res, ReceiptModel>;
  @useResult
  $Res call(
      {String id,
      String? description,
      String user,
      String stripeCode,
      String status,
      int amount,
      @JsonKey(fromJson: getDateTimeLocal) DateTime created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime updated});
}

/// @nodoc
class _$ReceiptModelCopyWithImpl<$Res, $Val extends ReceiptModel>
    implements $ReceiptModelCopyWith<$Res> {
  _$ReceiptModelCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? description = freezed,
    Object? user = null,
    Object? stripeCode = null,
    Object? status = null,
    Object? amount = null,
    Object? created = null,
    Object? updated = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      user: null == user
          ? _value.user
          : user // ignore: cast_nullable_to_non_nullable
              as String,
      stripeCode: null == stripeCode
          ? _value.stripeCode
          : stripeCode // ignore: cast_nullable_to_non_nullable
              as String,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as String,
      amount: null == amount
          ? _value.amount
          : amount // ignore: cast_nullable_to_non_nullable
              as int,
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
abstract class _$$ReceiptModelImplCopyWith<$Res>
    implements $ReceiptModelCopyWith<$Res> {
  factory _$$ReceiptModelImplCopyWith(
          _$ReceiptModelImpl value, $Res Function(_$ReceiptModelImpl) then) =
      __$$ReceiptModelImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {String id,
      String? description,
      String user,
      String stripeCode,
      String status,
      int amount,
      @JsonKey(fromJson: getDateTimeLocal) DateTime created,
      @JsonKey(fromJson: getDateTimeLocal) DateTime updated});
}

/// @nodoc
class __$$ReceiptModelImplCopyWithImpl<$Res>
    extends _$ReceiptModelCopyWithImpl<$Res, _$ReceiptModelImpl>
    implements _$$ReceiptModelImplCopyWith<$Res> {
  __$$ReceiptModelImplCopyWithImpl(
      _$ReceiptModelImpl _value, $Res Function(_$ReceiptModelImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? description = freezed,
    Object? user = null,
    Object? stripeCode = null,
    Object? status = null,
    Object? amount = null,
    Object? created = null,
    Object? updated = null,
  }) {
    return _then(_$ReceiptModelImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      user: null == user
          ? _value.user
          : user // ignore: cast_nullable_to_non_nullable
              as String,
      stripeCode: null == stripeCode
          ? _value.stripeCode
          : stripeCode // ignore: cast_nullable_to_non_nullable
              as String,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as String,
      amount: null == amount
          ? _value.amount
          : amount // ignore: cast_nullable_to_non_nullable
              as int,
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
class _$ReceiptModelImpl implements _ReceiptModel {
  const _$ReceiptModelImpl(
      {required this.id,
      required this.description,
      required this.user,
      required this.stripeCode,
      required this.status,
      required this.amount,
      @JsonKey(fromJson: getDateTimeLocal) required this.created,
      @JsonKey(fromJson: getDateTimeLocal) required this.updated});

  factory _$ReceiptModelImpl.fromJson(Map<String, dynamic> json) =>
      _$$ReceiptModelImplFromJson(json);

  @override
  final String id;
  @override
  final String? description;
  @override
  final String user;
  @override
  final String stripeCode;
  @override
  final String status;
  @override
  final int amount;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime created;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  final DateTime updated;

  @override
  String toString() {
    return 'ReceiptModel(id: $id, description: $description, user: $user, stripeCode: $stripeCode, status: $status, amount: $amount, created: $created, updated: $updated)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$ReceiptModelImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.user, user) || other.user == user) &&
            (identical(other.stripeCode, stripeCode) ||
                other.stripeCode == stripeCode) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.amount, amount) || other.amount == amount) &&
            (identical(other.created, created) || other.created == created) &&
            (identical(other.updated, updated) || other.updated == updated));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode => Object.hash(runtimeType, id, description, user,
      stripeCode, status, amount, created, updated);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$ReceiptModelImplCopyWith<_$ReceiptModelImpl> get copyWith =>
      __$$ReceiptModelImplCopyWithImpl<_$ReceiptModelImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$ReceiptModelImplToJson(
      this,
    );
  }
}

abstract class _ReceiptModel implements ReceiptModel {
  const factory _ReceiptModel(
      {required final String id,
      required final String? description,
      required final String user,
      required final String stripeCode,
      required final String status,
      required final int amount,
      @JsonKey(fromJson: getDateTimeLocal) required final DateTime created,
      @JsonKey(fromJson: getDateTimeLocal)
      required final DateTime updated}) = _$ReceiptModelImpl;

  factory _ReceiptModel.fromJson(Map<String, dynamic> json) =
      _$ReceiptModelImpl.fromJson;

  @override
  String get id;
  @override
  String? get description;
  @override
  String get user;
  @override
  String get stripeCode;
  @override
  String get status;
  @override
  int get amount;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get created;
  @override
  @JsonKey(fromJson: getDateTimeLocal)
  DateTime get updated;
  @override
  @JsonKey(ignore: true)
  _$$ReceiptModelImplCopyWith<_$ReceiptModelImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
