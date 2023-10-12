Patch version of [2.9](Jackson-Release-2.9), released 09-Dec-2017.

Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core)

* [#418](../../jackson-databind/issues/418): `ArrayIndexOutOfBoundsException` from UTF32Reader.read on invalid input

#### [Databind](../../jackson-databind)

* [#1604](../../jackson-databind/issues/1604): Nested type arguments doesn't work with polymorphic types
* [#1794](../../jackson-databind/issues/1794): `StackTraceElementDeserializer` not working if field visibility changed
* [#1799](../../jackson-databind/issues/1799): Allow creation of custom sub-types of `NullNode`, `BooleanNode`, `MissingNode`
* [#1804](../../jackson-databind/issues/1804): `ValueInstantiator.canInstantiate()` ignores `canCreateUsingArrayDelegate()`
* [#1807](../../jackson-databind/issues/1807): Jackson-databind caches plain map deserializer and use it even map has `@JsonDeserializer`
* [#1823](../../jackson-databind/issues/1823): ClassNameIdResolver doesn't handle resolve Collections$SingletonMap, Collections$SingletonSet
* [#1831](../../jackson-databind/issues/1831): `ObjectReader.readValue(JsonNode)` does not work correctly with polymorphic types, value to update
* [#1835](../../jackson-databind/issues/1835): `ValueInjector` break from 2.8.x to 2.9.x
* [#1842](../../jackson-databind/issues/1842): `null` String for `Exception`s deserialized as String "null" instead of `null`
* [#1843](../../jackson-databind/issues/1843): Include name of unsettable property in exception from `SetterlessProperty.set()`
* [#1844](../../jackson-databind/issues/1844): Map "deep" merge only adds new items, but not override existing values

### Changes, dataformats

#### [Binary formats](../../jackson-dataformats-binary/)

* [#106](../../jackson-dataformats-binary/issues/106): (protobuf) fix calling _skipUnknownValue() twice
* [#108](../../jackson-dataformats-binary/issues/108): (protobuf) fix NPE in skip unknown nested key
* [#114](../../jackson-dataformats-binary/issues/114): (cbor) copyStructure(): avoid duplicate tags when copying tagged binary
* [#116](../../jackson-dataformats-binary/issues/116): (protobuf) Should skip the positive byte which is the last byte of an varint
* [#124](../../jackson-dataformats-binary/issues/124): (cbor) Invalid value returned for negative int32 where the absolute value is > 2^31 - 1
* [#126](../../jackson-dataformats-binary/issues/126): (protobuf) always call checkEnd() when skip unknown field

#### [Textual formats](../../jackson-dataformats-text/)

* [#39](../../jackson-dataformats-text/issues/39): (yaml) Binary data not recognized by YAML parser
* [#42](../../jackson-dataformats-text/issues/42): (csv) Add support for escaping double quotes with the configured escape character
* [#53](../../jackson-dataformats-text/issues/53): (yaml) Binary values written without type tag

### Changes, datatypes

#### [Java 8](../../jackson-modules-java8)

* [#46](../../jackson-modules-java8/issues/46): (datetime) Double array serialization of `LocalDate` stored as an object with wrapper object typing enabled

### Changes, other modules

#### [Afterburner](../../jackson-modules-base/tree/master/afterburner)

* [#33](../../jackson-modules-base/issues/33): `@JsonSerialize` with `nullUsing` option not working for `String` properties

#### [JAXB Annotations](../../jackson-modules-base/tree/master/jaxb)

* [#31](../../jackson-modules-base/issues/31): `@JsonAppend` causes `IllegalStateException` `Unsupported annotated member`
  with `JaxbAnnotationModule`
* [#32](../../jackson-modules-base/issues/32): Fix introspector chaining in `JaxbAnnotationIntrospector.hasRequiredMarker()`
