Patch version of [2.10](Jackson-Release-2.10), released May 3rd, 2020.

Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core)

[#605](../../jackson-core/issues/605): Handle case when system property access is restricted
[#609](../../jackson-core/issues/609): (partial fix) `FilteringGeneratorDelegate` does not handle `writeString(Reader, int)`

#### [Databind](../../jackson-databind)

[#2679](../../jackson-databind/issues/2679): `ObjectMapper.readValue("123", Void.TYPE)` throws "should never occur"

### Changes, data formats

#### [Properties](../../jackson-dataformats-text)

* [#179](../../jackson-dataformats-text/issues/179): `JavaPropsMapper` doesn't close the .properties file
properly after reading
* [#184](../../jackson-dataformats-text/issues/184): `jackson-databind` should not be optional/provided dependency

#### [Protobuf](../../jackson-dataformats-binary)

* [#202](../../jackson-dataformats-binary/issues/202): Parsing a protobuf message doesn't properly skip unknown fields

#### [XML](../../jackson-dataformat-xml)

* Upgrade Woodstox dependency to 6.2.0 (minor improvement to MSV shading)

#### [YAML](../../jackson-dataformats-text)

* [#182](../../jackson-dataformats-text/issues/182): Negative numbers not quoted correctly wrt `ALWAYS_QUOTE_NUMBERS_AS_STRINGS`
* [#187](../../jackson-dataformats-text/issues/187): Update to SnakeYAML to 1.26 (from 1.24) to address CVE-2017-18640

### Changes, datatypes

#### [Joda](../../jackson-datatype-joda)

* [#113](../../jackson-datatype-joda/issues/113): `ObjectMapper.setDefaultLeniency()` is causing `NullPointerException` in `JacksonJodaDateFormat`

#### [JSR-353](../../jackson-datatype-jsr353)

* [#16](../../jackson-datatype-jsr353/issues/16): Null being deserialized as null literal instead of JsonValue.NULL
