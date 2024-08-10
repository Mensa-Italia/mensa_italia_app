// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'calendar_link.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

CalendarLinkModel _$CalendarLinkModelFromJson(Map<String, dynamic> json) {
  return _CalendarLinkModel.fromJson(json);
}

/// @nodoc
mixin _$CalendarLinkModel {
  String get id => throw _privateConstructorUsedError;
  String get user => throw _privateConstructorUsedError;
  String get hash => throw _privateConstructorUsedError;
  List<String> get state => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $CalendarLinkModelCopyWith<CalendarLinkModel> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $CalendarLinkModelCopyWith<$Res> {
  factory $CalendarLinkModelCopyWith(
          CalendarLinkModel value, $Res Function(CalendarLinkModel) then) =
      _$CalendarLinkModelCopyWithImpl<$Res, CalendarLinkModel>;
  @useResult
  $Res call({String id, String user, String hash, List<String> state});
}

/// @nodoc
class _$CalendarLinkModelCopyWithImpl<$Res, $Val extends CalendarLinkModel>
    implements $CalendarLinkModelCopyWith<$Res> {
  _$CalendarLinkModelCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? user = null,
    Object? hash = null,
    Object? state = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      user: null == user
          ? _value.user
          : user // ignore: cast_nullable_to_non_nullable
              as String,
      hash: null == hash
          ? _value.hash
          : hash // ignore: cast_nullable_to_non_nullable
              as String,
      state: null == state
          ? _value.state
          : state // ignore: cast_nullable_to_non_nullable
              as List<String>,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$CalendarLinkModelImplCopyWith<$Res>
    implements $CalendarLinkModelCopyWith<$Res> {
  factory _$$CalendarLinkModelImplCopyWith(_$CalendarLinkModelImpl value,
          $Res Function(_$CalendarLinkModelImpl) then) =
      __$$CalendarLinkModelImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({String id, String user, String hash, List<String> state});
}

/// @nodoc
class __$$CalendarLinkModelImplCopyWithImpl<$Res>
    extends _$CalendarLinkModelCopyWithImpl<$Res, _$CalendarLinkModelImpl>
    implements _$$CalendarLinkModelImplCopyWith<$Res> {
  __$$CalendarLinkModelImplCopyWithImpl(_$CalendarLinkModelImpl _value,
      $Res Function(_$CalendarLinkModelImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? user = null,
    Object? hash = null,
    Object? state = null,
  }) {
    return _then(_$CalendarLinkModelImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      user: null == user
          ? _value.user
          : user // ignore: cast_nullable_to_non_nullable
              as String,
      hash: null == hash
          ? _value.hash
          : hash // ignore: cast_nullable_to_non_nullable
              as String,
      state: null == state
          ? _value._state
          : state // ignore: cast_nullable_to_non_nullable
              as List<String>,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$CalendarLinkModelImpl implements _CalendarLinkModel {
  _$CalendarLinkModelImpl(
      {required this.id,
      required this.user,
      required this.hash,
      required final List<String> state})
      : _state = state;

  factory _$CalendarLinkModelImpl.fromJson(Map<String, dynamic> json) =>
      _$$CalendarLinkModelImplFromJson(json);

  @override
  final String id;
  @override
  final String user;
  @override
  final String hash;
  final List<String> _state;
  @override
  List<String> get state {
    if (_state is EqualUnmodifiableListView) return _state;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_state);
  }

  @override
  String toString() {
    return 'CalendarLinkModel(id: $id, user: $user, hash: $hash, state: $state)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$CalendarLinkModelImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.user, user) || other.user == user) &&
            (identical(other.hash, hash) || other.hash == hash) &&
            const DeepCollectionEquality().equals(other._state, _state));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode => Object.hash(
      runtimeType, id, user, hash, const DeepCollectionEquality().hash(_state));

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$CalendarLinkModelImplCopyWith<_$CalendarLinkModelImpl> get copyWith =>
      __$$CalendarLinkModelImplCopyWithImpl<_$CalendarLinkModelImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$CalendarLinkModelImplToJson(
      this,
    );
  }
}

abstract class _CalendarLinkModel implements CalendarLinkModel {
  factory _CalendarLinkModel(
      {required final String id,
      required final String user,
      required final String hash,
      required final List<String> state}) = _$CalendarLinkModelImpl;

  factory _CalendarLinkModel.fromJson(Map<String, dynamic> json) =
      _$CalendarLinkModelImpl.fromJson;

  @override
  String get id;
  @override
  String get user;
  @override
  String get hash;
  @override
  List<String> get state;
  @override
  @JsonKey(ignore: true)
  _$$CalendarLinkModelImplCopyWith<_$CalendarLinkModelImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
