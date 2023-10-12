Patch version of [2.7](Jackson-Release-2.7), released on 26-Sep-2016.

Following fixes are included.

### General notes:

As usual, this patch contains multiple fixes. But while all are important for some user or use case, some are considered more general important:

* Fix for [Immutables][https://github.com/immutables/immutables) library, regarding use of generated single-argument `@JsonCreator` annotated Delegating Creator. See `databind#1383` below for details
* Fix for XML processing defaults, regarding DTD processing: See `dataformat-xml#211` below for details

Because of these fixes, an upgrade from earlier 2.7.x patch versions is strongly recommended.

### Changes, core

#### [Streaming](../../jackson-core)

* [#317](../../jackson-core/issues/317): `ArrayIndexOutOfBoundsException`: 200 on floating point number with exactly 200-length decimal part

#### [Databind](../../jackson-databind)

* [#877](../../jackson-databind/issues/877): `@JsonIgnoreProperties`: ignoring the "cause" property of `Throwable` on GAE
* [#1359](../../jackson-databind/issues/1359): Improve `JsonNode` deserializer to create `FloatNode` if parser supports
* [#1362](../../jackson-databind/issues/1362): `ObjectReader.readValues()` ignores offset and length when reading an array
* [#1363](../../jackson-databind/issues/1363): The static field `ClassUtil.sCached` can cause a class loader leak
* [#1368](../../jackson-databind/issues/1368): Problem serializing `JsonMappingException` due to addition of non-ignored `processor` property (added in 2.7)
* [#1383](../../jackson-databind/issues/1383): Problem with `@JsonCreator` with 1-arg factory-method, implicit param names

### Changes, data formats

#### [XML](../../jackson-dataformat-xml)

* [#210](../../jackson-dataformat-xml/issues/210): `ToXmlGenerator` WRITE_BIGDECIMAL_AS_PLAIN is used the wrong way round
* [#211](../../jackson-dataformat-xml/issues/211): Disable `SUPPORT_DTD` for `XMLInputFactory` unless explicitly overridden

#### [YAML](../../jackson-dataformat-yaml)

* [#70](../../jackson-dataformat-yaml/issues/70): `UTF8Reader` is unsafe if a Jackson-annotated class itself deserializes a Jackson-annotated class
