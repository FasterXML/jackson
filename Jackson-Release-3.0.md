Version 3.0 is under development, as of March 2021 (and since late 2017).

It is a major version update and thereby not API compatible with 2.x; however, since 2.10
new methods have been added in 2.x line to reduce incompatibilities.

Overall plans are outlined in [JSTEP-1](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-1)

## Changes, compatibility

### JDK

Java 8 is now required for all components.

## Major changes/features in 3.0

1. Removal of all deprecated methods, functionality, as of 2.x
    * Remove format-auto-detection (interesting idea but not widely used and few formats beyond JSON, XML, Smile, Avro support detection)
2. Full immutability of core entities, via Builder-style construction
    * `TokenStreamFactory` (old `JsonFactory`)
    * `ObjectMapper` (addition of format-specific sub-classes, from `JsonMapper` to `XmlMapper`)
    * Note Jackson 2.10 and later will also support most of Builder-style construction but without strict guarantees of immutability of underlying entities (due to backwards-compatibility reasons)
    * See [Jackson 3 immutability with builders](https://cowtowncoder.medium.com/jackson-3-0-immutability-w-builders-d9c532860d88)
3. Tighter integration between Streaming and Databinding to allow improved support of non-JSON formats
    * `ObjectMapper` remains shared API, but every format will have its own mapper subtype, including `JsonMapper` for JSON
        * Partly implemented in 2.x since 2.10, to support Builder-style construction (albeit without immutability)
    * Format-specific mapper will (have to) use format-specific `TokenStreamFactory`
4. Unchecked exceptions (see [JSTEP-4](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-4)
    * Base 2.x exceptions will be replaced with new counterparts as follows (counterparts will be added in 2.x but mostly used in 3.0)
    * `JsonProcessingException` (root exception) becomes `JacksonException`
    * `JsonParseException`/`JsonGenerationException` (streaming) will become `StreamReadException`/`StreamWriteException`
    * `JsonMappingException` (root for databind exceptions) becomes `DatabindException`
    * While new exceptions will be added by Jackson 2.13, their use is limited: they CAN be caught but NOT thrown (sort of "read-only" upgrade) while retaining compatibility across minor versions

## Changes, core

### [Annotations](../../jackson-annotations)

### [Streaming](../../jackson-core)

* [#378](../../jackson-core/issues/378): Change default for `TokenStreamFactory.Feature.INTERN_FIELD_NAMES` to `false`
* [#402](../../jackson-core/issues/402): Remove dataformat-auto-detection functionality
* [#411](../../jackson-core/issues/411): Rename `JsonStreamContext` as `TokenStreamContext` 
* [#413](../../jackson-core/issues/413): Remove `ObjectCodec`: replace with `ObjectWriteContext` / `ObjectReadContext`
* [#432](../../jackson-core/issues/432): Add new `TreeNode` subtypes: `ArrayTreeNode`, `ObjectTreeNode`
* [#433](../../jackson-core/issues/433): Add Builder pattern for creating configured Stream factories
* [#456](../../jackson-core/issues/456): Add `JsonParser.readAsValue(ResolvedType)`
* [#492](../../jackson-core/issues/492): Ensure primitive type names in error message enclosed in backticks
* [#551](../../jackson-core/issues/551): Remove `JsonGenerator.setPrettyPrinter()` from 3.0
* [#663](../../jackson-core/issues/663): Rename `JsonEOFException` as `UnexpectedEndOfInputException`
* [#670](../../jackson-core/issues/670): Replace references to "field" in `JsonGenerator`, `JsonParser` method names with "property"
* [#671](../../jackson-core/issues/671): Replace `getCurrentLocation()`/`getTokenLocation()` with `currentLocation()`/`currentTokenLocation()`
* [#676](../../jackson-core/issues/676): Remove `JsonGenerator.canWriteBinaryNatively()`, `canWriteFormattedNumbers()` (replaced by `StreamWriteCapability` equivalents)
* [#680](../../jackson-core/issues/680): Allow use of `java.nio.file.Path` as parser source, generator target
* [#689](../../jackson-core/issues/689): Remove existing "request payload" functionality
* [#785](../../jackson-core/issues/785): Make `JsonGenerator.writeXxx()` methods chainable
* [#793](../../jackson-core/issues/793): Rename "com.fasterxml.jackson" -> "tools.jackson"
* Rename `JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT` as `AUTO_CLOSE_CONTENT`
* Add `TreeCodec.nullNode()`, `TreeNode.isNull()` methods
* Change the way `JsonLocation.NA` is included in exception messages

### [Databind](../../jackson-databind)

#### Functionality removal

* [#1772](../../jackson-databind/issues/1772): Remove `MapperFeature. USE_STD_BEAN_NAMING`
* [#1773](../../jackson-databind/issues/1773): Remove `MapperFeature.AUTO_DETECT_xxx` features
* [#1917](../../jackson-databind/issues/1917): Remove `canSerialize` and `canDeserialize` methods from `ObjectMapper`
* [#1973](../../jackson-databind/issues/1973): Remove support for "default [Map] key serializer" configuration from
`SerializerProvider`
* [#2040](../../jackson-databind/issues/2040): Remove `JsonSerializer.isEmpty()`
* Remove `MappingJsonFactory`

#### Functionality changes

* [#1600](../../jackson-databind/issues/1600): Serializing locale with underscore, not standard hyphen
* [#1762](../../jackson-databind/issues/1762): `StdDateFormat`: serialize time offset using colon
* [#1774](../../jackson-databind/issues/1774): Merge Java8 datatype (`Optional`, `Stream`) support in core databind
* [#1775](../../jackson-databind/issues/1775): Merge Java8 parameter name support (`jackson-module-parameter-names`) in core databind
* [#1781](../../jackson-databind/issues/1781): Return `ObjectNode` from `ObjectNode` set-methods in order to allow better chaining
* [#](../../jackson-databind/issues/): 
* [#1888](../../jackson-databind/issues/1888): Merge `ResolvableSerializer` into `JsonSerializer`, `ResolvableDeserializer` into `JsonDeserializer`
* [#1889](../../jackson-databind/issues/1889): Merge `ContextualSerializer` into `JsonSerializer`, `ContextualDeserializer` into `JsonDeserializer`
* [#1916](../../jackson-databind/issues/1916): Change `MapperFeature.USE_GETTERS_AS_SETTERS)` default to `false`
* [#1994](../../jackson-databind/issues/1994): Limit size of `SerializerCache`, auto-flush on exceeding
* [#1995](../../jackson-databind/issues/1995): Limit size of `DeserializerCache`, auto-flush on exceeding
* [#2177](../../jackson-databind/issues/2177): Change parent type of `JsonProcessingException` to be `RuntimeException`
* [#2713](../../jackson-databind/issues/2713): Change wording of `UnrecognizedPropertyException` to refer to "property" not "field"
* [#3028](../../jackson-databind/issues/3028): Change `UUIDSerializer` to use `StreamWriteCapability` check instead of
  `JsonGenerator.canWriteBinaryNatively()`

#### Naming changes

* [#3037](../../jackson-databind/issues/3037): Rename `Module` as `JacksonModule` in 3.0 (to avoid overlap with `java.lang.Module`)

#### New features

* [#1789](../../jackson-databind/issues/1781): Add `createGenerator` methods in `ObjectMapper`, `ObjectWriter`
* [#1790](../../jackson-databind/issues/1790): Add `createParser` methods in `ObjectMapper`, `ObjectReader`
* [#1883](../../jackson-databind/issues/1883): Add "abstract type mapping" for deserialization from `Map<ENUMTYPE,V>` into `EnumMap` (and `Set<ENUMTYPE>` to `EnumSet<EnumType>`)
* [#1954](../../jackson-databind/issues/1954): Add Builder pattern for creating configured `ObjectMapper` instances
* [#1955](../../jackson-databind/issues/1955): Change the way `Module`s configure, interact with `ObjectMapper`
* [#2013](../../jackson-databind/issues/2013): Allow use of `java.nio.file.Path` for `readValue()`, `writeValue()`
* [#2411](../../jackson-databind/issues/2411): `valueToTree()` during serialization (via `SerializerProvider()`)
* [#2828](../../jackson-databind/issues/2828): Add `DatabindException` as intermediate subtype of `JsonMappingException`
* [#3536](../../jackson-databind/issues/3536): Create new exception type `JsonNodeException` for use by `JsonNode`-related problems

## Changes, dataformats

## Changes, datatypes

### Guava

* [#24](../../jackson-datatypes-collections/issues/24): Support for Guava's `Immutable{Double,Int,Long}Array`
* [#69](../../jackson-datatypes-collections/issues/69): Add support for Guava primitives during deserialization

## Changes, JAX-RS

## Changes, JVM Languages

## Changes, Other modules
