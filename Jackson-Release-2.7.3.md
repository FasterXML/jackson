Patch version of [2.7](Jackson-Release-2.7), being released on March 15, 2016

### Changes, core

#### [Databind](../../jackson-databind)

* [#1125](../../jackson-databind/issues/1125): Problem with polymorphic types, losing properties from base type(s)
* [#1150](../../jackson-databind/issues/1150): Problem with Object id handling, explicit `null` token
* [#1154](../../jackson-databind/issues/1154): `@JsonFormat.pattern` on dates is now ignored if shape is not explicitely provided
* [#1161](../../jackson-databind/issues/1161): `DeserializationFeature.READ_ENUMS_USING_TO_STRING` not dynamically changeable with 2.7

### Changes, data formats

#### [Avro](../../jackson-dataformat-avro)

* [#35](../../jackson-dataformat-avro/issues/35): Not able to serialize avro generated object having schema$ object

#### [YAML](../../jackson-dataformat-yaml)

* [#59](../../jackson-dataformat-yaml/issues/59): Avoid exposing SnakeYAML exception types (wrap)

### Changes, datatypes

#### [Java 8 Date](../../jackson-datatype-jsr310)

* [#63](../../jackson-datatype-jsr310/issues/63): Should not leak `DateTimeException`s to caller

### Changes, other modules

#### [Jackson Jr](../../jackson-jr)

* [#37](../../jackson-jr/issues/37): Update Jackson Jr Retrofit 2 Converter for Retrofit 2.0.0
* [#38](../../jackson-jr/issues/38): `PRETTY_PRINT_OUTPUT` with composer doesn't work

#### [Java 8 Parameter Names](../../jackson-module-parameter-names)

* [#32](../../jackson-module-parameter-names/issues/32): `@JsonProperty` annotations on enums are not respected if parameter names module is registered