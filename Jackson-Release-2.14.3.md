Patch version of [2.14](Jackson-Release-2.14), released on March 5, 2023.

Following fixes are included in this patch release.

### Changes, core

#### [Streaming](../../jackson-core)

* [#909](../../jackson-core/issues/909): Revert schubfach changes in #854
* [#912](../../jackson-core/issues/912): Optional padding Base64Variant still throws exception on missing padding character
* [#967](../../jackson-core/issues/967): Address performance issue with `BigDecimalParser`
* [#990](../../jackson-core/pull/990): Backport removal of BigDecimal to BigInt conversion
* [#1004](../../jackson-core/pull/1004): FastDoubleParser license
* [#1012](../../jackson-core/pull/1012): Got `NegativeArraySizeException` when calling `writeValueAsString()`

#### [Databind](../../jackson-databind)

* [#3784](../../jackson-databind/issues/3784): `PrimitiveArrayDeserializers$ByteDeser.deserialize` ignores `DeserializationProblemHandler` for invalid Base64 content
* [#3837](../../jackson-databind/pull/3837): Set transformer factory attributes to improve protection against XXE

### Changes, data formats

#### Binary Formats (Avro, CBOR, Ion, Protobuf, Smile)

* [#354](../../jackson-dataformats-binary/issues/354): Some artifacts missing `NOTICE`, `LICENSE` files

#### Textual Formats (CSV, Properties, TOML, YAML)

* [#378](../../jackson-dataformats-text/issues/378): Some artifacts missing `NOTICE`, `LICENSE` files

#### CBOR

* [#366](../../jackson-dataformats-binary/issues/366): `CBORGenerator.writeRawUTF8String()` seems to ignore offset

### Changes, datatypes

#### Guava

* [#92](../../jackson-datatypes-collections/issues/92): `@JsonDeserialize.contentConverter` does not work for non-builtin collections
* [#104](../../jackson-datatypes-collections/issues/104): `ArrayListMultimapDeserializer` does not support multimaps inside another object as a property

### Changes, Other modules

#### JAXB

* [#199](../../jackson-modules-base/issues/199): jaxb and jakarta-xmlbind put module-info in versions/11
* Fix Gradle Module Metadata for Afterburner, Blackbird

### Changes, other

#### [Jackson-jr](../../jackson-jr)

* [#102](../../jackson-jr/issues/102): Missing module-info dependency from `jackson-jr-annotation-support`
* [#103](../../jackson-jr/issues/103): Some artifacts missing `NOTICE`, `LICENSE` files
