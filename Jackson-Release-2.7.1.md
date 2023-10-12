Patch version of [2.7](Jackson-Release-2.7), released February 2nd, 2016.

### Changes, core

#### [Databind](../../jackson-databind)

* [#1079](../../jackson-databind/issues/1079): Add back `TypeFactory.constructType(Type, Class)` as "deprecated" in 2.7.1
* [#1083](../../jackson-databind/issues/1083): Field in base class is not recognized, when using `@JsonType.defaultImpl`
* [#1095](../../jackson-databind/issues/1095): Prevent coercion of `int` from empty String to `null` if `DeserializationFeature .FAIL_ON_NULL_FOR_PRIMITIVES` is `true`
* [#1102](../../jackson-databind/issues/1102): Handling of deprecated `SimpleType.construct()` too minimalistic
* [#1109](../../jackson-databind/issues/1109): `@JsonFormat` is ignored by `DateSerializer` unless either a custom pattern or a timezone are specified

### Changes, dataformats

#### [CBOR](../../jackson-dataformat-cbor)

* [#19](../../jackson-dataformat-cbor/issues/19): Fix reported location after non-stream input has been parsed.

### Changes, datatypes

#### [Guava](../../jackson-datatype-guava)

* [#87](../../jackson-datatype-guava/issues/87): OSGi import missing for `com.fasterxml.jackson.annotation.JsonInclude$Value`

#### [Java 8 Dates](../../jackson-datatype-jsr310)

* [#56](../../jackson-datatype-jsr310/issues/56): Handle JSON serialized Dates from JavaScript in LocalDateDeserializer (follow up for earlier `#28`)
* [#57](../../jackson-datatype-jsr310/issues/57): Add support for `@JsonFormat` for `MonthDay`

### Changes, other modules

#### [Afterburner](../../jackson-module-afterburner)

* [#63](../../jackson-module-afterburner/issues/63): Revert back expansion of `NON_EMPTY` handling
