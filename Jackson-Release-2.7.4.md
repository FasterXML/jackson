Patch version of [2.7](Jackson-Release-2.7), released on April 29, 2016.
Following fixes were included.

### Changes, core

#### [Streaming](../../jackson-core)

* [#209](../../jackson-core/issues/209): Make use of `_allowMultipleMatches` in `FilteringParserDelegate`

#### [Databind](../../jackson-databind)

* [#1178](../../jackson-databind/issues/1178): `@JsonSerialize(contentAs=superType)` behavior disallowed in 2.7
* [#1186](../../jackson-databind/issues/1186): `SimpleAbstractTypeResolver` breaks generic parameters
* [#1189](../../jackson-databind/issues/1189): Converter called twice results in ClassCastException
* [#1191](../../jackson-databind/issues/1191): Non-matching quotes used in error message for date parsing
* [#1194](../../jackson-databind/issues/1194): Incorrect signature for generic type via `JavaType.getGenericSignature
* [#1195](../../jackson-databind/issues/1195): `JsonMappingException `not Serializable due to 2.7 reference to source (parser)
* [#1198](../../jackson-databind/issues/1198): Problem with `@JsonTypeInfo.As.EXTERNAL_PROPERTY`, `defaultImpl`, missing type id, NPE

### Changes, dataformats

#### [Avro](../../jackson-dataformat-avro)

* [#39](../../jackson-dataformat-avro/issues/39): Byte arrays are represented as strings in generated avro schema

#### [XML](../../jackson-dataformat-xml)

* [#178](../../jackson-dataformat-xml/issues/178): Problem with polymorphic serialization, inclusion type of
`As.WRAPPER_OBJECT`, extra tag
* [#190](../../jackson-dataformat-xml/issues/190): Ensure that defaults for `XMLInputFactory` have expansion of external parsed general entities disabled
* [#191](../../jackson-dataformat-xml/issues/191): Strange behaviour of an empty item (but with whitespace between start/end tags) in List

### Changes, datatypes

#### [JDK8](../../jackson-datatype-jdk8)

* [#31](../../jackson-datatype-jdk8/issues/31): Support `@JsonSerialize(contentUsing=)` and `@JsonDeserialize(contentUsing=)` for `Optional`
* [#34](../../jackson-datatype-jdk8/issues/34): Fix OptionalSerializer.isEmpty() from an incorrect class cast exception

#### [Java 8 Date](../../jackson-datatype-jsr310)

* [#68](../../jackson-datatype-jsr310/issues/68): Handle JSON serialized Dates from JavaScript in `LocalDateTimeDeserializer`
* [#76](../../jackson-datatype-jsr310/issues/76): Use `InvalidFormatException` for deserialization parse failures

### Changes, [JAX-RS](../../jackson-jaxrs-providers)

* [#80](../../jackson-jaxrs-providers/issues/80): Non-JSON providers don't support custom MIME types with extensions

### Changes, other modules

#### [Afterburner](../../jackson-modules-base)

* [#4](../../jackson-modules-base/issues/4): Serialization with Afterburner causes Java VerifyError for generic with String as type

#### [Mr Bean](../../jackson-modules-base)

* [#8](../../jackson-modules-base/issues/8): Problem with `Optional<T>`, `AtomicReference<T>` valued properties

#### [Parameter Names (Java 8)](../../jackson-module-parameter-names)

* [#33](../../jackson-module-parameter-names/issues/33): Calls to Executable.getParameters() should guard against MalformedParametersException

#### [Scala module](../../jackson-module-scala)

* [#252](../../jackson-module-scala/issues/252): Upgrade Option support to use ReferenceType

