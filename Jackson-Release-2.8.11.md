Patch version of [2.8](Jackson-Release-2.8), released on 24-Dec-2017 (merry Christmas!).

This release is mostly important for security fixes contained; mostly as follow up for work start (and included) in 2.8.9 and 2.8.10.
It is also very likely the last Full Release of Jackson for 2.8 branch. As usual, micro-patches for individual components are still possible for critical issues.

Following fixes are included.


### Changes, core

#### [Streaming](../../jackson-core)

* [#418](../../jackson-databind/issues/418): `ArrayIndexOutOfBoundsException` from UTF32Reader.read on invalid input

#### [Databind](../../jackson-databind)

* [#1604](../../jackson-databind/issues/1604): Nested type arguments doesn't work with polymorphic types
* [#1680](../../jackson-databind/issues/1680): Blacklist couple more types for deserialization
* [#1767](../../jackson-databind/issues/1767): Allow `DeserializationProblemHandler` to respond to primitive types
* [#1768](../../jackson-databind/issues/1768): Improve `TypeFactory.constructFromCanonical()` to work with `java.lang.reflect.Type.getTypeName()` format
* [#1804](../../jackson-databind/issues/1804): `ValueInstantiator.canInstantiate()` ignores `canCreateUsingArrayDelegate()`
* [#1807](../../jackson-databind/issues/1807): Jackson-databind caches plain map deserializer and use it even map has `@JsonDeserializer`
* [#1855](../../jackson-databind/issues/1855): Blacklist for more serialization gadgets (dbcp/tomcat, spring)

### Changes, data formats

#### [Binary formats](../../jackson-dataformats-binary/)

* [#106](../../jackson-dataformats-binary/issues/106): (protobuf) fix calling _skipUnknownValue() twice
* [#108](../../jackson-dataformats-binary/issues/108): (protobuf) fix NPE in skip unknown nested key
* [#126](../../jackson-dataformats-binary/issues/126): (protobuf) always call checkEnd() when skip unknown field

### Changes, other modules

#### [Afterburner](../../jackson-modules-base/tree/master/afterburner)

* [#33](../../jackson-modules-base/issues): `@JsonSerialize` with `nullUsing` option not working for `String` properties

#### [JAXB Annotations](../../jackson-modules-base/tree/master/jaxb)

* [#31](../../jackson-modules-base/issues/31): `@JsonAppend` causes `IllegalStateException` `Unsupported annotated member`
  with `JaxbAnnotationModule`
