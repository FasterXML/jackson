Patch version of [2.10](Jackson-Release-2.10), released on 05-Jan-2020.

Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core)

* [#580](../../jackson-core/issues/580): FilteringGeneratorDelegate writeRawValue delegate to `writeRaw()` instead of `writeRawValue()`
* [#582](../../jackson-core/issues/582): `FilteringGeneratorDelegate` bug when filtering arrays (in 2.10.1)

#### [Databind](../../jackson-databind)

* [#2101](../../jackson-databind/issues/2101): `FAIL_ON_NULL_FOR_PRIMITIVES` failure does not indicate field name in exception message
* [#2544](../../jackson-databind/issues/2544): `java.lang.NoClassDefFoundError` Thrown for compact profile1
* [#2553](../../jackson-databind/issues/2553): JsonDeserialize(contentAs=...) broken with raw collections
* [#2556](../../jackson-databind/issues/2556): Contention in `TypeNameIdResolver.idFromClass()`
* [#2560](../../jackson-databind/issues/2560): Check `WRAP_EXCEPTIONS` in `CollectionDeserializer.handleNonArray()`
* [#2564](../../jackson-databind/issues/2564): Fix `IllegalArgumentException` on empty input collection for `ArrayBlockingQueue`
* [#2566](../../jackson-databind/issues/2566): `MissingNode.toString()` returns `null` (4 character token) instead of empty string
* [#2567](../../jackson-databind/issues/2567): Incorrect target type for arrays when providing nulls and nulls are disabled
* [#2573](../../jackson-databind/issues/2573): Problem with `JsonInclude` config overrides for `java.util.Map`
* [#2576](../../jackson-databind/issues/2576): Fail to serialize `Enum` instance which includes a method override as POJO (shape = Shape.OBJECT)

### Changes, data formats

#### [CSV](../../jackson-dataformats-text)

* [#166](../../jackson-dataformats-text/issues/166): Incorrect `JsonParseException` Message for missing separator char

#### [Ion](../../jackson-dataformats-binary)

* [#189](../../jackson-dataformats-binary/issues/189): `IonObjectMapper` close()s the provided `IonWriter` unnecessarily

#### [XML](../../jackson-dataformat-xml)

* [#366](../../jackson-dataformat-xml/issues/366): XML containing xsi:nil is improperly parsed
* [#378](../../jackson-dataformat-xml/issues/378): Jackson 2.10.x fails to deserialize xsi:nil with multiple child elements

#### [YAML](../../jackson-dataformats-text)

* [#163](../../jackson-dataformats-text/issues/163): `SequenceWriter` does not create multiple docs in a single yaml file

### Changes, JVM Languages

#### [Kotlin](../../jackson-module-kotlin)

* [#270](../../jackson-module-kotlin/issues/270): 2.10.1 seems to output JSON field where name of function matches name of private field
* [#279](../../jackson-module-kotlin/issues/279): 2.10 introduces another binary compatibility issue in KotlinModule constructor 

### Changes, other

#### [JAX-RS](../../jackson-jaxrs-providers)

* [#121](../../jackson-jaxrs-providers/issues/121): Allow multiple implementations of ws.rs

#### [Jackson-jr](../../jackson-jr)

* [#71](../../jackson-jr/issues/71): Jackson-jr 2.10 accidentally uses `UncheckedIOException` only available on JDK 8
