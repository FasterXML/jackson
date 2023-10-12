Patch version of [2.9](Jackson-Release-2.9), released on March 28, 2018.

Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core)

#### [Databind](../../jackson-databind)

* [#1911](../../jackson-databind/issues/1911): Allow serialization of `BigDecimal` as String, using
`@JsonFormat(shape=Shape.String)`, config overrides
* [#1912](../../jackson-databind/issues/1912): `BeanDeserializerModifier.updateBuilder()` not work to set custom deserializer on a property (since 2.9.0)
* [#1931](../../jackson-databind/issues/1931): Two more `c3p0` gadgets to exploit default typing issue
* [#1932](../../jackson-databind/issues/1932): `EnumMap` cannot deserialize with type inclusion as property
* [#1940](../../jackson-databind/issues/1940): `Float` values with integer value beyond `int` lose precision if
bound to `long`
* [#1941](../../jackson-databind/issues/1941): `TypeFactory.constructFromCanonical()` throws NPE for Unparameterized generic canonical strings
* [#1947](../../jackson-databind/issues/1947): `MapperFeature.AUTO_DETECT_XXX` do not work if all disabled
* [#1977](../../jackson-databind/issues/1977): Serializing `Iterator` with multiple sub-types fails after upgrading to 2.9.x
* [#1978](../../jackson-databind/issues/1978): Using `@JsonUnwrapped` annotation in builderdeserializer hangs in infinite loop

### Changes, dataformats

#### [Binary formats](../../jackson-dataformats-binary/)

* [#128](../../jackson-dataformats-binary/issues/128): (protobuf) Fix skip unknown WireType.FIXED_64BIT value bug
* [#129](../../jackson-dataformats-binary/issues/129): (cbor) Remove "final" modifier from `CBORParser`

#### [Textual formats](../../jackson-dataformats-text/)

* [#74](../../jackson-dataformats-text/issues/74): (properties) `JavaPropsMapper` issue deserializing multiple byte array properties

### Changes, datatypes

#### [Guava](../../jackson-datatypes-collections)

* [#27](../../jackson-datatypes-collections/27): Null value handling not supported for `Optional` within `Multimap`

### Changes, other modules

#### [Mr Bean](../../jackson-modules-base)

* [#42](../../jackson-modules-base/issues/42): NPE from MrBean when `get()` or `set()` is though as property

#### [Java 8 Date/Time](../../jackson-modules-java8)

* [#98](../../jackson-modules-java8/issues/98): `OffsetDateTime` with `@JsonFormat(without=...)` doesn't seem to work
