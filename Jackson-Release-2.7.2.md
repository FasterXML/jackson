Patch version of [2.7](Jackson-Release-2.7), released February 27th, 2016.

### Changes, core

#### [Databind](../../jackson-databind)

* [#1115](../../jackson-databind/issues/1115): Problems with deprecated `TypeFactory.constructType(type, ctxt)` methods if `ctxt` is `null`
* [#1124](../../jackson-databind/issues/1124): JsonAnyGetter ignores JsonSerialize(contentUsing=...)
* [#1128](../../jackson-databind/issues/1128): UnrecognizedPropertyException in 2.7.1 for properties that work with version 2.6.5
* [#1129](../../jackson-databind/issues/1129): When applying type modifiers, don't ignore container types.
* [#1130](../../jackson-databind/issues/1130): NPE in `StdDateFormat` hashCode and equals
* [#1134](../../jackson-databind/issues/1134): Jackson 2.7 doesn't work with jdk6 due to use of `Collections.emptyIterator()`

### Changes, dataformats

#### [CBOR](../../jackson-dataformat-cbor)

* [#22](../../jackson-dataformat-cbor/issues/22): `CBORGenerator.copyCurrentStructure()` and `copyCurrentEvent()` do not copy tags

#### [Smile](../../jackson-dataformat-smile)
* [#34](../../jackson-dataformat-smile/issues/34): Deserialize error "Invalid type marker byte" for 'long' field names (57 characters or longer)

### Changes, datatypes

#### [Hibernate](../../jackson-datatype-hibernate)

* [#86](../../jackson-datatype-hibernate/issues/86): NoClassDefFoundError: org/hibernate/bytecode/internal/javassist/FieldHandler with hibernate 5.1

#### [Joda](../../jackson-datatype-joda)

* [#81](../../jackson-datatype-joda/issues/81): Add key deserializers for `Duration` and `Period` classes
