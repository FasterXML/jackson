[Jackson Version](Jackson-Releases) 2.16 development started in May 2023, after release of [2.15](Jackson-Release-2.15).

This wiki page gives a list of links to all changes (with brief descriptions) that will be included, as well as about plans for bigger changes.

## Status

Branch is under development as of September 2023.

## Patches

No release yet

## Documentation

### Articles, Blog posts

## New Modules

#### "Jakarta" variant of Guice module

[Guice 7](https://github.com/FasterXML/jackson-modules-base/tree/2.16/guice7) module is a "Jakarta" variant with `jakarta.inject` instead of `javax.inject`): needed to work with Guice 7.

## Changes, compatibility

### Compatibility: platform requirements

#### JDK

Same as [Jackson 2.15](Jackson-Release-2.15)

#### Kotlin

`kotlin-core` versions supported changed, as follows (see [module-kotlin#684](../../jackson-module-kotlin/pull/684) for details)

* Jackson 2.15.x: Kotlin-core 1.5 - 1.8
* Jackson 2.16.x: Kotlin-core 1.6 - 1.9

that is, support for `1.5` was dropped and support for `1.9` now verified.

jackson-module-kotlin removes MissingKotlinParameterException and replaces it with MismatchedInputException, which was its parent class([KOTLIN#617](https://github.com/FasterXML/jackson-module-kotlin/issues/617)).

## Changes, behavior

### Streaming (`jackson-core`)

* Default setting for `StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` (aka `JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION`) changed to `false` for improved defaults wrt security (less information leakage by default).
    * See [jackson-core#991](../../jackson-core/issues/991) for details.
    * Message for non-included source now "REDACTED" (instead of "UNKNOWN"), see [jackson-core#1039](../../jackson-core/issues/1039) for details.
* Behavior of `Version` (accessible by `JsonFactory.version()` and similar accessors in `JsonParser`, `JsonGenerator` etc) comparsion (`Version.compareTo()`) changed to consider Snapshot version, if any, 

### Databind

* `java.util.Locale` coercion from empty String will now result in `Locale.ROOT` (and not `null`) even if `DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT` is enabled (see [databind#4009](../../jackson-databind/issues/4009))

### Processing Limits (for jackson-core, format modules)

#### General

2.16 adds maximum processing limits for certain aspects of content generation (writing, serialization) as described below.
Addition of the general feature (and one of limits) was included under [jackson-core#1048](../../jackson-core/issues/1048).

Implemented limits are:

* Expressed in input units -- `byte`s or `char`s -- depending on input source
* Defined as longest allowed length, but not necessarily imposed at 100% accuracy: that is, if maximum allowed length is specified as 1000 units, something with length of, say 1003 may not cause exception (but 1500 would)
* Defined in new `StreamWriteConstraints` class, configurable on per-`JsonFactory` basis

#### Maximum Document length

Implementation of [jackson-core#1046](../../jackson-core/pull/1046) sets upper limit of longest accepted input for parsing.
Default limits is:

* Unlimited (marked as `-1`) -- no maximum length specified for input

#### Maximum Output nesting depth

Implementation of [jackson-core#1055](../../jackson-core/pull/1055) sets upper limit on maximum output nesting (Objects, Arrays) when generating output (writing JSON etc). Default limit is:

* 1000 levels

#### Maximum Property name length

Implementation of [jackson-core#1047](../../jackson-core/issues/1047) sets maximum length of allowed Property names when parsing input.
Default limit is:

* 50,000 units (bytes or chars, depending on input source)

-----

## Major focus areas, features included

### Improved/Extended Processing Limits

Implemented, See Above.

### Canonical JSON Output

Partial work, including:

* [#1042](../../jackson-core/pull/1042): Allow configuring spaces before and/or after the colon in `DefaultPrettyPrinter`
* [#3965](../../jackson-databind/issues/3965): Add `JsonNodeFeature.WRITE_PROPERTIES_SORTED` for sorting `ObjectNode` properties on serialization

### Rewrite of Property Introspection (internal)

Not Started Yet (same old story...)

-----

## Full Change list

### Changes, core

#### [Annotations](../../jackson-annotations)

* [#223](../../jackson-annotations/pull/223): Add new `OptBoolean` valued property in `@JsonTypeInfo` to allow per-type configuration of strict type id handling
* [#229](../../jackson-annotations/pull/229): Add `JsonTypeInfo.Value` object (backport from 3.0)
* [#234](../../jackson-annotations/pull/234): Add new `JsonTypeInfo.Id.SIMPLE_NAME`

#### [Streaming](../../jackson-core)

* [#991](../../jackson-core/issues/991): Change `StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` default to `false` in Jackson 2.16
* [#1007](../../jackson-core/issues/1007): Improve error message for `StreamReadConstraints` violations
* [#1015](../../jackson-core/issues/1015): `JsonFactory` implementations should respect `CANONICALIZE_FIELD_NAMES`
* [#1035](../../jackson-core/issues/1035): Root cause for failing test for `testMangledIntsBytes()` in `ParserErrorHandlingTest`
* [#1036](../../jackson-core/pull/1036): Allow all array elements in `JsonPointerBasedFilter`
* [#1039](../../jackson-core/issues/1039): Indicate explicitly blocked sources as "REDACTED" instead of "UNKNOWN" in `JsonLocation`
* [#1041](../../jackson-core/issues/1041): Start using AssertJ in unit tests
* [#1042](../../jackson-core/pull/1042): Allow configuring spaces before and/or after the colon in `DefaultPrettyPrinter` (for Canonical JSON)
* [#1046](../../jackson-core/issues/1046): Add configurable limit for the maximum number of bytes/chars of content to parse before failing
* [#1047](../../jackson-core/issues/1047): Add configurable limit for the maximum length of Object property names to parse before failing
* [#1048](../../jackson-core/issues/1048): Add configurable processing limits for JSON generator (`StreamWriteConstraints`)
* [#1050](../../jackson-core/issues/1050): Compare `_snapshotInfo` in `Version`
* [#1051](../../jackson-core/pull/1051): Add `JsonGeneratorDecorator` to allow decorating `JsonGenerator`s
* [#1064](../../jackson-core/pull/1064): Add full set of `BufferRecyclerPool` implementations
* [#1066](../../jackson-core/issues/1066): Add configurable error report behavior via `ErrorReportConfiguration`
* [#1081](../../jackson-core/pull/1081): Make `ByteSourceJsonBootstrapper` use `StringReader` for < 8KiB byte[] inputs
* [#1089](../../jackson-core/issues/1089): Allow pluggable buffer recycling via new `BufferRecyclerPool` extension point

#### [Databind](../../jackson-databind)

* [#2502](../../jackson-databind/issues/2502): Add a way to configure caches Jackson uses
* [#2787](../../jackson-databind/issues/2787): Mix-ins do not work for `Enum`s
* [#3251](../../jackson-databind/issues/3251): Generic class with generic field of runtime type `Double` is deserialized as `BigDecimal` when used with `@JsonTypeInfo` and `JsonTypeInfo.As.EXISTING_PROPERTY`
* [#3647](../../jackson-databind/issues/3647): `@JsonIgnoreProperties` not working with `@JsonValue`
* [#3780](../../jackson-databind/issues/3780): Deprecated JsonNode.with(String) suggests using JsonNode.withObject(String) but it is not the same thing
* [#3838](../../jackson-databind/issues/3838): Difference in the handling of `ObjectId-property` in `JsonIdentityInfo` depending on the deserialization route
* [#3877](../../jackson-databind/issues/3877): Add new `OptBoolean` valued property in `@JsonTypeInfo`, handling, to allow per-polymorphic type loose Type Id handling
* [#3906](../../jackson-databind/issues/3906): Regression: 2.15.0 breaks deserialization for records when `mapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE)`
* [#3924](../../jackson-databind/issues/3924): Incorrect target type when disabling coercion, trying to deserialize String from Array/Object
* [#3928](../../jackson-databind/issues/3928): `@JsonProperty` on constructor parameter changes default field serialization order
* [#3950](../../jackson-databind/issues/3950): Create new `JavaType` subtype `IterationType` (extending `SimpleType`)
* [#3953](../../jackson-databind/pull/3953): Use `JsonTypeInfo.Value` for annotation handling
* [#3965](../../jackson-databind/issues/3965): Add `JsonNodeFeature.WRITE_PROPERTIES_SORTED` for sorting `ObjectNode` properties on serialization (for Canonical JSON)
* [#4008](../../jackson-databind/pull/4008): Optimize `ObjectNode` findValue(s) and findParent(s) fast paths
* [#4009](../../jackson-databind/issues/4009): Locale "" is deserialised as `null` if `ACCEPT_EMPTY_STRING_AS_NULL_OBJECT` is enabled
* [#4011](../../jackson-databind/issues/4011): Add guardrail setting for `TypeParser` handling of type parameters
* [#4036](../../jackson-databind/pull/4036): Use `@JsonProperty` for Enum values also when `READ_ENUMS_USING_TO_STRING` enabled
* [#4037](../../jackson-databind/pull/4037): Fix `Enum` deserialization to use `@JsonProperty`, `@JsonAlias` even if `EnumNamingStrategy` used
* [#4039](../../jackson-databind/pull/4039): Use `@JsonProperty` and lowercase feature when serializing Enums despite using toString()
* [#4040](../../jackson-databind/pull/4040): Use `@JsonProperty` over `EnumNamingStrategy` for Enum serialization
* [#4041](../../jackson-databind/pull/4041): Actually cache EnumValues#internalMap
* [#4047](../../jackson-databind/issues/4047): `ObjectMapper.valueToTree()` will ignore the configuration `SerializationFeature.WRAP_ROOT_VALUE`
* [#4056](../../jackson-databind/pull/4056): Provide the "ObjectMapper.treeToValue(TreeNode, TypeReference)" method
* [#4060](../../jackson-databind/pull/4060): Expose `NativeImageUtil.isRunningInNativeImage()` method
* [#4061](../../jackson-databind/issues/4061): Add JsonTypeInfo.Id.SIMPLE_NAME which defaults type id to `Class.getSimpleName()`
* [#4071](../../jackson-databind/issues/4071): Impossible to deserialize custom `Throwable` sub-classes that do not have single-String constructors
* [#4078](../../jackson-databind/issues/4078): `java.desktop` module is no longer optional
* [#4082](../../jackson-databind/issues/4082): `ClassUtil` fails with `java.lang.reflect.InaccessibleObjectException` trying to setAccessible on `OptionalInt` with JDK 17+
* [#4090](../../jackson-databind/pull/4090): Support sequenced collections (JDK 21)
* [#4095](../../jackson-databind/issues/4095): Add `withObjectProperty(String)`, `withArrayProperty(String)` in `JsonNode`
* [#4096](../../jackson-databind/issues/4096): Change `JsonNode.withObject(String)` to work similar to `withArray()` wrt argument
* [#4144](../../jackson-databind/pull/4144): Log WARN if deprecated subclasses of `PropertyNamingStrategy` is used
* [#4145](../../jackson-databind/issues/4145): NPE when transforming a tree to a model class object, at `ArrayNode.elements()`
* [#4153](../../jackson-databind/issues/4153): Deprecated `ObjectReader.withType(Type)` has no direct replacement; need `forType(Type)`

### Changes, data formats

#### [Smile](../../jackson-dataformats-binary)

* [#403](../../jackson-dataformats-binary/issues/403): Remove Smile-specific buffer-recycling

#### [XML](../../jackson-dataformat-xml)

* [#148](../../jackson-dataformat/issues/148): `@JacksonXmlElementWrapper` not respected when serializing `Iterator`s / `Iterable`s
* [#302](../../jackson-dataformat/issues/302): Unable to serialize top-level Java8 Stream
* [#329](../../jackson-dataformat/issues/329): `@JacksonXmlElementWrapper` ignored on `Stream`
* [#599](../../jackson-dataformat/pull/599): Use `IterationType` in `TypeUtil`

#### YAML

* [#400](../../jackson-dataformats-text/issues/400): `IllegalArgumentException` when attempting to decode invalid UTF-8 surrogate by SnakeYAML
* [#406](../../jackson-dataformats-text/issues/406): `NumberFormatException` from SnakeYAML due to int overflow for corrupt YAML version
* [#426](../../jackson-dataformats-text/issues/426): Update to SnakeYAML 2.1

### Changes, datatypes

#### [Java 8 date/time](../../jackson-modules-java8/datetime)

* [#272](../../jackson-modules-java8/issues/272): `JsonFormat.Feature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS` not respected when deserialising `Instant`s

#### Guava

* [#90](../../jackson-datatypes-collections/issues/90): Cache Serialization serializes empty contents
* [#113](../../jackson-datatypes-collections/issues/113): Update default Guava dependency for Jackson 2.16 from Guava 23.x to 25.x
* [#116](../../jackson-datatypes-collections/pull/116): Suppport simple deserialization of `Cache`
* [#117](../../jackson-datatypes-collections/issues/117): `ImmutableRangeSet` fails to deserialize without explicit deserializer

#### [Hibernate](../../jackson-datatype-hibernate)

* [#140](../../jackson-datatype-hibernate/issues/140): `HibernateModule.REPLACE_PERSISTENT_COLLECTIONS` not working when `FetchType.EAGER`

#### [JSONP/JSR-353](../../jackson-datatypes-misc)

* [#37](../../jackson-datatypes-misc/pull/37): Fix class path scaning on each deserialization

### Changes, Other modules

#### Afterburner

* [#216](../../jackson-modules-base/pull/216): Disable when running in native-image

#### Blackbird

* [#181](../../jackson-modules-base/pull/181): BlackBird proxy object error in Java 17
* [#216](../../jackson-modules-base/pull/216): Disable when running in native-image

#### Guice

* [#209](../../jackson-modules-base/pull/209): Add guice7 (`jakarta.inject`) module

#### Jakarta XML Bind Annotations

* [#219](../../jackson-modules-base/issues/219): Using `jackson-module-jakarta-xmlbind-annotations` 2.15.2 fails in OSGi Environment with JAXB 4

### Changes, JVM Languages

#### [Kotlin](../../jackson-module-kotlin)

### Changes, Providers

...

