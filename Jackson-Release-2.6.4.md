Patch version of [2.6](Jackson-Release-2.6), released on December 7, 2015.

### Changes, core

#### [Databind](../../jackson-databind)

* [#984](../../jackson-databind/issues/984): JsonStreamContexts are not build the same way for write.. and convert methods
* [#989](../../jackson-databind/issues/989): Deserialization from "{}" to java.lang.Object causes "out of END_OBJECT token" error
* [#1003](../../jackson-databind/issues/1003): `JsonTypeInfo.As.EXTERNAL_PROPERTY` does not work with a Delegate
* [#1004](../../jackson-databind/issues/1004): Synthetic constructors confusing Jackson data binding
* [#1013](../../jackson-databind/issues/1013): `@JsonUnwrapped` is not treated as assuming `@JsonProperty("")`
* [#1031](../../jackson-databind/issues/1036): Problem with case-insensitive deserialization

### Changes, dataformats

#### [Avro](../../jackson-dataformat-avro)

* [#23](../../jackson-dataformat-avro/issues/23): `MapWriteContext.createChildObjectContext()` should call `_schema.getValueType()`
* [#26](../../jackson-dataformat-avro/issues/26): Should indicate type that was requested when complaining about Any not being supported
* [#27](../../jackson-dataformat-avro/issues/27): Support serialization of `BigDecimal` values

#### [CSV](../../jackson-dataformat-csv)

* [#90](../../jackson-dataformat-csv/issues/90): Unexpected output with arrays starting with a null/empty element
* [#98](../../jackson-dataformat-csv/issues/98): Escape char is not being escaped during serialization

#### [XML](../../jackson-dataformat-xml)

* [#171](../../jackson-dataformat-xml/issues/171): `@JacksonXmlRootElement` malfunction in multi-thread environment
* [#172](../../jackson-dataformat-xml/issues/172): XML INDENT_OUTPUT property fails to add newline/indent initial elements

### Changes, datatypes

#### [JDK8](../../jackson-datatype-jdk8)

* [#20](../../jackson-datatype-jdk8/issues/20): fails to serialize empty Optional with `@JsonUnwrapped` annotation
* [#21](../../jackson-datatype-jdk8/issues/21): Manifest file missing OSGi import for `com.fasterxml.jackson.databind.introspect`
* [#23](../../jackson-datatype-jdk8/issues/23): Fix an NPE for `Optional<Boolean>` when coercing from empty JSON String
* [#25](../../jackson-datatype-jdk8/issues/25): `OptionalDoubleSerializer` does not override `acceptJsonFormatVisitor`
* [#26](../../jackson-datatype-jdk8/issues/26): `@JsonUnwrapped` on Optional value does not propagate prefix to schema

#### [Java 8 Date/Time](../../jackson-datatype-jsr310)

* [#50](../../jackson-datatype-jsr310/issues/50): `Instant` schema representation is incorrect for timestamps serialization

### Changes, other

#### [Jackson jr](../../jackson-jr)

* [#27](../../jackson-jr/issues/27): `JSON.Feature.READ_JSON_ARRAYS_AS_JAVA_ARRAYS` does not work
