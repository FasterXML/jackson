Patch version of [2.10](Jackson-Release-2.10), released on 2019-11-09.

Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core)

* [#455](../../jackson-core/issues/455): Jackson reports wrong locations for JsonEOFException
* [#567](../../jackson-core/issues/567): Add `uses` for `ObjectCodec` in module-info

#### [Databind](../../jackson-databind)

* [#2457](../../jackson-databind/issues/2457): Extended enum values are not handled as enums when used as Map keys
* [#2473](../../jackson-databind/issues/2473): Array index missing in path of `JsonMappingException` for `Collection<String>`, with custom deserializer
* [#2475](../../jackson-databind/issues/2475): `StringCollectionSerializer` calls `JsonGenerator.setCurrentValue(value)`, which messes up current value for sibling properties
* [#2485](../../jackson-databind/issues/2485): Add `uses` for `Module` in module-info
* [#2513](../../jackson-databind/issues/2513): BigDecimalAsStringSerializer in NumberSerializer throws IllegalStateException in 2.10
* [#2519](../../jackson-databind/issues/2519): Serializing `BigDecimal` values inside containers ignores shape override
* [#2520](../../jackson-databind/issues/2520): Sub-optimal exception message when failing to deserialize non-static inner classes
* [#2529](../../jackson-databind/issues/2529): Add tests to ensure `EnumSet` and `EnumMap` work correctly with "null-as-empty"
* [#2534](../../jackson-databind/issues/2534): Add `BasicPolymorphicTypeValidator.Builder.allowIfSubTypeIsArray()`
* [#2535](../../jackson-databind/issues/2535): Allow String-to-byte[] coercion for String-value collections

### Changes, data formats

#### [CSV](../../jackson-dataformats-text)

* [#15](../../jackson-dataformats-text/issues/15): Add a `CsvParser.Feature.SKIP_EMPTY_LINES` to allow skipping empty rows

### Changes, datatypes

#### [Java 8 date/time](../../jackson-modules-java8)

* [#127](../../jackson-modules-java8/issues/127): ZonedDateTime in map keys ignores option to write Zone IDs

#### [Joda](../../jackson-datatype-joda)

[#108](../../jackson-datatype-joda/issues/108): `JodaDateSerializer` Discards Shape Override Preference

### Changes, other JVM languages

#### [Kotlin](../../jackson-module-kotlin)

* [#80](../../jackson-module-kotlin/issues/80): Boolean property name starting with 'is' not serialized/deserialized properly
* [#130](../../jackson-module-kotlin/issues/130): Using Kotlin Default Parameter Values when JSON value is null and Kotlin parameter type is Non-Nullable
* [#176](../../jackson-module-kotlin/issues/176): Version 2.9.7 breaks compatibility with Android minSdk '<' 24
* [#225](../../jackson-module-kotlin/issues/225): Don't instantiate new instances of Kotlin singleton objects
