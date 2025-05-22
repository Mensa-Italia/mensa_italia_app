// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'res_soci.dart';

// **************************************************************************
// IsarCollectionGenerator
// **************************************************************************

// coverage:ignore-file
// ignore_for_file: duplicate_ignore, non_constant_identifier_names, constant_identifier_names, invalid_use_of_protected_member, unnecessary_cast, prefer_const_constructors, lines_longer_than_80_chars, require_trailing_commas, inference_failure_on_function_invocation, unnecessary_parenthesis, unnecessary_raw_strings, unnecessary_null_checks, join_return_with_assignment, prefer_final_locals, avoid_js_rounded_ints, avoid_positional_boolean_parameters, always_specify_types

extension GetRegSociDBModelCollection on Isar {
  IsarCollection<RegSociDBModel> get regSociDBModels => this.collection();
}

const RegSociDBModelSchema = CollectionSchema(
  name: r'RegSociDBModel',
  id: 5106881311153185436,
  properties: {
    r'birthdate': PropertySchema(
      id: 0,
      name: r'birthdate',
      type: IsarType.dateTime,
    ),
    r'city': PropertySchema(
      id: 1,
      name: r'city',
      type: IsarType.string,
    ),
    r'fullDataJson': PropertySchema(
      id: 2,
      name: r'fullDataJson',
      type: IsarType.string,
    ),
    r'fullProfileLink': PropertySchema(
      id: 3,
      name: r'fullProfileLink',
      type: IsarType.string,
    ),
    r'image': PropertySchema(
      id: 4,
      name: r'image',
      type: IsarType.string,
    ),
    r'name': PropertySchema(
      id: 5,
      name: r'name',
      type: IsarType.string,
    ),
    r'nameFullTextSearch': PropertySchema(
      id: 6,
      name: r'nameFullTextSearch',
      type: IsarType.stringList,
    ),
    r'state': PropertySchema(
      id: 7,
      name: r'state',
      type: IsarType.string,
    ),
    r'uid': PropertySchema(
      id: 8,
      name: r'uid',
      type: IsarType.long,
    )
  },
  estimateSize: _regSociDBModelEstimateSize,
  serialize: _regSociDBModelSerialize,
  deserialize: _regSociDBModelDeserialize,
  deserializeProp: _regSociDBModelDeserializeProp,
  idName: r'id',
  indexes: {
    r'nameFullTextSearch': IndexSchema(
      id: -3814169666367957423,
      name: r'nameFullTextSearch',
      unique: false,
      replace: false,
      properties: [
        IndexPropertySchema(
          name: r'nameFullTextSearch',
          type: IndexType.value,
          caseSensitive: false,
        )
      ],
    )
  },
  links: {},
  embeddedSchemas: {},
  getId: _regSociDBModelGetId,
  getLinks: _regSociDBModelGetLinks,
  attach: _regSociDBModelAttach,
  version: '3.1.0+1',
);

int _regSociDBModelEstimateSize(
  RegSociDBModel object,
  List<int> offsets,
  Map<Type, List<int>> allOffsets,
) {
  var bytesCount = offsets.last;
  bytesCount += 3 + object.city.length * 3;
  bytesCount += 3 + object.fullDataJson.length * 3;
  {
    final value = object.fullProfileLink;
    if (value != null) {
      bytesCount += 3 + value.length * 3;
    }
  }
  bytesCount += 3 + object.image.length * 3;
  bytesCount += 3 + object.name.length * 3;
  bytesCount += 3 + object.nameFullTextSearch.length * 3;
  {
    for (var i = 0; i < object.nameFullTextSearch.length; i++) {
      final value = object.nameFullTextSearch[i];
      bytesCount += value.length * 3;
    }
  }
  bytesCount += 3 + object.state.length * 3;
  return bytesCount;
}

void _regSociDBModelSerialize(
  RegSociDBModel object,
  IsarWriter writer,
  List<int> offsets,
  Map<Type, List<int>> allOffsets,
) {
  writer.writeDateTime(offsets[0], object.birthdate);
  writer.writeString(offsets[1], object.city);
  writer.writeString(offsets[2], object.fullDataJson);
  writer.writeString(offsets[3], object.fullProfileLink);
  writer.writeString(offsets[4], object.image);
  writer.writeString(offsets[5], object.name);
  writer.writeStringList(offsets[6], object.nameFullTextSearch);
  writer.writeString(offsets[7], object.state);
  writer.writeLong(offsets[8], object.uid);
}

RegSociDBModel _regSociDBModelDeserialize(
  Id id,
  IsarReader reader,
  List<int> offsets,
  Map<Type, List<int>> allOffsets,
) {
  final object = RegSociDBModel(
    birthdate: reader.readDateTimeOrNull(offsets[0]),
    city: reader.readString(offsets[1]),
    fullDataJson: reader.readString(offsets[2]),
    fullProfileLink: reader.readStringOrNull(offsets[3]),
    image: reader.readString(offsets[4]),
    name: reader.readString(offsets[5]),
    state: reader.readString(offsets[7]),
    uid: reader.readLong(offsets[8]),
  );
  return object;
}

P _regSociDBModelDeserializeProp<P>(
  IsarReader reader,
  int propertyId,
  int offset,
  Map<Type, List<int>> allOffsets,
) {
  switch (propertyId) {
    case 0:
      return (reader.readDateTimeOrNull(offset)) as P;
    case 1:
      return (reader.readString(offset)) as P;
    case 2:
      return (reader.readString(offset)) as P;
    case 3:
      return (reader.readStringOrNull(offset)) as P;
    case 4:
      return (reader.readString(offset)) as P;
    case 5:
      return (reader.readString(offset)) as P;
    case 6:
      return (reader.readStringList(offset) ?? []) as P;
    case 7:
      return (reader.readString(offset)) as P;
    case 8:
      return (reader.readLong(offset)) as P;
    default:
      throw IsarError('Unknown property with id $propertyId');
  }
}

Id _regSociDBModelGetId(RegSociDBModel object) {
  return object.id;
}

List<IsarLinkBase<dynamic>> _regSociDBModelGetLinks(RegSociDBModel object) {
  return [];
}

void _regSociDBModelAttach(
    IsarCollection<dynamic> col, Id id, RegSociDBModel object) {}

extension RegSociDBModelQueryWhereSort
    on QueryBuilder<RegSociDBModel, RegSociDBModel, QWhere> {
  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterWhere> anyId() {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(const IdWhereClause.any());
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterWhere>
      anyNameFullTextSearchElement() {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(
        const IndexWhereClause.any(indexName: r'nameFullTextSearch'),
      );
    });
  }
}

extension RegSociDBModelQueryWhere
    on QueryBuilder<RegSociDBModel, RegSociDBModel, QWhereClause> {
  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterWhereClause> idEqualTo(
      Id id) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(IdWhereClause.between(
        lower: id,
        upper: id,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterWhereClause> idNotEqualTo(
      Id id) {
    return QueryBuilder.apply(this, (query) {
      if (query.whereSort == Sort.asc) {
        return query
            .addWhereClause(
              IdWhereClause.lessThan(upper: id, includeUpper: false),
            )
            .addWhereClause(
              IdWhereClause.greaterThan(lower: id, includeLower: false),
            );
      } else {
        return query
            .addWhereClause(
              IdWhereClause.greaterThan(lower: id, includeLower: false),
            )
            .addWhereClause(
              IdWhereClause.lessThan(upper: id, includeUpper: false),
            );
      }
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterWhereClause> idGreaterThan(
      Id id,
      {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(
        IdWhereClause.greaterThan(lower: id, includeLower: include),
      );
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterWhereClause> idLessThan(
      Id id,
      {bool include = false}) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(
        IdWhereClause.lessThan(upper: id, includeUpper: include),
      );
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterWhereClause> idBetween(
    Id lowerId,
    Id upperId, {
    bool includeLower = true,
    bool includeUpper = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(IdWhereClause.between(
        lower: lowerId,
        includeLower: includeLower,
        upper: upperId,
        includeUpper: includeUpper,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterWhereClause>
      nameFullTextSearchElementEqualTo(String nameFullTextSearchElement) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(IndexWhereClause.equalTo(
        indexName: r'nameFullTextSearch',
        value: [nameFullTextSearchElement],
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterWhereClause>
      nameFullTextSearchElementNotEqualTo(String nameFullTextSearchElement) {
    return QueryBuilder.apply(this, (query) {
      if (query.whereSort == Sort.asc) {
        return query
            .addWhereClause(IndexWhereClause.between(
              indexName: r'nameFullTextSearch',
              lower: [],
              upper: [nameFullTextSearchElement],
              includeUpper: false,
            ))
            .addWhereClause(IndexWhereClause.between(
              indexName: r'nameFullTextSearch',
              lower: [nameFullTextSearchElement],
              includeLower: false,
              upper: [],
            ));
      } else {
        return query
            .addWhereClause(IndexWhereClause.between(
              indexName: r'nameFullTextSearch',
              lower: [nameFullTextSearchElement],
              includeLower: false,
              upper: [],
            ))
            .addWhereClause(IndexWhereClause.between(
              indexName: r'nameFullTextSearch',
              lower: [],
              upper: [nameFullTextSearchElement],
              includeUpper: false,
            ));
      }
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterWhereClause>
      nameFullTextSearchElementGreaterThan(
    String nameFullTextSearchElement, {
    bool include = false,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(IndexWhereClause.between(
        indexName: r'nameFullTextSearch',
        lower: [nameFullTextSearchElement],
        includeLower: include,
        upper: [],
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterWhereClause>
      nameFullTextSearchElementLessThan(
    String nameFullTextSearchElement, {
    bool include = false,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(IndexWhereClause.between(
        indexName: r'nameFullTextSearch',
        lower: [],
        upper: [nameFullTextSearchElement],
        includeUpper: include,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterWhereClause>
      nameFullTextSearchElementBetween(
    String lowerNameFullTextSearchElement,
    String upperNameFullTextSearchElement, {
    bool includeLower = true,
    bool includeUpper = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(IndexWhereClause.between(
        indexName: r'nameFullTextSearch',
        lower: [lowerNameFullTextSearchElement],
        includeLower: includeLower,
        upper: [upperNameFullTextSearchElement],
        includeUpper: includeUpper,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterWhereClause>
      nameFullTextSearchElementStartsWith(
          String NameFullTextSearchElementPrefix) {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(IndexWhereClause.between(
        indexName: r'nameFullTextSearch',
        lower: [NameFullTextSearchElementPrefix],
        upper: ['$NameFullTextSearchElementPrefix\u{FFFFF}'],
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterWhereClause>
      nameFullTextSearchElementIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addWhereClause(IndexWhereClause.equalTo(
        indexName: r'nameFullTextSearch',
        value: [''],
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterWhereClause>
      nameFullTextSearchElementIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      if (query.whereSort == Sort.asc) {
        return query
            .addWhereClause(IndexWhereClause.lessThan(
              indexName: r'nameFullTextSearch',
              upper: [''],
            ))
            .addWhereClause(IndexWhereClause.greaterThan(
              indexName: r'nameFullTextSearch',
              lower: [''],
            ));
      } else {
        return query
            .addWhereClause(IndexWhereClause.greaterThan(
              indexName: r'nameFullTextSearch',
              lower: [''],
            ))
            .addWhereClause(IndexWhereClause.lessThan(
              indexName: r'nameFullTextSearch',
              upper: [''],
            ));
      }
    });
  }
}

extension RegSociDBModelQueryFilter
    on QueryBuilder<RegSociDBModel, RegSociDBModel, QFilterCondition> {
  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      birthdateIsNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(const FilterCondition.isNull(
        property: r'birthdate',
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      birthdateIsNotNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(const FilterCondition.isNotNull(
        property: r'birthdate',
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      birthdateEqualTo(DateTime? value) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.equalTo(
        property: r'birthdate',
        value: value,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      birthdateGreaterThan(
    DateTime? value, {
    bool include = false,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.greaterThan(
        include: include,
        property: r'birthdate',
        value: value,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      birthdateLessThan(
    DateTime? value, {
    bool include = false,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.lessThan(
        include: include,
        property: r'birthdate',
        value: value,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      birthdateBetween(
    DateTime? lower,
    DateTime? upper, {
    bool includeLower = true,
    bool includeUpper = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.between(
        property: r'birthdate',
        lower: lower,
        includeLower: includeLower,
        upper: upper,
        includeUpper: includeUpper,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      cityEqualTo(
    String value, {
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.equalTo(
        property: r'city',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      cityGreaterThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.greaterThan(
        include: include,
        property: r'city',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      cityLessThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.lessThan(
        include: include,
        property: r'city',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      cityBetween(
    String lower,
    String upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.between(
        property: r'city',
        lower: lower,
        includeLower: includeLower,
        upper: upper,
        includeUpper: includeUpper,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      cityStartsWith(
    String value, {
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.startsWith(
        property: r'city',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      cityEndsWith(
    String value, {
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.endsWith(
        property: r'city',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      cityContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.contains(
        property: r'city',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      cityMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.matches(
        property: r'city',
        wildcard: pattern,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      cityIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.equalTo(
        property: r'city',
        value: '',
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      cityIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.greaterThan(
        property: r'city',
        value: '',
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullDataJsonEqualTo(
    String value, {
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.equalTo(
        property: r'fullDataJson',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullDataJsonGreaterThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.greaterThan(
        include: include,
        property: r'fullDataJson',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullDataJsonLessThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.lessThan(
        include: include,
        property: r'fullDataJson',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullDataJsonBetween(
    String lower,
    String upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.between(
        property: r'fullDataJson',
        lower: lower,
        includeLower: includeLower,
        upper: upper,
        includeUpper: includeUpper,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullDataJsonStartsWith(
    String value, {
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.startsWith(
        property: r'fullDataJson',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullDataJsonEndsWith(
    String value, {
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.endsWith(
        property: r'fullDataJson',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullDataJsonContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.contains(
        property: r'fullDataJson',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullDataJsonMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.matches(
        property: r'fullDataJson',
        wildcard: pattern,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullDataJsonIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.equalTo(
        property: r'fullDataJson',
        value: '',
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullDataJsonIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.greaterThan(
        property: r'fullDataJson',
        value: '',
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullProfileLinkIsNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(const FilterCondition.isNull(
        property: r'fullProfileLink',
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullProfileLinkIsNotNull() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(const FilterCondition.isNotNull(
        property: r'fullProfileLink',
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullProfileLinkEqualTo(
    String? value, {
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.equalTo(
        property: r'fullProfileLink',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullProfileLinkGreaterThan(
    String? value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.greaterThan(
        include: include,
        property: r'fullProfileLink',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullProfileLinkLessThan(
    String? value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.lessThan(
        include: include,
        property: r'fullProfileLink',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullProfileLinkBetween(
    String? lower,
    String? upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.between(
        property: r'fullProfileLink',
        lower: lower,
        includeLower: includeLower,
        upper: upper,
        includeUpper: includeUpper,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullProfileLinkStartsWith(
    String value, {
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.startsWith(
        property: r'fullProfileLink',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullProfileLinkEndsWith(
    String value, {
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.endsWith(
        property: r'fullProfileLink',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullProfileLinkContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.contains(
        property: r'fullProfileLink',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullProfileLinkMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.matches(
        property: r'fullProfileLink',
        wildcard: pattern,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullProfileLinkIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.equalTo(
        property: r'fullProfileLink',
        value: '',
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      fullProfileLinkIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.greaterThan(
        property: r'fullProfileLink',
        value: '',
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition> idEqualTo(
      Id value) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.equalTo(
        property: r'id',
        value: value,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      idGreaterThan(
    Id value, {
    bool include = false,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.greaterThan(
        include: include,
        property: r'id',
        value: value,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      idLessThan(
    Id value, {
    bool include = false,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.lessThan(
        include: include,
        property: r'id',
        value: value,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition> idBetween(
    Id lower,
    Id upper, {
    bool includeLower = true,
    bool includeUpper = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.between(
        property: r'id',
        lower: lower,
        includeLower: includeLower,
        upper: upper,
        includeUpper: includeUpper,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      imageEqualTo(
    String value, {
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.equalTo(
        property: r'image',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      imageGreaterThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.greaterThan(
        include: include,
        property: r'image',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      imageLessThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.lessThan(
        include: include,
        property: r'image',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      imageBetween(
    String lower,
    String upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.between(
        property: r'image',
        lower: lower,
        includeLower: includeLower,
        upper: upper,
        includeUpper: includeUpper,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      imageStartsWith(
    String value, {
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.startsWith(
        property: r'image',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      imageEndsWith(
    String value, {
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.endsWith(
        property: r'image',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      imageContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.contains(
        property: r'image',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      imageMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.matches(
        property: r'image',
        wildcard: pattern,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      imageIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.equalTo(
        property: r'image',
        value: '',
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      imageIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.greaterThan(
        property: r'image',
        value: '',
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameEqualTo(
    String value, {
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.equalTo(
        property: r'name',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameGreaterThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.greaterThan(
        include: include,
        property: r'name',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameLessThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.lessThan(
        include: include,
        property: r'name',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameBetween(
    String lower,
    String upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.between(
        property: r'name',
        lower: lower,
        includeLower: includeLower,
        upper: upper,
        includeUpper: includeUpper,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameStartsWith(
    String value, {
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.startsWith(
        property: r'name',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameEndsWith(
    String value, {
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.endsWith(
        property: r'name',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.contains(
        property: r'name',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.matches(
        property: r'name',
        wildcard: pattern,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.equalTo(
        property: r'name',
        value: '',
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.greaterThan(
        property: r'name',
        value: '',
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameFullTextSearchElementEqualTo(
    String value, {
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.equalTo(
        property: r'nameFullTextSearch',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameFullTextSearchElementGreaterThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.greaterThan(
        include: include,
        property: r'nameFullTextSearch',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameFullTextSearchElementLessThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.lessThan(
        include: include,
        property: r'nameFullTextSearch',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameFullTextSearchElementBetween(
    String lower,
    String upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.between(
        property: r'nameFullTextSearch',
        lower: lower,
        includeLower: includeLower,
        upper: upper,
        includeUpper: includeUpper,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameFullTextSearchElementStartsWith(
    String value, {
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.startsWith(
        property: r'nameFullTextSearch',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameFullTextSearchElementEndsWith(
    String value, {
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.endsWith(
        property: r'nameFullTextSearch',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameFullTextSearchElementContains(String value,
          {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.contains(
        property: r'nameFullTextSearch',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameFullTextSearchElementMatches(String pattern,
          {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.matches(
        property: r'nameFullTextSearch',
        wildcard: pattern,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameFullTextSearchElementIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.equalTo(
        property: r'nameFullTextSearch',
        value: '',
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameFullTextSearchElementIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.greaterThan(
        property: r'nameFullTextSearch',
        value: '',
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameFullTextSearchLengthEqualTo(int length) {
    return QueryBuilder.apply(this, (query) {
      return query.listLength(
        r'nameFullTextSearch',
        length,
        true,
        length,
        true,
      );
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameFullTextSearchIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.listLength(
        r'nameFullTextSearch',
        0,
        true,
        0,
        true,
      );
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameFullTextSearchIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.listLength(
        r'nameFullTextSearch',
        0,
        false,
        999999,
        true,
      );
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameFullTextSearchLengthLessThan(
    int length, {
    bool include = false,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.listLength(
        r'nameFullTextSearch',
        0,
        true,
        length,
        include,
      );
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameFullTextSearchLengthGreaterThan(
    int length, {
    bool include = false,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.listLength(
        r'nameFullTextSearch',
        length,
        include,
        999999,
        true,
      );
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      nameFullTextSearchLengthBetween(
    int lower,
    int upper, {
    bool includeLower = true,
    bool includeUpper = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.listLength(
        r'nameFullTextSearch',
        lower,
        includeLower,
        upper,
        includeUpper,
      );
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      stateEqualTo(
    String value, {
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.equalTo(
        property: r'state',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      stateGreaterThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.greaterThan(
        include: include,
        property: r'state',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      stateLessThan(
    String value, {
    bool include = false,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.lessThan(
        include: include,
        property: r'state',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      stateBetween(
    String lower,
    String upper, {
    bool includeLower = true,
    bool includeUpper = true,
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.between(
        property: r'state',
        lower: lower,
        includeLower: includeLower,
        upper: upper,
        includeUpper: includeUpper,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      stateStartsWith(
    String value, {
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.startsWith(
        property: r'state',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      stateEndsWith(
    String value, {
    bool caseSensitive = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.endsWith(
        property: r'state',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      stateContains(String value, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.contains(
        property: r'state',
        value: value,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      stateMatches(String pattern, {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.matches(
        property: r'state',
        wildcard: pattern,
        caseSensitive: caseSensitive,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      stateIsEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.equalTo(
        property: r'state',
        value: '',
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      stateIsNotEmpty() {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.greaterThan(
        property: r'state',
        value: '',
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      uidEqualTo(int value) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.equalTo(
        property: r'uid',
        value: value,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      uidGreaterThan(
    int value, {
    bool include = false,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.greaterThan(
        include: include,
        property: r'uid',
        value: value,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      uidLessThan(
    int value, {
    bool include = false,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.lessThan(
        include: include,
        property: r'uid',
        value: value,
      ));
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterFilterCondition>
      uidBetween(
    int lower,
    int upper, {
    bool includeLower = true,
    bool includeUpper = true,
  }) {
    return QueryBuilder.apply(this, (query) {
      return query.addFilterCondition(FilterCondition.between(
        property: r'uid',
        lower: lower,
        includeLower: includeLower,
        upper: upper,
        includeUpper: includeUpper,
      ));
    });
  }
}

extension RegSociDBModelQueryObject
    on QueryBuilder<RegSociDBModel, RegSociDBModel, QFilterCondition> {}

extension RegSociDBModelQueryLinks
    on QueryBuilder<RegSociDBModel, RegSociDBModel, QFilterCondition> {}

extension RegSociDBModelQuerySortBy
    on QueryBuilder<RegSociDBModel, RegSociDBModel, QSortBy> {
  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> sortByBirthdate() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'birthdate', Sort.asc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy>
      sortByBirthdateDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'birthdate', Sort.desc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> sortByCity() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'city', Sort.asc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> sortByCityDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'city', Sort.desc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy>
      sortByFullDataJson() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'fullDataJson', Sort.asc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy>
      sortByFullDataJsonDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'fullDataJson', Sort.desc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy>
      sortByFullProfileLink() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'fullProfileLink', Sort.asc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy>
      sortByFullProfileLinkDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'fullProfileLink', Sort.desc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> sortByImage() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'image', Sort.asc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> sortByImageDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'image', Sort.desc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> sortByName() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'name', Sort.asc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> sortByNameDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'name', Sort.desc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> sortByState() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'state', Sort.asc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> sortByStateDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'state', Sort.desc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> sortByUid() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'uid', Sort.asc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> sortByUidDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'uid', Sort.desc);
    });
  }
}

extension RegSociDBModelQuerySortThenBy
    on QueryBuilder<RegSociDBModel, RegSociDBModel, QSortThenBy> {
  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> thenByBirthdate() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'birthdate', Sort.asc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy>
      thenByBirthdateDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'birthdate', Sort.desc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> thenByCity() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'city', Sort.asc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> thenByCityDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'city', Sort.desc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy>
      thenByFullDataJson() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'fullDataJson', Sort.asc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy>
      thenByFullDataJsonDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'fullDataJson', Sort.desc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy>
      thenByFullProfileLink() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'fullProfileLink', Sort.asc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy>
      thenByFullProfileLinkDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'fullProfileLink', Sort.desc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> thenById() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'id', Sort.asc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> thenByIdDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'id', Sort.desc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> thenByImage() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'image', Sort.asc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> thenByImageDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'image', Sort.desc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> thenByName() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'name', Sort.asc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> thenByNameDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'name', Sort.desc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> thenByState() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'state', Sort.asc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> thenByStateDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'state', Sort.desc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> thenByUid() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'uid', Sort.asc);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QAfterSortBy> thenByUidDesc() {
    return QueryBuilder.apply(this, (query) {
      return query.addSortBy(r'uid', Sort.desc);
    });
  }
}

extension RegSociDBModelQueryWhereDistinct
    on QueryBuilder<RegSociDBModel, RegSociDBModel, QDistinct> {
  QueryBuilder<RegSociDBModel, RegSociDBModel, QDistinct>
      distinctByBirthdate() {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'birthdate');
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QDistinct> distinctByCity(
      {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'city', caseSensitive: caseSensitive);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QDistinct>
      distinctByFullDataJson({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'fullDataJson', caseSensitive: caseSensitive);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QDistinct>
      distinctByFullProfileLink({bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'fullProfileLink',
          caseSensitive: caseSensitive);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QDistinct> distinctByImage(
      {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'image', caseSensitive: caseSensitive);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QDistinct> distinctByName(
      {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'name', caseSensitive: caseSensitive);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QDistinct>
      distinctByNameFullTextSearch() {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'nameFullTextSearch');
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QDistinct> distinctByState(
      {bool caseSensitive = true}) {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'state', caseSensitive: caseSensitive);
    });
  }

  QueryBuilder<RegSociDBModel, RegSociDBModel, QDistinct> distinctByUid() {
    return QueryBuilder.apply(this, (query) {
      return query.addDistinctBy(r'uid');
    });
  }
}

extension RegSociDBModelQueryProperty
    on QueryBuilder<RegSociDBModel, RegSociDBModel, QQueryProperty> {
  QueryBuilder<RegSociDBModel, int, QQueryOperations> idProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'id');
    });
  }

  QueryBuilder<RegSociDBModel, DateTime?, QQueryOperations>
      birthdateProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'birthdate');
    });
  }

  QueryBuilder<RegSociDBModel, String, QQueryOperations> cityProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'city');
    });
  }

  QueryBuilder<RegSociDBModel, String, QQueryOperations>
      fullDataJsonProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'fullDataJson');
    });
  }

  QueryBuilder<RegSociDBModel, String?, QQueryOperations>
      fullProfileLinkProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'fullProfileLink');
    });
  }

  QueryBuilder<RegSociDBModel, String, QQueryOperations> imageProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'image');
    });
  }

  QueryBuilder<RegSociDBModel, String, QQueryOperations> nameProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'name');
    });
  }

  QueryBuilder<RegSociDBModel, List<String>, QQueryOperations>
      nameFullTextSearchProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'nameFullTextSearch');
    });
  }

  QueryBuilder<RegSociDBModel, String, QQueryOperations> stateProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'state');
    });
  }

  QueryBuilder<RegSociDBModel, int, QQueryOperations> uidProperty() {
    return QueryBuilder.apply(this, (query) {
      return query.addPropertyName(r'uid');
    });
  }
}

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$RegSociModelImpl _$$RegSociModelImplFromJson(Map<String, dynamic> json) =>
    _$RegSociModelImpl(
      id: json['id'] as String,
      image: json['image'] as String,
      name: json['name'] as String,
      city: json['city'] as String,
      birthdate: getDateTimeLocalNullabe(json['birthdate'] as String),
      state: json['state'] as String,
      fullData: json['full_data'] as Map<String, dynamic>,
      fullProfileLink: json['full_profile_link'] as String?,
    );

Map<String, dynamic> _$$RegSociModelImplToJson(_$RegSociModelImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'image': instance.image,
      'name': instance.name,
      'city': instance.city,
      'birthdate': instance.birthdate?.toIso8601String(),
      'state': instance.state,
      'full_data': instance.fullData,
      'full_profile_link': instance.fullProfileLink,
    };

_$RegSociDBModelImpl _$$RegSociDBModelImplFromJson(Map<String, dynamic> json) =>
    _$RegSociDBModelImpl(
      uid: (json['uid'] as num).toInt(),
      image: json['image'] as String,
      name: json['name'] as String,
      city: json['city'] as String,
      birthdate: getDateTimeLocalNullabe(json['birthdate'] as String),
      state: json['state'] as String,
      fullDataJson: json['full_data_json'] as String,
      fullProfileLink: json['full_profile_link'] as String?,
    );

Map<String, dynamic> _$$RegSociDBModelImplToJson(
        _$RegSociDBModelImpl instance) =>
    <String, dynamic>{
      'uid': instance.uid,
      'image': instance.image,
      'name': instance.name,
      'city': instance.city,
      'birthdate': instance.birthdate?.toIso8601String(),
      'state': instance.state,
      'full_data_json': instance.fullDataJson,
      'full_profile_link': instance.fullProfileLink,
    };
