Patch version of [2.7](Jackson-Release-2.7), released on 27-Aug-2016,

Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core)

* [#307](../../jackson-core/issues/307): JsonGenerationException: Split surrogate on writeRaw() input thrown for input of certain size
* [#315](../../jackson-core/issues/315): `OutOfMemoryError` when writing `BigDecimal`

#### [Databind](../../jackson-databind/)

* [#1322](../../jackson-databind/1322): `EnumMap` keys not using enum's `@JsonProperty` values unlike Enum values
* [#1332](../../jackson-databind/1332): `ArrayIndexOutOfBoundException` for enum by index deser
* [#1344](../../jackson-databind/1344): Deserializing locale assumes JDK separator (underscore), does not accept RFC specified (hyphen)

### Changes, data formats

#### [CSV](../../jackson-dataformat-csv)

* [#128](../../jackson-dataformat-csv/issues/128): Write out headers even if no data rows written
* [#132](../../jackson-dataformat-csv/issues/132): Invalid UTF8-character in non-UTF8 file is detected too early, so parsing can not be continued

#### Protobuf

* [#27](https://github.com/FasterXML/jackson-dataformats-binary/pull/27): Fixed long deserialization problem for longs of ~13digit length

#### [XML](../../jackson-dataformat-xml)

* [#204](../../jackson-dataformat-xml/issues/204): `FromXMLParser.nextTextValue()` incorrect for attributes

#### [YAML](../../jackson-dataformat-yaml)

* [#69](../../jackson-dataformat-yaml/issues/69): Problem to parse time values as string
