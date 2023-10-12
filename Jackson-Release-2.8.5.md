Patch version of [2.8](Jackson-Release-2.8), released Nov 14, 2016.

Following fixes were included.

### Changes, core

#### [Databind](../../jackson-databind/)

* [#1417](../../jackson-databind/issues/1417): Further issues with `@JsonInclude` with `NON_DEFAULT`
* [#1421](../../jackson-databind/issues/1421): `ACCEPT_SINGLE_VALUE_AS_ARRAY` partially broken in 2.7.x, 2.8.x
* [#1429](../../jackson-databind/issues/1429): `StdKeyDeserializer` can erroneously use a static factory method with more than one argument
* [#1432](../../jackson-databind/issues/1432): Off by 1 bug in `PropertyValueBuffer`
* [#1438](../../jackson-databind/issues/1438): `ACCEPT_CASE_INSENSITIVE_PROPERTIES` is not respected for creator properties
* [#1439](../../jackson-databind/issues/1439): NPE when using with filter id, serializing `java.util.Map` types
* [#1441](../../jackson-databind/issues/1441): Failure with custom Enum key deserializer, polymorphic types
* [#1445](../../jackson-databind/issues/1445): Map key `deserializerModifiers()` ignored

### Changes, data formats

#### [CBOR](../../jackson-dataformats-binary)

* [#31](../../jackson-dataformats-binary/issues/31): Exception serializing `double[][]`

#### [XML](../../jackson-dataformat-xml)

* [#213](../../jackson-dataformat-xml/issues/213) `XmlSerializerProvider` does not use `withRootName` config for JSON `null`

### Changes, datatypes

#### [Guava](../../jackson-datatypes-collections)

* [#6](../../jackson-datatypes-collections/issues/6): (further fixes to earlier incomplete fix) `Multimap` does not support `DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY`

### Changes, [JAX-RS](../../jackson-jaxrs-providers)

* [#91](../../jackson-jaxrs-providers/issues/91): Implemented dynamic selection of `NoContentException` to try to
support JAX-RS 1.x

### Changes, other modules

#### [JSON Schema](../../jackson-module-jsonSchema)

* [#117](../../jackson-module-jsonSchema/issues/117): Deserialisation of enums does not respect ordering
