// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'res_soci.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$RegSociModel {
  String get id;
  String get image;
  String get name;
  String get city;
  @JsonKey(fromJson: getDateTimeLocalNullabe)
  DateTime? get birthdate;
  String get state;
  Map<String, dynamic> get fullData;
  String? get fullProfileLink;

  /// Create a copy of RegSociModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  $RegSociModelCopyWith<RegSociModel> get copyWith =>
      _$RegSociModelCopyWithImpl<RegSociModel>(
          this as RegSociModel, _$identity);

  /// Serializes this RegSociModel to a JSON map.
  Map<String, dynamic> toJson();

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is RegSociModel &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.image, image) || other.image == image) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.city, city) || other.city == city) &&
            (identical(other.birthdate, birthdate) ||
                other.birthdate == birthdate) &&
            (identical(other.state, state) || other.state == state) &&
            const DeepCollectionEquality().equals(other.fullData, fullData) &&
            (identical(other.fullProfileLink, fullProfileLink) ||
                other.fullProfileLink == fullProfileLink));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, image, name, city, birthdate,
      state, const DeepCollectionEquality().hash(fullData), fullProfileLink);

  @override
  String toString() {
    return 'RegSociModel(id: $id, image: $image, name: $name, city: $city, birthdate: $birthdate, state: $state, fullData: $fullData, fullProfileLink: $fullProfileLink)';
  }
}

/// @nodoc
abstract mixin class $RegSociModelCopyWith<$Res> {
  factory $RegSociModelCopyWith(
          RegSociModel value, $Res Function(RegSociModel) _then) =
      _$RegSociModelCopyWithImpl;
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
class _$RegSociModelCopyWithImpl<$Res> implements $RegSociModelCopyWith<$Res> {
  _$RegSociModelCopyWithImpl(this._self, this._then);

  final RegSociModel _self;
  final $Res Function(RegSociModel) _then;

  /// Create a copy of RegSociModel
  /// with the given fields replaced by the non-null parameter values.
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
    return _then(_self.copyWith(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      image: null == image
          ? _self.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _self.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      city: null == city
          ? _self.city
          : city // ignore: cast_nullable_to_non_nullable
              as String,
      birthdate: freezed == birthdate
          ? _self.birthdate
          : birthdate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      state: null == state
          ? _self.state
          : state // ignore: cast_nullable_to_non_nullable
              as String,
      fullData: null == fullData
          ? _self.fullData
          : fullData // ignore: cast_nullable_to_non_nullable
              as Map<String, dynamic>,
      fullProfileLink: freezed == fullProfileLink
          ? _self.fullProfileLink
          : fullProfileLink // ignore: cast_nullable_to_non_nullable
              as String?,
    ));
  }
}

/// Adds pattern-matching-related methods to [RegSociModel].
extension RegSociModelPatterns on RegSociModel {
  /// A variant of `map` that fallback to returning `orElse`.
  ///
  /// It is equivalent to doing:
  /// ```dart
  /// switch (sealedClass) {
  ///   case final Subclass value:
  ///     return ...;
  ///   case _:
  ///     return orElse();
  /// }
  /// ```

  @optionalTypeArgs
  TResult maybeMap<TResult extends Object?>(
    TResult Function(_RegSociModel value)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _RegSociModel() when $default != null:
        return $default(_that);
      case _:
        return orElse();
    }
  }

  /// A `switch`-like method, using callbacks.
  ///
  /// Callbacks receives the raw object, upcasted.
  /// It is equivalent to doing:
  /// ```dart
  /// switch (sealedClass) {
  ///   case final Subclass value:
  ///     return ...;
  ///   case final Subclass2 value:
  ///     return ...;
  /// }
  /// ```

  @optionalTypeArgs
  TResult map<TResult extends Object?>(
    TResult Function(_RegSociModel value) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _RegSociModel():
        return $default(_that);
      case _:
        throw StateError('Unexpected subclass');
    }
  }

  /// A variant of `map` that fallback to returning `null`.
  ///
  /// It is equivalent to doing:
  /// ```dart
  /// switch (sealedClass) {
  ///   case final Subclass value:
  ///     return ...;
  ///   case _:
  ///     return null;
  /// }
  /// ```

  @optionalTypeArgs
  TResult? mapOrNull<TResult extends Object?>(
    TResult? Function(_RegSociModel value)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _RegSociModel() when $default != null:
        return $default(_that);
      case _:
        return null;
    }
  }

  /// A variant of `when` that fallback to an `orElse` callback.
  ///
  /// It is equivalent to doing:
  /// ```dart
  /// switch (sealedClass) {
  ///   case Subclass(:final field):
  ///     return ...;
  ///   case _:
  ///     return orElse();
  /// }
  /// ```

  @optionalTypeArgs
  TResult maybeWhen<TResult extends Object?>(
    TResult Function(
            String id,
            String image,
            String name,
            String city,
            @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? birthdate,
            String state,
            Map<String, dynamic> fullData,
            String? fullProfileLink)?
        $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _RegSociModel() when $default != null:
        return $default(
            _that.id,
            _that.image,
            _that.name,
            _that.city,
            _that.birthdate,
            _that.state,
            _that.fullData,
            _that.fullProfileLink);
      case _:
        return orElse();
    }
  }

  /// A `switch`-like method, using callbacks.
  ///
  /// As opposed to `map`, this offers destructuring.
  /// It is equivalent to doing:
  /// ```dart
  /// switch (sealedClass) {
  ///   case Subclass(:final field):
  ///     return ...;
  ///   case Subclass2(:final field2):
  ///     return ...;
  /// }
  /// ```

  @optionalTypeArgs
  TResult when<TResult extends Object?>(
    TResult Function(
            String id,
            String image,
            String name,
            String city,
            @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? birthdate,
            String state,
            Map<String, dynamic> fullData,
            String? fullProfileLink)
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _RegSociModel():
        return $default(
            _that.id,
            _that.image,
            _that.name,
            _that.city,
            _that.birthdate,
            _that.state,
            _that.fullData,
            _that.fullProfileLink);
      case _:
        throw StateError('Unexpected subclass');
    }
  }

  /// A variant of `when` that fallback to returning `null`
  ///
  /// It is equivalent to doing:
  /// ```dart
  /// switch (sealedClass) {
  ///   case Subclass(:final field):
  ///     return ...;
  ///   case _:
  ///     return null;
  /// }
  /// ```

  @optionalTypeArgs
  TResult? whenOrNull<TResult extends Object?>(
    TResult? Function(
            String id,
            String image,
            String name,
            String city,
            @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? birthdate,
            String state,
            Map<String, dynamic> fullData,
            String? fullProfileLink)?
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _RegSociModel() when $default != null:
        return $default(
            _that.id,
            _that.image,
            _that.name,
            _that.city,
            _that.birthdate,
            _that.state,
            _that.fullData,
            _that.fullProfileLink);
      case _:
        return null;
    }
  }
}

/// @nodoc
@JsonSerializable()
class _RegSociModel extends RegSociModel {
  const _RegSociModel(
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
  factory _RegSociModel.fromJson(Map<String, dynamic> json) =>
      _$RegSociModelFromJson(json);

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

  /// Create a copy of RegSociModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  _$RegSociModelCopyWith<_RegSociModel> get copyWith =>
      __$RegSociModelCopyWithImpl<_RegSociModel>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$RegSociModelToJson(
      this,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _RegSociModel &&
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

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, image, name, city, birthdate,
      state, const DeepCollectionEquality().hash(_fullData), fullProfileLink);

  @override
  String toString() {
    return 'RegSociModel(id: $id, image: $image, name: $name, city: $city, birthdate: $birthdate, state: $state, fullData: $fullData, fullProfileLink: $fullProfileLink)';
  }
}

/// @nodoc
abstract mixin class _$RegSociModelCopyWith<$Res>
    implements $RegSociModelCopyWith<$Res> {
  factory _$RegSociModelCopyWith(
          _RegSociModel value, $Res Function(_RegSociModel) _then) =
      __$RegSociModelCopyWithImpl;
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
class __$RegSociModelCopyWithImpl<$Res>
    implements _$RegSociModelCopyWith<$Res> {
  __$RegSociModelCopyWithImpl(this._self, this._then);

  final _RegSociModel _self;
  final $Res Function(_RegSociModel) _then;

  /// Create a copy of RegSociModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
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
    return _then(_RegSociModel(
      id: null == id
          ? _self.id
          : id // ignore: cast_nullable_to_non_nullable
              as String,
      image: null == image
          ? _self.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _self.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      city: null == city
          ? _self.city
          : city // ignore: cast_nullable_to_non_nullable
              as String,
      birthdate: freezed == birthdate
          ? _self.birthdate
          : birthdate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      state: null == state
          ? _self.state
          : state // ignore: cast_nullable_to_non_nullable
              as String,
      fullData: null == fullData
          ? _self._fullData
          : fullData // ignore: cast_nullable_to_non_nullable
              as Map<String, dynamic>,
      fullProfileLink: freezed == fullProfileLink
          ? _self.fullProfileLink
          : fullProfileLink // ignore: cast_nullable_to_non_nullable
              as String?,
    ));
  }
}

/// @nodoc
mixin _$RegSociDBModel {
  @Id(assignable: true)
  int get uid;
  String get image;
  @Index()
  String get name;
  String get city;
  @JsonKey(fromJson: getDateTimeLocalNullabe)
  DateTime? get birthdate;
  String get state;
  String get fullDataJson;
  String? get fullProfileLink;
  String get nameToSearch;

  /// Create a copy of RegSociDBModel
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  $RegSociDBModelCopyWith<RegSociDBModel> get copyWith =>
      _$RegSociDBModelCopyWithImpl<RegSociDBModel>(
          this as RegSociDBModel, _$identity);

  /// Serializes this RegSociDBModel to a JSON map.
  Map<String, dynamic> toJson();

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is RegSociDBModel &&
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
                other.fullProfileLink == fullProfileLink) &&
            (identical(other.nameToSearch, nameToSearch) ||
                other.nameToSearch == nameToSearch));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, uid, image, name, city,
      birthdate, state, fullDataJson, fullProfileLink, nameToSearch);

  @override
  String toString() {
    return 'RegSociDBModel(uid: $uid, image: $image, name: $name, city: $city, birthdate: $birthdate, state: $state, fullDataJson: $fullDataJson, fullProfileLink: $fullProfileLink, nameToSearch: $nameToSearch)';
  }
}

/// @nodoc
abstract mixin class $RegSociDBModelCopyWith<$Res> {
  factory $RegSociDBModelCopyWith(
          RegSociDBModel value, $Res Function(RegSociDBModel) _then) =
      _$RegSociDBModelCopyWithImpl;
  @useResult
  $Res call(
      {@Id(assignable: true) int uid,
      String image,
      @Index() String name,
      String city,
      @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? birthdate,
      String state,
      String fullDataJson,
      String? fullProfileLink,
      String nameToSearch});
}

/// @nodoc
class _$RegSociDBModelCopyWithImpl<$Res>
    implements $RegSociDBModelCopyWith<$Res> {
  _$RegSociDBModelCopyWithImpl(this._self, this._then);

  final RegSociDBModel _self;
  final $Res Function(RegSociDBModel) _then;

  /// Create a copy of RegSociDBModel
  /// with the given fields replaced by the non-null parameter values.
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
    Object? nameToSearch = null,
  }) {
    return _then(_self.copyWith(
      uid: null == uid
          ? _self.uid
          : uid // ignore: cast_nullable_to_non_nullable
              as int,
      image: null == image
          ? _self.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _self.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      city: null == city
          ? _self.city
          : city // ignore: cast_nullable_to_non_nullable
              as String,
      birthdate: freezed == birthdate
          ? _self.birthdate
          : birthdate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      state: null == state
          ? _self.state
          : state // ignore: cast_nullable_to_non_nullable
              as String,
      fullDataJson: null == fullDataJson
          ? _self.fullDataJson
          : fullDataJson // ignore: cast_nullable_to_non_nullable
              as String,
      fullProfileLink: freezed == fullProfileLink
          ? _self.fullProfileLink
          : fullProfileLink // ignore: cast_nullable_to_non_nullable
              as String?,
      nameToSearch: null == nameToSearch
          ? _self.nameToSearch
          : nameToSearch // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

/// Adds pattern-matching-related methods to [RegSociDBModel].
extension RegSociDBModelPatterns on RegSociDBModel {
  /// A variant of `map` that fallback to returning `orElse`.
  ///
  /// It is equivalent to doing:
  /// ```dart
  /// switch (sealedClass) {
  ///   case final Subclass value:
  ///     return ...;
  ///   case _:
  ///     return orElse();
  /// }
  /// ```

  @optionalTypeArgs
  TResult maybeMap<TResult extends Object?>(
    TResult Function(_RegSociDBModel value)? $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _RegSociDBModel() when $default != null:
        return $default(_that);
      case _:
        return orElse();
    }
  }

  /// A `switch`-like method, using callbacks.
  ///
  /// Callbacks receives the raw object, upcasted.
  /// It is equivalent to doing:
  /// ```dart
  /// switch (sealedClass) {
  ///   case final Subclass value:
  ///     return ...;
  ///   case final Subclass2 value:
  ///     return ...;
  /// }
  /// ```

  @optionalTypeArgs
  TResult map<TResult extends Object?>(
    TResult Function(_RegSociDBModel value) $default,
  ) {
    final _that = this;
    switch (_that) {
      case _RegSociDBModel():
        return $default(_that);
      case _:
        throw StateError('Unexpected subclass');
    }
  }

  /// A variant of `map` that fallback to returning `null`.
  ///
  /// It is equivalent to doing:
  /// ```dart
  /// switch (sealedClass) {
  ///   case final Subclass value:
  ///     return ...;
  ///   case _:
  ///     return null;
  /// }
  /// ```

  @optionalTypeArgs
  TResult? mapOrNull<TResult extends Object?>(
    TResult? Function(_RegSociDBModel value)? $default,
  ) {
    final _that = this;
    switch (_that) {
      case _RegSociDBModel() when $default != null:
        return $default(_that);
      case _:
        return null;
    }
  }

  /// A variant of `when` that fallback to an `orElse` callback.
  ///
  /// It is equivalent to doing:
  /// ```dart
  /// switch (sealedClass) {
  ///   case Subclass(:final field):
  ///     return ...;
  ///   case _:
  ///     return orElse();
  /// }
  /// ```

  @optionalTypeArgs
  TResult maybeWhen<TResult extends Object?>(
    TResult Function(
            @Id(assignable: true) int uid,
            String image,
            @Index() String name,
            String city,
            @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? birthdate,
            String state,
            String fullDataJson,
            String? fullProfileLink,
            String nameToSearch)?
        $default, {
    required TResult orElse(),
  }) {
    final _that = this;
    switch (_that) {
      case _RegSociDBModel() when $default != null:
        return $default(
            _that.uid,
            _that.image,
            _that.name,
            _that.city,
            _that.birthdate,
            _that.state,
            _that.fullDataJson,
            _that.fullProfileLink,
            _that.nameToSearch);
      case _:
        return orElse();
    }
  }

  /// A `switch`-like method, using callbacks.
  ///
  /// As opposed to `map`, this offers destructuring.
  /// It is equivalent to doing:
  /// ```dart
  /// switch (sealedClass) {
  ///   case Subclass(:final field):
  ///     return ...;
  ///   case Subclass2(:final field2):
  ///     return ...;
  /// }
  /// ```

  @optionalTypeArgs
  TResult when<TResult extends Object?>(
    TResult Function(
            @Id(assignable: true) int uid,
            String image,
            @Index() String name,
            String city,
            @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? birthdate,
            String state,
            String fullDataJson,
            String? fullProfileLink,
            String nameToSearch)
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _RegSociDBModel():
        return $default(
            _that.uid,
            _that.image,
            _that.name,
            _that.city,
            _that.birthdate,
            _that.state,
            _that.fullDataJson,
            _that.fullProfileLink,
            _that.nameToSearch);
      case _:
        throw StateError('Unexpected subclass');
    }
  }

  /// A variant of `when` that fallback to returning `null`
  ///
  /// It is equivalent to doing:
  /// ```dart
  /// switch (sealedClass) {
  ///   case Subclass(:final field):
  ///     return ...;
  ///   case _:
  ///     return null;
  /// }
  /// ```

  @optionalTypeArgs
  TResult? whenOrNull<TResult extends Object?>(
    TResult? Function(
            @Id(assignable: true) int uid,
            String image,
            @Index() String name,
            String city,
            @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? birthdate,
            String state,
            String fullDataJson,
            String? fullProfileLink,
            String nameToSearch)?
        $default,
  ) {
    final _that = this;
    switch (_that) {
      case _RegSociDBModel() when $default != null:
        return $default(
            _that.uid,
            _that.image,
            _that.name,
            _that.city,
            _that.birthdate,
            _that.state,
            _that.fullDataJson,
            _that.fullProfileLink,
            _that.nameToSearch);
      case _:
        return null;
    }
  }
}

/// @nodoc
@JsonSerializable()
@Entity(realClass: RegSociDBModel)
class _RegSociDBModel extends RegSociDBModel {
  const _RegSociDBModel(
      {@Id(assignable: true) required this.uid,
      required this.image,
      @Index() required this.name,
      required this.city,
      @JsonKey(fromJson: getDateTimeLocalNullabe) required this.birthdate,
      required this.state,
      required this.fullDataJson,
      required this.fullProfileLink,
      required this.nameToSearch})
      : super._();
  factory _RegSociDBModel.fromJson(Map<String, dynamic> json) =>
      _$RegSociDBModelFromJson(json);

  @override
  @Id(assignable: true)
  final int uid;
  @override
  final String image;
  @override
  @Index()
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
  final String nameToSearch;

  /// Create a copy of RegSociDBModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  @pragma('vm:prefer-inline')
  _$RegSociDBModelCopyWith<_RegSociDBModel> get copyWith =>
      __$RegSociDBModelCopyWithImpl<_RegSociDBModel>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$RegSociDBModelToJson(
      this,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _RegSociDBModel &&
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
                other.fullProfileLink == fullProfileLink) &&
            (identical(other.nameToSearch, nameToSearch) ||
                other.nameToSearch == nameToSearch));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, uid, image, name, city,
      birthdate, state, fullDataJson, fullProfileLink, nameToSearch);

  @override
  String toString() {
    return 'RegSociDBModel(uid: $uid, image: $image, name: $name, city: $city, birthdate: $birthdate, state: $state, fullDataJson: $fullDataJson, fullProfileLink: $fullProfileLink, nameToSearch: $nameToSearch)';
  }
}

/// @nodoc
abstract mixin class _$RegSociDBModelCopyWith<$Res>
    implements $RegSociDBModelCopyWith<$Res> {
  factory _$RegSociDBModelCopyWith(
          _RegSociDBModel value, $Res Function(_RegSociDBModel) _then) =
      __$RegSociDBModelCopyWithImpl;
  @override
  @useResult
  $Res call(
      {@Id(assignable: true) int uid,
      String image,
      @Index() String name,
      String city,
      @JsonKey(fromJson: getDateTimeLocalNullabe) DateTime? birthdate,
      String state,
      String fullDataJson,
      String? fullProfileLink,
      String nameToSearch});
}

/// @nodoc
class __$RegSociDBModelCopyWithImpl<$Res>
    implements _$RegSociDBModelCopyWith<$Res> {
  __$RegSociDBModelCopyWithImpl(this._self, this._then);

  final _RegSociDBModel _self;
  final $Res Function(_RegSociDBModel) _then;

  /// Create a copy of RegSociDBModel
  /// with the given fields replaced by the non-null parameter values.
  @override
  @pragma('vm:prefer-inline')
  $Res call({
    Object? uid = null,
    Object? image = null,
    Object? name = null,
    Object? city = null,
    Object? birthdate = freezed,
    Object? state = null,
    Object? fullDataJson = null,
    Object? fullProfileLink = freezed,
    Object? nameToSearch = null,
  }) {
    return _then(_RegSociDBModel(
      uid: null == uid
          ? _self.uid
          : uid // ignore: cast_nullable_to_non_nullable
              as int,
      image: null == image
          ? _self.image
          : image // ignore: cast_nullable_to_non_nullable
              as String,
      name: null == name
          ? _self.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      city: null == city
          ? _self.city
          : city // ignore: cast_nullable_to_non_nullable
              as String,
      birthdate: freezed == birthdate
          ? _self.birthdate
          : birthdate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      state: null == state
          ? _self.state
          : state // ignore: cast_nullable_to_non_nullable
              as String,
      fullDataJson: null == fullDataJson
          ? _self.fullDataJson
          : fullDataJson // ignore: cast_nullable_to_non_nullable
              as String,
      fullProfileLink: freezed == fullProfileLink
          ? _self.fullProfileLink
          : fullProfileLink // ignore: cast_nullable_to_non_nullable
              as String?,
      nameToSearch: null == nameToSearch
          ? _self.nameToSearch
          : nameToSearch // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

// dart format on
