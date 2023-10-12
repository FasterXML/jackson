Patch version of [2.6](Jackson-Release-2.6), released April 5th, 2016.

This is planned to be the last full 2.6 release of all components: further releases, if any, will be micro-releases (like next hypothetical one, `2.6.6.1`) of specific components with issues.

### Changes, core

#### [Streaming](../../jackson-core)

* [#248](../../jackson-core/issues/248): VersionUtil.versionFor() unexpectedly return null instead of Version.unknownVersion()

#### [Databind](../../jackson-databind)

* [#1088](../../jackson-databind/1088): NPE possibility in `SimpleMixinResolver`
* [#1099](../../jackson-databind/1099): Fix custom comparator container node traversal
* [#1108](../../jackson-databind/1108): Jackson not continue to parse after `DeserializationFeature.FAIL_ON_INVALID_SUBTYPE` error
* [#1112](../../jackson-databind/1112): Detailed error message from custom key deserializer is discarded
* [#1120](../../jackson-databind/1120): String value omitted from weirdStringException
* [#1123](../../jackson-databind/1123): Serializing and Deserializing Locale.ROOT

### Changes, dataformats

#### [Avro](../../jackson-dataformat-avro)

* [#41](../../jackson-dataformat-avro/issues/41): Problems with type `Number`, with schema

#### [CBOR](../../jackson-dataformat-cbor)

* [#18](../../jackson-dataformat-cbor/issues/18): Correct parsing of zero length byte strings

#### [Smile](../../jackson-dataformat-smile)

* [#34](../../jackson-dataformat-smile/issues/34): Deserialize error "Invalid type marker byte" for 'long' field names (57 characters or longer)

### Changes, data types

#### [Joda](../../jackson-datatype-joda)

* [#82](../../jackson-datatype-joda/issues/82): Can't deserialize a serialized DateTimeZone with default typing

#### [Java 8 Date/Time](../../jackson-datatype-jsr310)

* [#16](../../jackson-datatype-jsr310/issues/16): Instant is serialized as String by some dataformats/libs but can't be deserialized (unless manually converted to float)
* [#69](../../jackson-datatype-jsr310/issues/69): Serialization of `Instant` seems to throw exceptions when when `@JsonFormat` is used

### Changes, other

#### [Jackson-jr](../../jackson-jr)

* [#40](../../jackson-jr/issues/40): Cannot read empty or singleton arrays with JSON.arrayOfFrom
