Patch version of [2.12](Jackson-Release-2.12) was released on March 3, 2021.

Following fixes are included in this patch release.

### Changes, core

#### [Streaming](../../jackson-core)

No changes since 2.12.1

#### [Databind](../../jackson-databind)

* [#3008](../../jackson-databind/issues/3008): String property deserializes null as "null" for `JsonTypeInfo.As.EXTERNAL_PROPERTY`
* [#3022](../../jackson-databind/issues/3022): Property ignorals cause `BeanDeserializer `to forget how to read from arrays (not copying `_arrayDelegateDeserializer`)
* [#3025](../../jackson-databind/issues/3025): `UntypedObjectDeserializer` mixes multiple unwrapped collections
* [#3038](../../jackson-databind/issues/3038): Two cases of incorrect error reporting about `DeserializationFeature`
* [#3045](../../jackson-databind/issues/3045): Bug in polymorphic deserialization with `@JsonCreator`, `@JsonAnySetter`,
`JsonTypeInfo.As.EXTERNAL_PROPERTY`
* [#3055](../../jackson-databind/issues/3055): Polymorphic subtype deduction ignores `defaultImpl` attribute
* [#3056](../../jackson-databind/issues/3056): MismatchedInputException: Cannot deserialize instance of
`com.fasterxml.jackson.databind.node.ObjectNode` out of VALUE_NULL token
* [#3060](../../jackson-databind/issues/3060): Missing override for `hasAsKey()` in `AnnotationIntrospectorPair`
* [#3062](../../jackson-databind/issues/3062): Creator lookup fails with `InvalidDefinitionException` for conflict between single-double/single-Double arg constructo
* [#3068](../../jackson-databind/issues/3068): `MapDeserializer` forcing `JsonMappingException` wrapping even if WRAP_EXCEPTIONS set to false

### Changes, data formats

#### CBOR

* [#236](../../jackson-dataformats-binary/issues/236): `ArrayIndexOutOfBoundsException` in `CBORParser` for invalid UTF-8 String
* [#240](../../jackson-dataformats-binary/issues/240): Handle invalid CBOR content like `[ 0x84 ]` (incomplete array)

#### Ion

* [#241](../../jackson-dataformats-binary/issues/241): Respect `WRITE_ENUMS_USING_TO_STRING` in `EnumAsIonSymbolSerializer`
* [#242](../../jackson-dataformats-binary/issues/242): Add support for generating IonSexps
* [#244](../../jackson-dataformats-binary/issues/244): Add support for deserializing IonTimestamps and IonBlobs
* [#247](../../jackson-dataformats-binary/issues/247): Enabling pretty-printing fails Ion serialization

#### [XML](../../jackson-dataformat-xml)

* [#445](../../jackson-dataformat-xml/issues/445): `XmlMapper`/`UntypedObjectDeserializer` mixes multiple unwrapped collections
* [#451](../../jackson-dataformat-xml/issues/451): Xml type resolver fails with NPE when property name is not specified in
polymorphic (de)serialization

### Changes, datatypes

#### [Java 8 date/time](../../jackson-modules-java8)

* [#202](../../jackson-modules-java8/issues/202): Unable to deserialize `YearMonth` when running as java9 module,
  added with `@JsonDeserialize` annotation

#### [JSR-353/JSON-P](../../jackson-datatypes-misc)

* [#6](../../jackson-datatypes-misc/issues/6): Add `jakarta` classifier version of `jackson-datatype-jsr353` to work with new Jakarta-based JSON-P

### Changes, other modules

#### Java 8 / parameter names

* [#206](../../jackson-modules-java8/issues/206): `@JsonKey`is ignored with parameter-names module registered

#### JAX-RS Providers

* [#132](../../jackson-jaxrs-providers/issues/132): jaxrs jakarta versions have javax.ws references in OSGi manifest

### Changes, JVM Languages

#### [Kotlin](../../jackson-module-kotlin)

* [#182](../../jackson-module-kotlin/issues/182): Nullable unsigned numbers do not serialize correctly
* [#409](../../jackson-module-kotlin/issues/409): `module-info.java` missing "exports"

#### [Scala](../../jackson-module-scala)

* [#330](../../jackson-module-scala/issues/330): try to fix issue where wrong constructor can be chosen
* [#438](../../jackson-module-scala/issues/438): fix issues with handling field names that have dashes
* [#495](../../jackson-module-scala/issues/495): Fix regression since v2.12.0 where Scala objects (as opposed to case objects) were not serializing correctly.
* [#497](../../jackson-module-scala/issues/497): allow DescriptorCache to be replaced with custom implementation
* [#505](../../jackson-module-scala/issues/505): Fix regression since v2.12.0 where `@JsonCreator` annotations on companion classes were not always been handled. Had to reintroduce the dependency on paranamer jar to fix this
