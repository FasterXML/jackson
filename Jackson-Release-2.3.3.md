Patch version released on 10-Apr-2014. Following changes included.

Here are accumulated changes so far.

### Changes, core

#### [Core Databind](../../jackson-databind)

* [#420](../../jackson-databind/issues/420): Remove 'final' modifier from `BeanDeserializerBase.deserializeWithType`
* [#422](../../jackson-databind/issues/422): Allow use of "True" and "False" as aliases for booleans when coercing from JSON String
* [#423](../../jackson-databind/issues/423): Fix `CalendarSerializer` to work with custom format
* [#433](../../jackson-databind/issues/433): `ObjectMapper.valueToTree()` wraps `JsonSerializable` objects into a `POJONode`

### Changes, [JAX-RS](../../jackson-jaxrs-providers)

* [#41](../../jackson-jaxrs-providers/issues/41): Try to resolve problems with RESTeasy, missing `_configForWriting` override.

### Changes, data formats

#### [Avro](../../jackson-dataformat-avro)

* [#6] (../../jackson-dataformat-avro/issues/6): Nested Map type fields do not work
* [#7] (../../jackson-dataformat-avro/issues/7): Add `AvroGenerator.Feature.IGNORE_UNKWNOWN` to allow filtering of properties that are not defined in Schema, during output

#### [CSV](../../jackson-dataformat-csv)

* [#33](../../jackson-dataformat-csv/issues/33): CSV is written without column separators if first column is null
* Fix a minor problem with `CsvSchema` defaults: was setting default escape char to be same as default quote (i.e. double-quote)

#### [CBOR](../../jackson-dataformat-cbor)

* [#2](../../jackson-dataformat-cbor/issues/2): Negative Long values written as zero

#### [XML](../../jackson-dataformat-xml)

* [#101](../../jackson-dataformat-xml/issues/101): Invalid index error when deserializing unwrapped list element with multiple attributes

### Changes, data types

#### [Guava](../../jackson-datatype-guava)

* [#37](../../jackson-datatype-guava/issues/37): `Optional` not correctly deserialized from JSON null, if inside a Collection
* [#41](../../jackson-datatype-guava/issues/41): `Multimap` serializer does not honor `@JsonInclude(JsonInclude.Include.NON_EMPTY)`

#### [Hibernate](../../jackson-datatype-hibernate)

* [#47](../../jackson-datatype-hibernate/issues/47): Feature.USE_TRANSIENT_ANNOTATION does not work

#### [Joda](../../jackson-datatype-joda)

* [#32](../../jackson-datatype-joda/issues/32): Support use of `@JsonFormat(shape=...)` for timestamp/string choice

### Changes, other modules

#### [Afterburner](../../jackson-module-afterburner)

* [#28](../../jackson-module-afterburner/issues/28): JsonCreator deser with single-String-argument constructor fails

#### [JAXB Annotations](../../jackson-module-jaxb-annotations)

* [#27](../../jackson-module-jaxb-annotations/issues/27): Try to make `JaxbAnnotationIntrospector` work with Jackson XML module in determining whether property is output as attribute

#### [Mr Bean](../../jackson-module-mrbean)

* [#12](../../jackson-module-mrbean/issues/12): Fix problem with `Number` as Map value (and probably other types too)

#### [Scala](../../jackson-module-scala)

See [Scala module Issue Tracker](../../jackson-module-scala/issues/)
