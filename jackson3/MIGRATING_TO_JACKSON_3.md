# Jackson 3 Migration Guide

This guide aims to "Connect the Dots" for Jackson 2.x to 3.x migration, helping developers by outlining the process. It is not a comprehensive guide, or check list.

Guide mostly references documentation in other repos and provides a high-level summary with appropriate links.

## Overview of Major Changes

1. New Maven group-id and Java package: `tools.jackson` (2.x used `com.fasterxml.jackson`)
    - See [JSTEP-1](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-1) for details
    - Exception: `jackson-annotations`: 2.x version still used with 3.x, so no group-id/Java package change
         - See [this discussion](https://github.com/FasterXML/jackson-future-ideas/discussions/90) for rationale
         - Jackson 3.0 uses `jackson-annotations` `2.20`
         - "Exception to Exception": annotations within `jackson-databind` like `@JsonSerialize` and `@JsonDeserialize` DO move to the new Java package (`tools.jackson.databind.annotation`). Same for format-specific annotation like XML (`jackson-dataformat-xml`) ones.
2. All `@Deprecated` (as of 2.20) methods, fields and classes are removed from 3.0
    - Javadocs in Jackson `2.20` updated to indicate replacements where available (incomplete: PRs welcome for more!)
3. Renaming of Core Entities (classes), methods, fields
    - See [JSTEP-6](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-6) for rationale, references to notable renamings
    - [JSTEP-8](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-8) covers refactoring/renaming of format-specific Read/Write features (like `JsonParser.Feature` into `JsonReadFeature` and `StreamReadFeaure`)
    - Javadocs in Jackson `2.20` updated to indicate new names where available (incomplete: PRs welcome for more!)
4. Changes to Default Configuration Settings (esp. various XxxFeatures)
    - See [JSTEP-2](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-2) for rationale, the set of changes made
5. Immutability of `ObjectMapper`, `JsonFactory`
    - `ObjectMapper` and `JsonFactory` (and their sub-types) are fully immutable in 3.x: instances to be constructed using the *builder* pattern
6. Use of format-aligned `ObjectMapper` mandatory: `new YAMLMapper()`, `new XmlMapper()`
    - Old `new ObjectMapper(new YAMLFactory())` no longer allowed
7. Unchecked exceptions: all Jackson exceptions are now `RuntimeException`s (unchecked)
    - [JSTEP-4](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-4)  explains rationale, changes
    - Base exception (`JsonProcessingException` in 2.x, renamed as `JacksonException`) now extends `RuntimeException` and NOT `IOException` (like 2.x did)
8. Embedding and Removal of "Java 8 modules"
    -  All 3 "Java 8 modules", that were separate in Jackson 2.x, are now built-in to `jackson-databind` (no need to register separately)
        - `jackson-module-parameter-names`: auto-detection of constructor parameter names
        - `jackson-datatype-jdk8`: support for `java.util.Optional` and other optional types (`OptionalDouble`, ...)
        - `jackson-datatype-jsr310`: support `java.time` types (added in 3.0.0-rc3)
9. Baseline JDK raised to Java 17, from Java 8 in Jackson 2
    - [Vote happened here](https://github.com/FasterXML/jackson-future-ideas/discussions/73)
    - [Actual issue for works](https://github.com/FasterXML/jackson-databind/issues/4820) 

For the full list of all issues resolved for 3.0, see [Jackson 3.0 Release Notes](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0).

## High-level conversion overflow

Starting from the high-level change list, we can see the need for following changes:

1. Maven group id, Java package change
    - Need to update build files (`pom.xml`, `build.gradle`) to use new group id (`com.fasterxml.jackson.core` -> `tools.jackson.core` and so on)
    - Need to change import statements due to the change in the Java package (`com.fasterxml.jackson` -> `tools.jackson` -- EXCEPT not for `jackson-annotations`)
2. `@Deprecated` method, field, class removal:
    - Need to replace with non-Deprecated alternatives, as per `2.20` Javadocs updated to indicate replacement where possible
    - See later a section for a set of common cases
3. Renaming of Core Entities (classes), methods, fields
    - Need to change references to use the new name (including `import` statements): `2.20` Javadocs updated to indicate replacement where possible
    - [JSTEP-6](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-6) includes a list (likely incomplete) of renamed things as well
    - Streaming API Read/Write `Feature`s like former `JsonParser.Feature` and `JsonGenerator.Feature` were renamed as per [JSTEP-8](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-8)
        - `JsonParser.Feature` split into general `StreamReadFeature`s and JSON-specific `JsonReadFeature`s
        - `JsonGenerator.Feature` split into general `StreamWriteFeature`s and JSON-specific `JsonWriteFeature`s
        - Same was done for other format backends as well: Avro, CBOR, CSV, Ion, Smile, XML and YAML
4. Changes to Default Configuration Settings
    - MAY need to override some defaults (where existing 2.x behavior preferred) -- but most changes are to settings developers prefer so unlikely to need to change all
        - `JsonMapper.builderWithJackson2Defaults()` may be used to use some of legacy configuration settings (cannot change all defaults but can help migration)
    - [JSTEP-2](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-2) lists all default changes
5. Immutability of `ObjectMapper`, `JsonFactory`
    - `ObjectMapper`/`JsonMapper`: convert direct configuration with `Builder` alternatives: `JsonMapper.builder().enable(...).build()`
    - `JsonFactory` / `TokenStreamFactory`: convert direct configuration with `Builder` alternatives:  `JsonFactory.builder().enable(...).build()`
6. Use of format-aligned `ObjectMapper` mandatory
    - Format-specific sub-types already exist for all formats in 2.20
    - In 3.0, constructing plain `ObjectMapper` with format-specific `TokenStreamFactory` no longer allowed
7. Unchecked exceptions
    - May require changes to handling since catching Jackson exceptions is now optional
    - No need to declare `throws` clause for Jackson calls
    - Base exceptions renamed; specifically:
        - `JsonProcessingException` -> `JacksonException`
        - `JsonMappingException` -> `DatabindException`
8. Embedding and Removal of "Java 8 modules"
    - Simply remove Maven/Gradle dependencies, module registrations

## Discussions and References

1. [Jackson-future-ideas](https://github.com/FasterXML/jackson-future-ideas/wiki) Github repo contains the majority of planning and discussion for Jackson 3 preparation. It includes:
    - [JSTEP](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP)s (Jackson STrategic Enhancement Proposals)
         - Sources for most major changes
    - [Github Discussions section](https://github.com/FasterXML/jackson-future-ideas/discussions) in the same repo as JSTEP
         - see first (but not last) discussion [here](https://github.com/FasterXML/jackson-future-ideas/discussions/72) as example

## Detailed Conversion Guidelines

### 1. New Maven group-id and Java package

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
    implementation "com.fasterxml.jackson.core:jackson-databind"
}
```

### 2. Deprecated method/field/class/functionality removal

It is necessary to convert `@Deprecated` methods/fields/classes with the documented alternative where ones exist.
Jackson `2.20` Javadocs include replacement in many cases.

Here are some notes on the most commonly encountered cases.

#### Deprecated Methods

None reported yet

#### Deprecated Fields

None reported yet

#### Deprecated/Removed Classes

* `com.fasterxml.jackson.databind.MappingJsonFactory` is removed -- while not Deprecated in 2.x (where it is used), implementation is neither needed nor possible to support as-is.

#### Deprecated functionality: Format Detection

Jackson 1.x and 2.x contained functionality for auto-detecting format of arbitrary content to decode: functionality was part of `jackson-core` -- Java classes under `com.fasterxml.jackson.core.format` (like `DataFormatDetector`) -- (and implemented by `jackson-dataformat-xxx` components for non-JSON formats).

But due to complexity of implementation, problems with API handling, and lack of usage, this functionality was dropped from 3.0. No replacement exists

### 3. Core entity, method, field renaming

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
   - `writeObject()` -> `writePOJO()`
   - `getCurrentValue()` -> `currentValue()`
   - `setCurrentValue()` -> `assignCurrentValue()`
- `JsonParser`:
   - replace references in method names to "field" with "property"
   - replace "xxxTextYyy" methods (like `getTextCharacters()`) with "xxxStringYyy" methods (like `getStringCharacters()`)
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
* `JsonDeserializer` -> `ValueDeserializer`
* `JsonSerializer` -> `ValueSerializer`
* `JsonSerializable` -> `JacksonSerializable`
* `Module` -> `JacksonModule`
* `SerializerProvider` -> `SerializationContext`
* `TextNode` -> `StringNode`

Exception types:

* `JsonMappingException` -> `DatabindException`

Methods:

* For `JsonNode` many renamings: see [JSTEP-3](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-3) for details

### 4. Default Config Setting changes

[JSTEP-2](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-2) lists all changes.
In general, these changes do not necessarily cause problems or require changes, however, if you do observe runtime problems (or new unit test failures), it is good to consider the possibility that some default config setting changes could be the cause.
But not all changes are equally likely to cause compatibility problems: here are ones that are considered the most likely to cause problems or observed behavioral changes:

#### Changes: MapperFeature

* `MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS` (disabled in 3.0): this non-intuitive feature may have masked actual problems with *immutable* classes, wherein Jackson forcibly overwrote values of `final` fields (which is possible via *reflection*!), but the developer assumed a constructor was being used.
    * "Is it a Bug or Feature?" -- disabled since newer JVMs are less likely to allow the feature to work.
* `MapperFeature.DEFAULT_VIEW_INCLUSION` (disabled in 3.0): simple configuration change, but significant impact for `@JsonView` usage
* `MapperFeature.SORT_PROPERTIES_ALPHABETICALLY` (enabled in 3.0): likely to change the default ordering of property serialization for POJOs (where `@JsonPropertyOrder` is not used)
    * Highly visible and may break brittle unit tests (ones that assume specific ordering)
* `MapperFeature.USE_GETTERS_AS_SETTERS` (disabled in 3.0): another highly non-intuitive feature; but one that may have masked actual problems (no setter or constructor for passing `Collection` / `Map` valued properties)
    * Originally included for JAXB compatibility

#### Changes: DeserializationFeature

* `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES` (disabled in 3.0): May mask real issues with name mismatch
* `DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES` (enabled in 3.0): May start failing `@JsonCreator` usage where missing values for primitive (like `int`) valued properties can start failing.

#### Changes: SerializationFeature

* `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS` (enabled in 3.0): Highly visible change to serialization; may break unit tests

### 5. Immutability of `ObjectMapper`, `JsonFactory`

Since both `ObjectMapper` and `JsonFactory` (`TokenStreamFactory`) -- along with their subtypes -- are fully immutable in 3.0, neither has direct configurability: no simple setters, or methods to configure handlers.
Instead, builder-based configuration is needed:

#### Configuring ObjectMappers

A simple example of constructing JSON-handling `ObjectMapper`:

```java
final JsonMapper mapper = JsonMapper.builder() // format-specific builders
   .addModule(new JodaModule()) // to use Joda date/time types
   .enable(JsonWriteFeature.ESCAPE_NON_ASCII) // configure JSON-escaping
   .build();
```

Note, too, that given a mapper instance, you CAN create a `Builder` with its settings to create a re-configured instance:

```java
JsonMapper mapper2 = mapper.rebuild()
   .enable(SerializationFeature.INDENT_OUTPUT)
   .build();
```

#### Configuring TokenStreamFactories

### 6. Use of format-aligned `ObjectMapper`

Although use of

    new ObjectMapper()

is still allowed, recommended to use one of

    new JsonMapper()
    JsonMapper.builder().builder()

recommend. And all construction of generic `ObjectMapper`:

    new ObjectMapper(new YAMLFactory()); // and similar

MUST be converted to format-specific `ObjectMapper` subtypes:

    new YAMLMapper(new YAMLFactory());
    new YAMLMapper(); // same as above
    new YAMLMapper(YAMLFactory().builder()
        // configure
        .build());

In addition, it may make sense to start passing typed mapper instances along: `JsonMapper` instead of `ObjectMapper` (unless format-agnostic handling needs to be supported).


### 7. Unchecked Exceptions

No additional suggestions.

### 8. Embedding and Removal of "Java 8 modules"

No additional suggestions.
