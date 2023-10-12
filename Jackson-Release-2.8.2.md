A patch version of [2.8](Jackson-Release-2.8), released on August 30th, 2016.

This release includes all fixes up to [2.7.7](Jackson-Release-2.7.7) from earlier branches,
as well as following:

### Changes, core

#### [Databind](../../jackson-databind/)

* [#1315](../../jackson-databind/issues/1315): Binding numeric values can BigDecimal lose precision
* [#1327](../../jackson-databind/issues/1327): Class-level `@JsonInclude(JsonInclude.Include.NON_EMPTY)` is ignored
* [#1335](../../jackson-databind/issues/1335): Unconditionally call `TypeIdResolver.getDescForKnownTypeIds()`

### Changes, dataformats

#### [Binary dataformats](../../jackson-dataformats-binary)

* [#27](../../jackson-dataformats-binary/issues/27): (protobuf) Fixed long deserialization problem for `long`s of ~13digit length

#### [YAML](../../jackson-dataformats-yaml)

* [#65](../../jackson-dataformats-yaml/issues/65): Feature.MINIMIZE_QUOTES needs to write numbers as strings with quotes

### Changes, data types

#### [Hibernate](../../jackson-datatype-hibernate)

* [#96](../../jackson-datatype-hibernate/issues/96): Improve `SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS` feature
