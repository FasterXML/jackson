Patch version of [2.14](Jackson-Release-2.14), released on November 21st, 2022.

Following fixes are included in this patch release.

### Changes, core

#### [Databind](../../jackson-databind)

* [#3655](../../jackson-databind/issues/3655): `Enum` values can not be read from single-element array even with `DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS`
* [#3665](../../jackson-databind/issues/3665): `ObjectMapper` default heap consumption increased significantly from 2.13.x to 2.14.0

### Changes, data formats

#### CSV

* [#352](../../jackson-dataformats-text/issues/352): Disabling `CsvParser.Feature.FAIL_ON_MISSING_HEADER_COLUMNS` has no effect

#### Smile

* [#342](../../jackson-dataformats-binary/issues/342): Possible performance improvement on jdk9+ for Smile decoding

### Changes, datatypes

#### [Jakarta-JSONP / JSR-353](../../jackson-datatypes-misc)

* [#27](../../jackson-datatypes-misc/issues/27): Deserializing a JSON Merge Patch fails when the input is not a JSON object

