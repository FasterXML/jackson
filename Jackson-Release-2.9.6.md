Patch version of [2.9](Jackson-Release-2.9), released 12-Jun-2018.

Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core)

* [#400](../../jackson-core/issues/400): Add mechanism for forcing `BufferRecycler` released (to call on shutdown)
* [#460](../../jackson-core/issues/460): Failing to link `ObjectCodec` with `JsonFactory` copy constructor
* [#463](../../jackson-core/issues/463): Ensure that `skipChildren()` of non-blocking `JsonParser` will throw exception if not enough input

#### [Databind](../../jackson-databind)

* [#1328](../../jackson-databind/issues/1328): External property polymorphic deserialization does not work with enums
* [#1565](../../jackson-databind/issues/1565): Deserialization failure with Polymorphism using JsonTypeInfo `defaultImpl`, subtype as target
* [#1964](../../jackson-databind/issues/1964): Failed to specialize `Map` type during serialization where type key type incompatibility overidden via "raw" types
* [#1990](../../jackson-databind/issues/1990): MixIn `@JsonProperty` for `Object.hashCode()` is ignored
* [#1991](../../jackson-databind/issues/1991): Context attributes are not passed/available to custom serializer if object is in POJO
* [#1998](../../jackson-databind/issues/1998): Removing "type" attribute with Mixin not taken in account if using ObjectMapper.copy()
* [#1999](../../jackson-databind/issues/1999): "Duplicate property" issue should mention which class it complains about
* [#2001](../../jackson-databind/issues/2001): Deserialization issue with `@JsonIgnore` and `@JsonCreator` + `@JsonProperty` for same property name
* [#2015](../../jackson-databind/issues/2015): `@Jsonsetter with Nulls.SKIP` collides with `DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL` when parsing enum
* [#2016](../../jackson-databind/issues/2016): Delegating JsonCreator disregards JsonDeserialize info
* [#2019](../../jackson-databind/issues/2019): Abstract Type mapping in 2.9 fails when multiple modules are registered
* [#2023](../../jackson-databind/issues/2023): `JsonFormat.Feature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT` not working with `null` coercion with `@JsonSetter`
* [#2027](../../jackson-databind/issues/2027): Concurrency error causes `IllegalStateException` on `BeanPropertyMap`
* [#2032](../../jackson-databind/issues/2032): Blacklist another serialization gadget (ibatis)
* [#2034](../../jackson-databind/issues/2034): Serialization problem with type specialization of nested generic types
* [#2038](../../jackson-databind/issues/2038): JDK Serializing and using Deserialized `ObjectMapper` loses linkage back from `JsonParser.getCodec()`
* [#2051](../../jackson-databind/issues/2051): Implicit constructor property names are not renamed properly with `PropertyNamingStrategy`
* [#2052](../../jackson-databind/issues/2052): CVE-2018-12022: Block polymorphic deserialization of types from Jodd-db library
* [#2058](../../jackson-databind/issues/2058): CVE-2018-12023: Block polymorphic deserialization of types from Oracle JDBC driver
* [#2060](../../jackson-databind/issues/2060): `UnwrappingBeanPropertyWriter` incorrectly assumes the found serializer is of type `UnwrappingBeanSerializer`

### Changes, dataformats

#### [Avro](../../jackson-dataformats-binary)

* [#136](../../jackson-dataformats-binary/issues/136): Fix MapWriteContext not correctly resolving union values

#### [CBOR](../../jackson-dataformats-binary)

* [#93](../../jackson-dataformats-binary/issues/93): `CBORParser` does not accept "undefined value"

#### [Protobuf](../../jackson-dataformats-binary)

* [#135](../../jackson-dataformats-binary/issues/135): Infinite sequence of `END_OBJECT` tokens returned at end of streaming read

#### [XML](../../jackson-dataformat-xml)

* [#282](../../jackson-dataformat-xml/issues/282): `@JacksonXmlRootElement` malfunction when using it with multiple `XmlMapper`s and disabling annotations

#### [YAML](../../jackson-dataformats-text)

* [#84](../../jackson-dataformats-text/issues/84): Add option to allow use of platform-linefeed (`YAMLGenerator.Feature.USE_PLATFORM_LINE_BREAKS`)

### Changes, datatypes

#### [Eclipse Collections](../../jackson-datatypes-collections)

** NEW DATATYPE!!! ***

* [#29](../../jackson-datatypes-collections/29): Initial datatype module implementation for [Eclipse Collections](https://github.com/eclipse/eclipse-collections)


### Changes, other modules

#### [Java 8 Date/Time](../../jackson-modules-java8)

* [#65](../../jackson-modules-java8/issues/65): Use `DeserializationContext.handleWeirdXxxValue()` for datetime deserializers

#### [Java 8 Parameter Names](../../jackson-modules-java8)

* [#67](../../jackson-modules-java8/issues/67): `ParameterNamesModule` does not deserialize with a single parameter constructor when using `SnakeCase` `PropertyNamingStrategy`

#### [JAXB](../../jackson-modules-base)

* [#44](../../jackson-modules-base/issues/44): (jaxb) `@XmlElements` does not work with `@XmlAccessorType(XmlAccessType.NONE)`
