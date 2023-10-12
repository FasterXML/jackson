Patch version of [2.8](Jackson-Release-2.8), released Jan 12, 2017.

* with exception of Scala module, which is to follow at a later point (apologies for delay)

Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core)

* [#322](../../jackson-core/issues/322): Trim tokens in error messages to 256 byte to prevent attacks
* [#335](../../jackson-core/issues/335): Missing exception for invalid last character of base64 string to decode using `Base64Variant.decode()`

#### [Databind](../../jackson-databind/)

* [#349](../../jackson-databind/issues/349): @JsonAnySetter with @JsonUnwrapped: deserialization fails with arrays
* [#1388](../../jackson-databind/issues/1388): `@JsonIdentityInfo`: id has to be the first key in deserialization when deserializing with `@JsonCreator`
* [#1425](../../jackson-databind/issues/1425): `JsonNode.binaryValue()` ignores illegal character if it's the last one
* [#1453](../../jackson-databind/issues/1453): `UntypedObjectDeserializer` does not retain `float` type (over `double`)
* [#1456](../../jackson-databind/issues/1456): `TypeFactory` type resolution broken in 2.7 for generic types when using `constructType` with context
* [#1476](../../jackson-databind/issues/1476): Wrong constructor picked up when deserializing object
* [#1493](../../jackson-databind/issues/1493): `ACCEPT_CASE_INSENSITIVE_PROPERTIES` fails with `@JsonUnwrapped`

### Changes, dataformats

#### [YAML](../../jackson-dataformat-yaml)

* [#80](../../jackson-dataformat-yaml/issues/80): Fix UTF8Writer when used in same thread

### Changes, datatypes

#### [Hibernate](../../jackson-datatype-hibernate)

* [#92](../../jackson-datatype-hibernate/issues/92): Add support for Hibernate 5.2
