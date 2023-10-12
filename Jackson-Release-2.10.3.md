Patch version of [2.10](Jackson-Release-2.10), released on 03-Mar-2020.

Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core)

* [#592](../../jackson-core/issues/592): DataFormatMatcher#getMatchedFormatName throws NPE when no match exists
* [#603](../../jackson-core/issues/603): 'JsonParser.getCurrentLocation()` byte/char offset update incorrectly for big payloads

#### [Databind](../../jackson-databind)

* [#2482](../../jackson-databind/issues/2482): `JSONMappingException` `Location` column number is one line Behind the actual location
* [#2599](../../jackson-databind/issues/2599): NoClassDefFoundError at DeserializationContext.<init> on Android 4.1.2 and Jackson 2.10.0
* [#2602](../../jackson-databind/issues/2602): ByteBufferSerializer produces unexpected results with a duplicated ByteBuffer and a position > 0
* [#2605](../../jackson-databind/issues/2605): Failure to deserialize polymorphic subtypes of base type `Enum`
* [#2610](../../jackson-databind/issues/2610): `EXTERNAL_PROPERTY` doesn't work with `@JsonIgnoreProperties`

### Changes, [JAX-RS](../../jackson-jaxrs-providers)

* [#120](../../jackson-jaxrs-providers/issues/120): Incorrect export of `com.fasterxml.jackson.jaxrs.json` for JSON provider

### Changes, JVM Languages

#### [Scala](../../jackson-module-scala)

* [#218](../../../../jackson-module-scala/218): Serialization of case class with overridden attributes not working



