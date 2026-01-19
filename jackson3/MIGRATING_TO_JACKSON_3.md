# Jackson 3 Migration Guide

This guide aims to "Connect the Dots" for Jackson 2.x to 3.x migration, helping developers by outlining the process. It is not a comprehensive guide, or check list.

Guide mostly references documentation in other repos and provides a high-level summary with appropriate links.

## Overview of Major Changes

1. Baseline JDK raised to Java 17, from Java 8 in Jackson 2.x
    - [Vote happened here](https://github.com/FasterXML/jackson-future-ideas/discussions/73)
    - [Actual issue for works](https://github.com/FasterXML/jackson-databind/issues/4820)
2. New Maven group-id and Java package: `tools.jackson` (2.x used `com.fasterxml.jackson`)
    - See [JSTEP-1](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-1) for details
    - Exception: `jackson-annotations`: 2.x version still used with 3.x, so no group-id/Java package change
         - See [this discussion](https://github.com/FasterXML/jackson-future-ideas/discussions/90) for rationale
         - Jackson 3.0 uses `jackson-annotations` `2.20`
         - "Exception to Exception": annotations within `jackson-databind` like `@JsonSerialize` and `@JsonDeserialize` DO move to the new Java package (`tools.jackson.databind.annotation`). Same for format-specific annotation like XML (`jackson-dataformat-xml`) ones.
3. All `@Deprecated` (as of 2.20) methods, fields and classes are removed from 3.0
    - Javadocs in Jackson `2.20` updated to indicate replacements where available (incomplete: PRs welcome for more!)
4. Renaming of Core Entities (classes), methods, fields
    - See [JSTEP-6](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-6) for rationale, references to notable renamings
    - [JSTEP-8](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-8) covers refactoring/renaming of format-specific Read/Write features (like `JsonParser.Feature` into `JsonReadFeature` and `StreamReadFeaure`)
    - Javadocs in Jackson `2.20` updated to indicate new names where available (incomplete: PRs welcome for more!)
5. Changes to Default Configuration Settings (esp. various XxxFeatures)
    - See [JSTEP-2](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-2) for rationale, the set of changes made
6. Immutability of `ObjectMapper`, `JsonFactory`
    - `ObjectMapper` and `JsonFactory` (and their sub-types) are fully immutable in 3.x: instances to be constructed using the *builder* pattern
7. Use of format-aligned `ObjectMapper` mandatory: `new YAMLMapper()`, `new XmlMapper()`
    - Old `new ObjectMapper(new YAMLFactory())` no longer allowed
8. Unchecked exceptions: all Jackson exceptions are now `RuntimeException`s (unchecked)
    - [JSTEP-4](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-4)  explains rationale, changes
    - Base exception (`JsonProcessingException` in 2.x, renamed as `JacksonException`) now extends `RuntimeException` and NOT `IOException` (like 2.x did)
9. Embedding and Removal of "Java 8 modules"
    -  All 3 "Java 8 modules", that were separate in Jackson 2.x, are now built-in to `jackson-databind` (no need to register separately)
        - `jackson-module-parameter-names`: auto-detection of constructor parameter names
        - `jackson-datatype-jdk8`: support for `java.util.Optional` and other optional types (`OptionalDouble`, ...)
        - `jackson-datatype-jsr310`: support `java.time` types (added in 3.0.0-rc3)
10. Deprecation of `jackson-module-jsonSchema` module
    - As per [JSTEP-9](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-9), some 2.x modules were Deprecated and no 3.0.0 version was produced (no 3.x branch with converted code base)
        - [JSON Schema](https://github.com/FasterXML/jackson-module-jsonSchema) module is considered Obsolete, no plans to migrate
        - [Jackson Hibernate datatype](https://github.com/FasterXML/jackson-datatype-hibernate) was added in 3.0.2.

For the full list of all issues resolved for 3.0, see [Jackson 3.0 Release Notes](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0).

## Discussions and References

- [Jackson-future-ideas](https://github.com/FasterXML/jackson-future-ideas/wiki) Github repo contains the majority of planning and discussion for Jackson 3 preparation. It includes:
    - [JSTEP](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP)s (Jackson STrategic Enhancement Proposals)
        - Sources for most major changes
    - [Github Discussions section](https://github.com/FasterXML/jackson-future-ideas/discussions) in the same repo as JSTEP
        - see first (but not last) discussion [here](https://github.com/FasterXML/jackson-future-ideas/discussions/72) as example

## Community References

* OpenRewrite recipe for Jackson 2 -> 3 migration which can be found [here](https://docs.openrewrite.org/recipes/java/jackson/upgradejackson_2_3)
* Blog posts about regarding Jackson 3 release from our own @cowtowncoder [here](https://cowtowncoder.medium.com/jackson-3-0-0-ga-released-1f669cda529a)
* Jackson 3 support in Spring [here](https://spring.io/blog/2025/10/07/introducing-jackson-3-support-in-spring)

## Performance considerations (new in 3.x)

While the functional migration covers API and behavior changes, a couple of **default settings in 3.x can have modest performance impact** compared to 2.x. If you are sensitive to throughput/latency, consider the following:

1. **Trailing-token checks**
   - In Jackson 3.x, `DeserializationFeature.FAIL_ON_TRAILING_TOKENS` is **enabled by default** (it was off by default in 2.x). This adds a small amount of overhead because the parser validates there is no extra content after a successful value parse.
   - If your inputs are trusted/controlled and you want to match 2.x behavior, you can disable it:

     ```java
     JsonMapper mapper = JsonMapper.builder()
         .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
         .build();
     // Or on read call:
     MyValue v = mapper.readerFor(MyValue.class)
         .without(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
         .readValue(source);
     ```

   - Recommendation: keep it **enabled** for security/correctness unless profiling shows a measurable regression on your workload.

2. **Buffer recycling settings**

Buffer recycling is used for `byte[]` and `char[]` buffers used by Streaming parsers and generators (`JsonParser`, `JsonGenerator`). There are multiple recycler pool (`RecyclerPool`) implementations optimized for different use cases: Jackson 2.x and 3.x have different default pool choices.

   - Jackson 3.0 defaults to a **deque-based** `RecyclerPool`, which can add overhead in some common cases versus the 2.x default (`JsonRecyclerPools.threadLocalPool()`).
   - If your workload benefited from the 2.x behavior, set the pool explicitly when building your `TokenStreamFactory` (e.g., `JsonFactory`), and then pass that factory to your mapper:

     ```java
     JsonFactory factory = JsonFactory.builder()
         .recyclerPool(JsonRecyclerPools.threadLocalPool())
         .build();

     JsonMapper mapper = JsonMapper.builder(factory)
         .build();
     ```

   - Note: choose the pool that matches your deployment model (single-thread hot loops vs. highly concurrent). Test both options under production-like load.
   - Note: it is even possible that for some cases, not recycling may have least overhead:

     ```java
     JsonFactory factory = JsonFactory.builder()
         .recyclerPool(JsonRecyclerPools.nonRecyclingPool())
         .build();

We will expand this section as more performance-affecting defaults are identified.

**References**
- Default for trailing-tokens in 3.0 discussion: [jackson-databind#3406](https://github.com/FasterXML/jackson-databind/issues/3406)
- Recycler pool defaults & alternatives: [jackson-core#1117](https://github.com/FasterXML/jackson-core/issues/1117), default change notes [jackson-core#1266](https://github.com/FasterXML/jackson-core/issues/1266), how to override in 2.17.x/3.x [jackson-core#1293](https://github.com/FasterXML/jackson-core/issues/1293)
- Background on moving away from ThreadLocal pools (virtual threads): [jackson-core#919](https://github.com/FasterXML/jackson-core/issues/919)

-----

# Conversion

> This section aims to guide users through actual conversation from Jackson 2 to 3.
> Starting with "High-level conversion overflow" section, followed by "Detailed Conversion Guidelines"

## High-level conversion overflow

Starting from the high-level change list, we can see the need for following changes:

1. Java baseline: JDK 17
    - Depends on your application :)
2. Maven group id, Java package change
    - Need to update build files (`pom.xml`, `build.gradle`) to use new group id (`com.fasterxml.jackson.core` -> `tools.jackson.core` and so on)
    - Need to change import statements due to the change in the Java package (`com.fasterxml.jackson` -> `tools.jackson` -- EXCEPT not for `jackson-annotations`)
3. `@Deprecated` method, field, class removal:
    - Need to replace with non-Deprecated alternatives, as per `2.20` Javadocs updated to indicate replacement where possible
    - See a later section for a set of common cases
4. Renaming of Core Entities (classes), methods, fields
    - Need to change references to use the new name (including `import` statements): `2.20` Javadocs updated to indicate replacement where possible
    - [JSTEP-6](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-6) includes a list (likely incomplete) of renamed things as well
    - Streaming API Read/Write `Feature`s like former `JsonParser.Feature` and `JsonGenerator.Feature` were renamed as per [JSTEP-8](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-8)
        - `JsonParser.Feature` split into general `StreamReadFeature`s and JSON-specific `JsonReadFeature`s
        - `JsonGenerator.Feature` split into general `StreamWriteFeature`s and JSON-specific `JsonWriteFeature`s
        - Same was done for other format backends as well: Avro, CBOR, CSV, Ion, Smile, XML and YAML
5. Changes to Default Configuration Settings
    - MAY need to override some defaults (where existing 2.x behavior preferred) -- but most changes are to settings developers prefer so unlikely to need to change all
        - `JsonMapper.builderWithJackson2Defaults()` may be used to use some of legacy configuration settings (cannot change all defaults but can help migration)
    - [JSTEP-2](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-2) lists all default changes
6. Immutability of `ObjectMapper`, `JsonFactory`
    - `ObjectMapper`/`JsonMapper`: convert direct configuration with `Builder` alternatives: `JsonMapper.builder().enable(...).build()`
    - `JsonFactory` / `TokenStreamFactory`: convert direct configuration with `Builder` alternatives:  `JsonFactory.builder().enable(...).build()`
7. Use of format-aligned `ObjectMapper` mandatory
    - Format-specific sub-types already exist for all formats in 2.20
    - In 3.0, constructing plain `ObjectMapper` with format-specific `TokenStreamFactory` no longer allowed
8. Unchecked exceptions
    - May require changes to handling since catching Jackson exceptions is now optional
    - No need to declare `throws` clause for Jackson calls
    - Base exceptions renamed; specifically:
        - `JsonProcessingException` -> `JacksonException`
        - `JsonMappingException` -> `DatabindException`
    - Other exception renamings:
        - `JsonEOFException` -> `UnexpectedEndOfInputException`
9. Embedding and Removal of "Java 8 modules"
    - Simply remove Maven/Gradle dependencies, module registrations
10. Deprecation of `jackson-module-jsonSchema` module
    - For `jackson-module-jsonSchema` use alternate tools

## Detailed Conversion Guidelines

### 1. Java baseline: JDK 17

No additional suggestions.

### 2. New Maven group-id and Java package

#### Java Package name change

Changes to `import` statements should be quite mechanical:

- Replace `com.fasterxml.jackson.` with `tools.jackson.` everywhere
    - EXCEPT NOT for `com.fasterxml.jackson.annotation`

#### Maven Group id

Similarly change to Maven Group id in the build files (`pom.xml`, `build.gradle`) should be mechanical:

- Replace `com.fasterxml.jackson` with `tools.jackson` for dependencies
    - BUT NOT for `com.fasterxml.jackson.annotation`

But beyond this, if you are not yet using [jackson-bom](https://github.com/FasterXML/jackson-bom) for enforcing compatible set of versions, we would highly encourage starting to do so.
This could additionally help avoid most of complexity of `jackson-annotations` dependency since it is possible to rely on annotations package as transitive dependency. For Maven you can just do:

```xml
<project>
  <!-- ... -->
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>tools.jackson</groupId>
        <artifactId>jackson-bom</artifactId>
        <version>3.0.0</version>
        <scope>import</scope>
        <type>pom</type>
      </dependency>   
   </dependencies>
  </dependencyManagement>

  <!-- ... -->
  <dependencies>
    <dependency>
      <groupId>tools.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
    </dependency>
    <!-- jackson-core and jackson-annotations transitive dependencies -->
  </dependencies>
<project>
```

and for Gradle you can use something like:

```gradle
plugins {
    id 'java'
}
repositories {
    mavenCentral()
}
dependencies {
    implementation platform("tools.jackson:jackson-bom:3.0.0")
    // Now declare Jackson modules WITHOUT versions
    implementation "tools.jackson.core:jackson-databind"
}
```

### 3. Deprecated method/field/class/functionality removal

It is necessary to convert `@Deprecated` methods/fields/classes with the documented alternative where ones exist.
Jackson `2.20` Javadocs include replacement in many cases.

Here are some notes on the most commonly encountered cases.

#### Deprecated Methods

* `JsonFactory`
    * `getCodec()`, `setCodec()`: removed from 3.0 while not deprecated (since use ok in 2.x)
    * No replacement since codec-style object (`ObjectMapper` or similar) no longer associated with streaming factory -- only with generators, parsers
* `JsonGenerator`
    * `getCodec()`: replaced with `objectWriteContext()` to provide same (or better) functionality
* `JsonParser`
    * `canWriteBinaryNatively()`, `canWriteFormattedNumbers()` replaced with `StreamWriteCapability.CAN_WRITE_BINARY_NATIVELY` / `StreamWriteCapability.CAN_WRITE_FORMATTED_NUMBERS` (respectively)
    * `getCodec()`: replaced with `objectReadContext()` to provide same (or better) functionality
* `ObjectMapper`
    * `copy()`: removed from 3.0 while not deprecated (since use ok in 2.x)
    * No replacement as making exact copies no longer necessary or useful: creating differently configured instances needs to use new Build pattern
        * See "ObjectMapper: copying an existing mapper instance" for details

#### Deprecated Fields

None reported yet

#### Deprecated/Removed Classes

* `com.fasterxml.jackson.databind.MappingJsonFactory` is removed -- while not Deprecated in 2.x (where it is used), implementation is neither needed nor possible to support as-is.
* `com.fasterxml.jackson.core.ObjectCodec` (of `jackson-core`) -- which exposed subset of `ObjectMapper` functionality to streaming API was removed
    * Replaced with 2 separate interfaces: `tools.jackson.core.ObjectReadContext`, `tools.jackson.core.ObjectWriteContext`
    * `ObjectReadContext` implemented by `jackson-databind` class `DeserializationContext`
    * `ObjectWriteContext` implemented by `jackson-databind` class `SerializationContext` (known as `SerializerProvider` in 2.x)
    * See [jackson-core#413](https://github.com/FasterXML/jackson-core/issues/413) for details

#### Classes With Changed Visibility

* `tools.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator` is no longer public. Please use `BasicPolymorphicTypeValidator.builder()` to construct and configure your type validator. See "ObjectMapper: automatic inclusion of type information configuration".

#### Deprecated functionality: Format Detection

Jackson 1.x and 2.x contained functionality for auto-detecting format of arbitrary content to decode: functionality was part of `jackson-core` -- Java classes under `com.fasterxml.jackson.core.format` (like `DataFormatDetector`) -- (and implemented by `jackson-dataformat-xxx` components for non-JSON formats).

But due to complexity of implementation, problems with API handling, and lack of usage, this functionality was dropped from 3.0. No replacement exists

#### Deprecated functionality: ObjectMapper.can[De]Serialize()

Methods `ObjectMapper.canDeserialize()`, `ObjectMapper.canSerialize()` removed since they cannot be implemented in a useful way.

See [databind#1917](https://github.com/FasterXML/jackson-databind/issues/1917) for details.

### 4. Core entity, method, field renaming

Similar to deprecations, it is necessary to change old references to use new names (including `import` statements): `2.20` Javadocs were updated in some cases to indicate replacement (if available).
Further [JSTEP-6](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-6) includes a list of renamed things as well -- possibly incomplete list, but useful.

From that, here are ones you are most likely to encounter.

#### Renaming: `jackson-core`

Regular classes:

- `JsonFactory` split
    - API extracted as `TokenStreamFactory`
    - Implementation moved under `tools.jackson.core.json` (note the added "json" segment)
- `JsonStreamContext` -> `TokenStreamContext`
- `JsonLocation` -> `TokenStreamLocation`

Exception types:

- `JsonProcessingException` -> `JacksonException` (ultimate base exception)
- `JsonParseException` -> `StreamReadException
- `JsonEOFException` -> `UnexpectedEndOfInputException`
- `JsonGenerationException` -> `StreamWriteException`

Methods:

- `JsonGenerator`:
   - replace references in method names to "field" with "property"
   - `getCodec()` -> `objectWriteContext()`
   - `getCurrentValue()` -> `currentValue()`
   - `setCurrentValue()` -> `assignCurrentValue()`
   - `writeObject()` -> `writePOJO()`
- `JsonParser`:
   - replace references in method names to "field" with "property"
   - replace "xxxTextYyy" methods (like `getText()`, `getTextCharacters()`) with "xxxStringYyy" methods (like `getString()`, `getStringCharacters()`)
   - `getCodec()` -> `objectReadContext()`
   - `getCurrentLocation()` -> `currentLocation()`
   - `getTokenLocation()` -> `currentTokenLocation()`
   - `getCurrentValue()` -> `currentValue()`
   - `setCurrentValue()` -> `assignCurrentValue()`

Fields:

- `JsonToken.FIELD_NAME` -> `JsonToken.PROPERTY_NAME`

#### Renaming: Streaming Format backends

Streaming API Read/Write `Feature`s like former `JsonParser.Feature` and `JsonGenerator.Feature` were renamed as per [JSTEP-8](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-8).

* `JsonParser.Feature` split into general `StreamReadFeature`s and JSON-specific `JsonReadFeature`s
* `JsonGenerator.Feature` split into general `StreamWriteFeature`s and JSON-specific `JsonWriteFeature`s

Same was done for format backends as well:

* Avro: `AvroParser.Feature` -> `AvroReadFeature`; `AvroGenerator.Feature` -> `AvroWriteFeature`
* CBOR: `CBORGenerator.Feature` -> `CBORWriteFeature`
* CSV: `CsvParser.Feature` -> `CsvReadFeature`; `CsvGenerator.Feature` -> `CsvWriteFeature`
* Ion: `IonParser.Feature` -> `IonReadFeature`; `IonGenerator.Feature` -> `IonWriteFeature`
* Smile: `SmileParser.Feature` -> `SmileReadFeature`; `SmileGenerator.Feature` -> `SmileWriteFeature`
* XML: `FromXmlParser.Feature` -> `XmlReadFeature`; `ToXmlGenerator.Feature` -> `XmlWriteFeature`
* YAML: `YAMLParser.Feature` -> `YAMLReadFeature`; `YAMLGenerator.Feature` -> `YAMLWriteFeature`

#### Renaming: `jackson-databind`

Regular classes:

* `BeanDeserializerModifier` -> `ValueDeserializerModifier`
* `BeanSerializerModifier` -> `ValueSerializerModifier`
* `ContainerSerializer` -> `StdContainerSerializer`
* `ContextualDeserializer` -> REMOVED -- now method `createContextual` part of `ValueDeserializer`
* `ContextualSerializer` -> REMOVED -- now method `createContextual` part of `ValueSerializer`
* `JsonDeserializer` -> `ValueDeserializer`
* `JsonSerializer` -> `ValueSerializer`
* `JsonSerializable` -> `JacksonSerializable`
* `Module` -> `JacksonModule` (to resolve naming overlap with JDK `Module`)
* `ResolvableDeserializer` -> REMOVED -- now method `resolve()` part of `ValueDeserializer`
* `ResolvableSerializer` -> REMOVED -- now method `resolve()` part of `ValueSerializer`
* `SerializerProvider` -> `SerializationContext`
* `TextNode` -> `StringNode`

Exception types:

* `JsonMappingException` -> `DatabindException`

"Feature" enums

* `DateTimeFeature` created: some existing `DeserializationFeature`/`SerializationFeature`s moved
    * `DeserializationFeature`
        * `ADJUST_DATES_TO_CONTEXT_TIME_ZONE`
        * `READ_DATE_TIMESTAMPS_AS_NANOSECONDS`
    * `SerializationFeature`
        * `WRITE_DATES_AS_TIMESTAMPS` (default changed to `false` in 3.0)
        * `WRITE_DATE_KEYS_AS_TIMESTAMPS`
        * `WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS`
        * `WRITE_DATES_WITH_ZONE_ID`
        * `WRITE_DATES_WITH_CONTEXT_TIME_ZONE`
        * `WRITE_DURATIONS_AS_TIMESTAMPS`
* Some existing `DeserializationFeature`/`SerializationFeature`s moved to `EnumFeature` (added in 2.14)
    * `DeserializationFeature`
        * `FAIL_ON_NUMBERS_FOR_ENUMS`
        * `READ_ENUMS_USING_TO_STRING` (default changed to `true` in 3.0)
        * `READ_UNKNOWN_ENUM_VALUES_AS_NULL`
        * `READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE`
    * `SerializationFeature`
        * `WRITE_ENUMS_USING_TO_STRING` (default changed to `true` in 3.0)
        * `WRITE_ENUMS_USING_INDEX`
        * `WRITE_ENUM_KEYS_USING_INDEX`

Methods:

* Many of `JsonNode` methods renamed: see [JSTEP-3](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-3) for details
* `ObjectMapper.getRegisteredModuleIds()` -> `ObjectMapper.registeredModules()`
    * note: return value changed; see [databind#5272](https://github.com/FasterXML/jackson-databind/issues/5272) for details.

### 5. Default Config Setting changes

[JSTEP-2](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-2) lists all changes.
In general, these changes do not necessarily cause problems or require changes, however, if you do observe runtime problems (or new unit test failures), it is good to consider the possibility that some default config setting changes could be the cause.
But not all changes are equally likely to cause compatibility problems: here are ones that are considered the most likely to cause problems or observed behavioral changes:

#### Changes: MapperFeature

* `MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS` (disabled in 3.0): this non-intuitive feature may have masked actual problems with *immutable* classes, wherein Jackson forcibly overwrote values of `final` fields (which is possible via *reflection*!), but the developer assumed a constructor was being used.
    * "Is it a Bug or Feature?" -- disabled since newer JVMs are less likely to allow the feature to work.
* `MapperFeature.AUTO_DETECT_CREATORS` (and 4 related `AUTO_DETECT_xxx` variants) were removed: see "Configuring ObjectMappers" section for replacement (`JsonMapper.builder().changeDefaultVisibility(* `MapperFeature.DEFAULT_VIEW_INCLUSION` (disabled in 3.0): simple configuration change, but significant impact for `@JsonView` usage
* `MapperFeature.SORT_PROPERTIES_ALPHABETICALLY` (enabled in 3.0): likely to change the default ordering of property serialization for POJOs (where `@JsonPropertyOrder` is not used)
    * Highly visible and may break brittle unit tests (ones that assume specific ordering)
* `MapperFeature.USE_GETTERS_AS_SETTERS` (disabled in 3.0): another highly non-intuitive feature; but one that may have masked actual problems (no setter or constructor for passing `Collection` / `Map` valued properties)
    * Originally included for JAXB compatibility
...)`)
* `MapperFeature.USE_STD_BEAN_NAMING`: removed; no longer used or needed -- 3.0 behavior same as 2.x with Feature enabled.

#### Changes: DeserializationFeature

* `DeserializationFeature.FAIL_ON_TRAILING_TOKENS` (**enabled in 3.0**, disabled in 2.x): enables validation that no extra content follows a parsed value; improves safety but introduces modest overhead. Disable if inputs are trusted and performance is critical.
* `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES` (disabled in 3.0): May mask real issues with name mismatch
* `DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES` (enabled in 3.0): May start failing `@JsonCreator` usage where missing values for primitive (like `int`) valued properties can start failing.

#### Changes: SerializationFeature

* `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS` (**disabled in 3.0**, enabled in 2.x): Highly visible change to serialization; may break unit tests

### 6. Immutability of `ObjectMapper`, `JsonFactory`

Since both `ObjectMapper` and `JsonFactory` (`TokenStreamFactory`) -- along with their subtypes -- are fully immutable in 3.0, neither has direct configurability: no simple setters, or methods to configure handlers.
Instead, builder-based configuration is needed:

#### Configuring ObjectMappers, general

A simple example of constructing JSON-handling `ObjectMapper`:

```java
final JsonMapper mapper = JsonMapper.builder() // format-specific builders
   .addModule(new JodaModule()) // to use Joda date/time types
   .enable(JsonWriteFeature.ESCAPE_NON_ASCII) // configure streaming JSON-escaping
   .build();
```

Note, too, that given a mapper instance, you CAN create a `Builder` with its settings to create a re-configured instance:

```java
JsonMapper mapper2 = mapper.rebuild()
   .enable(SerializationFeature.INDENT_OUTPUT)
   .build();
```
#### Configuring ObjectMappers, special changes

Beside configuring simple Features via Builder instead of direct `ObjectMapper` calls, some changes are bit more involved

##### ObjectMapper: date format/time zone configuration

Instead of

    mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"));
    mapper.setTimeZone(TimeZone.getDefault());

use

    ObjectMapper mapper = JsonMapper.builder()
      .defaultDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"))
      .defaultTimeZone(TimeZone.getDefault())
      .build();

Note that the default time zone is UTC, NOT default TimeZone of JVM.

##### ObjectMapper: Serialization inclusion configuration

Instead of

    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

use

    ObjectMapper mapper = JsonMapper.builder()
      .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
      .changeDefaultPropertyInclusion(incl -> incl.withContentInclusion(JsonInclude.Include.NON_NULL))
      .build();

##### ObjectMapper: Visibility configuration

Instead of

    mapper.disable(MapperFeature.AUTO_DETECT_FIELDS);

use

    ObjectMapper mapper = JsonMapper.builder()
        .changeDefaultVisibility(vc ->
            vc.withFieldVisibility(JsonAutoDetect.Visibility.NONE))
    .build();


##### ObjectMapper: automatic inclusion of type information configuration

To configure specific type information, use `.activateDefaultTypingAsProperty()`. This did not change, however, the way this is configured did:

Instead of for example

    mapper.activateDefaultTypingAsProperty(LaissezFaireSubTypeValidator.instance,
        ObjectMapper.DefaultTyping.NON_CONCRETE_AND_ARRAYS,
        "@class");

use

    ObjectMapper mapper = JsonMapper.builder()
        .activateDefaultTypingAsProperty(typeValidator, DefaultTyping.NON_CONCRETE_AND_ARRAYS, "@class")
    .build();

where `typeValidator` is built using the builder present in `BasicPolymorphicTypeValidator`:

    var typeValidator = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("my.package.base.name.")
            .allowIfSubType("java.util.concurrent.")
            .allowIfSubTypeIsArray()
            // ...
            .build();


Note that the enum `DefaultTyping` also moved outside of the `ObjectMapper` to `tools.jackson.databind`.

##### ObjectMapper: copying an existing mapper instance

`ObjectMapper.copy` method has been removed. You are encouraged to create mappers by reusing a `Mapper.Builder` instance.
You can modify `Mapper.Builder` instances but `ObjectMapper` instances are immutable in Jackson 3 so it is not as useful to
copy mappers any more. `ObjectMapper.rebuild()` is one way to get a builder instance if you don't have a cached builder instance to work with.

One quick workaround if you still want to copy a mapper is to call `ObjectMapper.rebuild().build()` to create a new mapper instance.

##### ObjectMapper: default serialization/deserialization Views

In Jackson 2.x, you could configure default view at the `ObjectMapper` level:

```java
objectMapper.setConfig(objectMapper.getSerializationConfig().withView(Views.Public.class));
objectMapper.setConfig(objectMapper.getDeserializationConfig().withView(Views.Public.class));
```

Since `ObjectMapper` is immutable in 3.x and `setConfig` is removed, this no longer works.

**Jackson 3.1** ([#5575](https://github.com/FasterXML/jackson-databind/issues/5575)): Use `MapperBuilder.defaultSerializationView()` and `defaultDeserializationView()`:

```java
ObjectMapper mapper = JsonMapper.builder()
    .defaultSerializationView(Views.Public.class)
    .defaultDeserializationView(Views.Public.class)
    .build();
```

For per-request views, use `ObjectReader.withView()` / `ObjectWriter.withView()` as before.

#### Configuring TokenStreamFactories, general

Similar to `ObjectMapper`, streaming parser/generator factories -- subtypes of `TokenStreamFactory` like `JsonFactory` -- are also built using Builders:

```
JsonFactory f = JsonFactory.builder()
    .disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
    .build();
```

and may also be re-configured like so:

```
JsonFactory f2 = f.rebuild()
    .enable(StreamWriteFeature.STRICT_DUPLICATE_DETECTION)
    .build();
```

##### Choosing a RecyclerPool (performance)

To align with 2.x performance characteristics, you may explicitly configure the recycler pool on the factory builder:

```java
JsonFactory f = JsonFactory.builder()
    .recyclerPool(JsonRecyclerPools.threadLocalPool())
    .build();

JsonMapper mapper = JsonMapper.builder(f).build();
```

If your workload is highly concurrent, benchmark the default deque-based pool versus `threadLocalPool()` and choose based on observed throughput/latency and memory behavior.

Finally, to pass customized `TokenStreamFactory` for `ObjectMapper`, you will need to pass instance to `builder()` like so:

```
JsonFactory f = JsonFactory.builder()
   // configure
   .build();
ObjectMapper mapper = JsonMapper.builder(f)
   // configure
   .build();
```

### 7. Use of format-aligned `ObjectMapper`

Although use of

    new ObjectMapper()

is still allowed, the use of one of

    new JsonMapper()
    JsonMapper.builder().builder()

is recommend. And all construction of generic `ObjectMapper`:

    new ObjectMapper(new YAMLFactory()); // and similar

MUST be converted to format-specific `ObjectMapper` subtypes:

    new YAMLMapper(new YAMLFactory());
    new YAMLMapper(); // same as above
    new YAMLMapper(YAMLFactory().builder()
        // configure
        .build());

In addition, it may make sense to start passing typed mapper instances along: `JsonMapper` instead of `ObjectMapper` (unless format-agnostic handling needs to be supported).


### 8. Unchecked Exceptions

No additional suggestions.

### 9. Embedding and Removal of "Java 8 modules"

Configuration of Java 8 Date/Time handling in 3.x is done using new `DateTimeFeature` enumeration, like so:

```java
ObjectMapper MAPPER = JsonMapper.builder()
    .enable(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
    .build();
```

instead of either module-specific `JavaTimeFeature` or renamed `DeserializationFeature` / `SerializationFeature`

### 10. Deprecation of `jackson-module-jsonSchema` module

No additional suggestions.
