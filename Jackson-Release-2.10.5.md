Patch version of [2.10](Jackson-Release-2.10), released on 21-Jul-2020.

It is likely the last full patch set for 2.10.x series.

Following fixes were included.

### Changes, core

#### [Streaming](../../jackson-core)

[#616](../../jackson-core/issues/616): Parsing JSON with `ALLOW_MISSING_VALUE` enabled results in endless stream of `VALUE_NULL` tokens

#### [Databind](../../jackson-databind)

* [#2787](../../jackson-databind/issues/2787): (partial fix): NPE after add mixin for enum

### Changes, data formats

#### [Avro](../jackson-dataformats-binary)

* [#211](../jackson-dataformats-binary/issues/211): Fix schema evolution involving maps of non-scalar

#### [CSV](../../jackson-dataformats-text)

* [#204](../../jackson-dataformats-text/issues/204): `CsvParser.Feature.ALLOW_TRAILING_COMMA` doesn't work with header columns

#### [Ion](../jackson-dataformats-binary)

* [#204](../jackson-dataformats-binary/issues/204): Add `IonFactory.getIonSystem()` accessor

#### [XML](../../jackson-dataformat-xml)

* [#395](../../jackson-dataformat-xml/issues/395): Namespace repairing generates xmlns definitions for xml: prefix (which is implicit)
* [#413](../../jackson-dataformat-xml/issues/413): Null String field serialization through ToXmlGenerator causes NullPointerException

#### [YAML](../../jackson-dataformats-text)

* [#146](../../jackson-dataformats-text/issues/146): Jackson can't handle underscores in numbers

### Changes, datatypes

#### [Collections](../../jackson-datatypes-collections)

* [#67](../../jackson-datatypes-collections/issues/67): (guava) Guava collection deserialization failure with `Nulls.AS_EMPTY`

#### [JSR-353](../../jackson-datatype-jsr353)

* [#18](../../jackson-datatype-jsr353/issues/18): Deserialization of `JsonObject` from `null` broken since 2.10.4

### Changes, jackson-jr

* [#73](../../jackson-jr/issues/73): Allow for reading `null` fields when reading simple objects

