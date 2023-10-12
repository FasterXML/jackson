Version 2.10 was released on September 26, 2019.

This wiki page gives a list of links to all changes, but there is also 
a [blog entry](https://medium.com/@cowtowncoder/jackson-2-10-features-cd880674d8a2) that covers major
features in more detail.

## Status

Branch is closed for new patch versions after 2.10.5 release: new per-component micro-patches may still be released.

## Patches

Beyond initial 2.10.0 (described here), following patch releases have been made.

* [2.10.1](Jackson-Release-2.10.1) (09-Nov-2019)
* [2.10.2](Jackson-Release-2.10.2) (05-Jan-2020)
* [2.10.3](Jackson-Release-2.10.3) (03-Mar-2020)
* [2.10.4](Jackson-Release-2.10.4) (03-May-2020)
* [2.10.5](Jackson-Release-2.10.5) (21-Jul-2020)

### Micro-patches

Following micro-patches have been or will be released:

* `jackson-databind` `2.10.5.1` (02-Dec-2020) -- with `jackson-bom` version `2.10.5.20201202`
    * [#2589](../../jackson-databind/issues/2589):  `DOMDeserializer`: setExpandEntityReferences(false) may not prevent external entity expansion in all cases (CVE-2020-25649)

## Documentation

* [Jackson 2.10 Features](https://medium.com/@cowtowncoder/jackson-2-10-features-cd880674d8a2)
    * [Jackson 2.10: Safe Default Typing](https://medium.com/@cowtowncoder/jackson-2-10-safe-default-typing-2d018f0ce2ba)
    * [Jackson 2.10: JsonNode improvements](https://medium.com/@cowtowncoder/jackson-2-10-feature-jsonnode-improvements-18894c3ac3b5)
    * [Jackson 2.10: jackson-jr improvements](https://medium.com/@cowtowncoder/jackson-2-10-jackson-jr-improvements-9eb5bb7b35f)

## Changes, compatibility

No changes to minimum JDK baselines for use since [2.9](Jackson-Release-2.9), but 2.10 includes JDK 9+ compliant `module-info.class` which should improve forward-compatibility.
Older versions of tools may have issues with this class.

JDK 8 is required to build all components, however, as module info inclusion plug-in requires it (note: publishing to Maven Central also requires JDK 8), but runtime environment of JDK/JVM 7 is needed with exception of:

* `jackson-annotations`, `jackson-core`, `jackson-jr` only require JDK/JVM 6
* Kotlin, Scala and Java 8 modules require JDK/JVM 8 or higher

Enum serialization has changed slightly by introduction of 
`SerializationFeature.WRITE_ENUM_KEYS_USING_TO_STRING` which takes over `SerializationFeature.WRITE_ENUMS_USING_TO_STRING` for specific case of serializing `Enum` values of `java.util.Map` keys (see [databind#2129](../../jackson-databind/issues/2129) for details)

Another functional change is with Java 8 `Duration` type, which formerly ignored setting `SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS` but now uses it as originally planned (see
[java8-modules#75](https://github.com/FasterXML/jackson-modules-java8/pull/75) for details).

### Reported problems

We have released following problem reports regarding backwards-compatibility, aside from planned changes

#### Databind: `TypeReference` assignment compatibility for `readValue()`

Due to changes:

* [jackson-core#506](https://github.com/FasterXML/jackson-core/issues/506)
* [jackson-databind#2196](https://github.com/FasterXML/jackson-databind/pull/2196)

Generic type assignment compatibility is now expected for `TypeReference`: so, for example, following code:

```java
    MyType value = mapper.readValue(src, new TypeReference<SomeUnrelatedType>() { });
```

used to compile ok but obviously fail on run type when type cast fails.
With 2.10 code will not compile, and that is intentional.

But there is a problem with generic type co-variance (?): although you can assign to subtype like

```java
    Map<String, Object> value = mapper.readValue(src, new TypeReference<HashMap<String, Object>() { });
```

you CAN NOT use subtype of a type variable, so this DOES NOT compile, unfortunately:

```java
    Map<String, List<String> value = mapper.readValue(src, new TypeReference<Map<String, ArrayList<String>>() { });
```

which used to be allowed and actually does work. This is unfortunate, and we are not aware of a way to allow above case.

One thing to note is that this change IS binary-compatible (so anything compiled against 2.9 will still link fine against 2.10), but NOT source-compatible. This means that change should not cause any issues with transitive dependencies; but will cause compilation failure.

#### CBOR: fix for `BigDecimal` encoding

A fix to CBOR encoding of `BigDecimal` values in [dataformats-binary#139](https://github.com/FasterXML/jackson-dataformats-binary/issues/139) is, technically speaking, a compatibility-breaking change.
Since former encoding was incorrect, no option was added to allow producing (or accepting) old format.

#### JDK 8: Module info

```
I just tried to run my tests. When my application server tried to start, I got errors:

 Suppressed: java.lang.RuntimeException: Error scanning entry module-info.class from jar file:/home/ruwen/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.10.0.pr1/jackson-core-2.10.0.pr1.jar
        at org.eclipse.jetty.annotations.AnnotationParser.parseJar(AnnotationParser.java:913)
        at org.eclipse.jetty.annotations.AnnotationParser.parse(AnnotationParser.java:831)
        at org.eclipse.jetty.annotations.AnnotationConfiguration$ParserTask.call(AnnotationConfiguration.java:163)
        at org.eclipse.jetty.annotations.AnnotationConfiguration$1.run(AnnotationConfiguration.java:548)
        at org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:635)
        at org.eclipse.jetty.util.thread.QueuedThreadPool$3.run(QueuedThreadPool.java:555)
        at java.lang.Thread.run(Thread.java:748)
    Caused by: java.lang.IllegalArgumentException
        at org.objectweb.asm.ClassReader.<init>(Unknown Source)
        at org.eclipse.jetty.annotations.AnnotationParser.scanClass(AnnotationParser.java:973)
        at org.eclipse.jetty.annotations.AnnotationParser.parseJarEntry(AnnotationParser.java:956)
        at org.eclipse.jetty.annotations.AnnotationParser.parseJar(AnnotationParser.java:909)
        ... 6 more

11:05 xxx@yyy:~$ java -version
openjdk version "1.8.0_212"
OpenJDK Runtime Environment (build 1.8.0_212-8u212-b03-2~deb9u1-b03)
OpenJDK 64-Bit Server VM (build 25.212-b03, mixed mode)

The blame was on an older version of Jetty. Since this was used only in test, the version was quite old. But other people might get similar problems with byte-code manipulating libraries.     
```

## New Modules, status changes

### Datatype: Eclipse-collection

First official version of the `jackson-datatype-eclipse-collections` (for [Eclipse Collections](https://www.eclipse.org/collections/) was introduced in 2.10 (preview version included since 2.9.5)

## Major features of 2.10

Much of additional functionality mirrors changes that 3.0 developments are making, in form that will get some of the benefits, and should ease migration to 3.0.

### Safe Default typing

Due to numerous CVEs for one specific kind of Polymorphic Deserialization (see [this blog post](https://medium.com/@cowtowncoder/on-jackson-cves-dont-panic-here-is-what-you-need-to-know-54cd0d6e8062) for details), block-list approach has proven insufficient to prevent issues.
As a result, [#2195](../../jackson-databind/issues/2195) -- add abstraction `PolymorphicTypeValidator`, for limiting allowed polymorphic subtypes -- was added.

### Builder-based construction of streaming factories and `ObjectMapper`

Although full moved to immutable factories, mappers, by (only) using builder-style construction will only be included in 3.0, 2.10 does introduce builder-pattern itself (but does not force strict immutability beyond what 2.x API supports). The main benefits of this addition are:

* Easier migration to 3.0 since change to builder-based construction can be made in 2.10 already
* More convenient configurability; ability to expose format-specific configuration beyond simple on/off features

Example of builder style:

```java
JsonFactory f = JsonFactory.builder()
    // Change per-factory setting to prevent use of `String.intern()` on symbols
    .disable(JsonFactory.Feature.INTERN_FIELD_NAMES, false)
    // Disable json-specific setting to produce non-standard "unquoted" field names:
    .disable(JsonWriteFeature.QUOTE_FIELD_NAMES, true)
    .build();
ObjectMapper mapper = JsonMapper.builder(f)
    .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)    
    .build();
```

### Separation of general / JSON-specific features

Another change geared towards 3.0 compatibility is the creation of 4 new format feature types, to replace 2 existing features (`JsonParser.Feature`, `JsonGenerator.Feature`):

* `StreamReadFeature`: format-agnostic (general) features for stream readers (parsers)
* `StreamWriteFeature`: format-agnostic (general) features for stream writers (generators)
* `JsonReadFeature`: JSON-specific stream reader (parser) features
* `JsonWriteFeature`: JSON-specific stream writer (generator) features

For 2.10 these features will act as aliases to matching `JsonParser.Feature`s and `JsonGenerator.Feature`s: in 3.0 they will be the only stream-level features (`JsonParser.Feature`s, `JsonGenerator.Feature` being removed)

Example of using new configuration features:

```java
JsonFactory f = JsonFactory.builder()
    .disable(JsonWriteFeature.QUOTE_FIELD_NAMES, true)
    .build();
```

### JDK 9 Module info

All standard components will include `module-info.class`, generated using Moditect Maven plug-in.
Resulting jars will therefore have proper module information for Java 9 and beyond but still be usable on JDK 7 (or, in case of `jackson-core` and `jackson-annotations`, JDK 6)

### "Most Improved" non-core modules

Beyond core functionality most modules got some updates; here's a sampling of most notable

#### Jackson-jr

Root value sequence reading; reading typed `Map`s; ability to plug-in custom `ValueReader`s and `ValueWriter`s (similar to databind `JsonSerializer`, `JsonDeserializer`

#### `jackson-dataformat-xml`

A bunch of fixes, plus support of XML Schema `xsi:nil` concept.

-----

## Change list

### Changes, core

#### [Annotations](../../jackson-annotations)

* [#138](../../jackson-annotations/issues/138): Add basic Java 9+ module info
* [#141](../../jackson-annotations/issues/141): Add `JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES`
* [#159](../../jackson-annotations/issues/159): Add `JsonFormat.Shape.BINARY`

#### [Streaming](../../jackson-core)

* [#433](../../jackson-core/issues/433): Add Builder pattern for creating configured Stream factories
* [#464](../../jackson-core/issues/464): Add "maximum unescaped char" configuration option for `JsonFactory` via builder
* [#467](../../jackson-core/issues/467): Create `JsonReadFeature` to move JSON-specific `JsonParser.Feature`s to
* [#479](../../jackson-core/issues/479): Improve thread-safety of buffer recycling
* [#480](../../jackson-core/issues/480): `SerializableString` value can not directly render to Writer
* [#481](../../jackson-core/issues/481): Create `JsonWriteFeature` to move JSON-specific `JsonGenerator.Feature`s to
* [#484](../../jackson-core/issues/484): Implement `UTF8JsonGenerator.writeRawValue(SerializableString)` (and
  `writeRaw(..)`) more efficiently
* [#495](../../jackson-core/issues/495): Create `StreamReadFeature` to move non-json specific `JsonParser.Feature`s to
* [#496](../../jackson-core/issues/496): Create `StreamWriteFeature` to take over non-json-specific `JsonGenerator.Feature`s
* [#502](../../jackson-core/issues/502): Make `DefaultPrettyPrinter.createInstance()` to fail for sub-classes
* [#506](../../jackson-core/issues/506): Add missing type parameter for `TypeReference` in `ObjectCodec`
* [#508](../../jackson-core/issues/508): Add new exception type `InputCoercionException` to be used for failed coercions like overflow for `int`
* [#517](../../jackson-core/issues/517): Add `JsonGenerator.writeStartObject(Object, int)` (needed by CBOR, maybe Avro)
* [#527](../../jackson-core/issues/527): Add simple module-info for JDK9+, using Moditect
* [#533](../../jackson-core/issues/533): UTF-8 BOM not accounted for in JsonLocation.getByteOffset()
* [#539](../../jackson-core/issues/539): Reduce max size of recycled byte[]/char[] blocks by `TextBuffer`, `ByteArrayBuilder`
* [#547](../../jackson-core/issues/547): `CharsToNameCanonicalizer`: Internal error on `SymbolTable.rehash()` with high number of hash collisions
* [#548](../../jackson-core/issues/548): ByteQuadsCanonicalizer: ArrayIndexOutOfBoundsException in addName
* [#549](../../jackson-core/issues/549): Add configurability of "quote character" for JSON factory
* [#561](../../jackson-core/issues/561): Misleading exception for unquoted String parsing
* [#563](../../jackson-core/issues/563): Async parser does not keep track of Array context properly

#### [Databind](../../jackson-databind)

* [#18](../../jackson-databind/issues/18): Make `JsonNode` serializable
* [#1675](../../jackson-databind/issues/1675): Remove "impossible" `IOException` in `readTree()` and `readValue()` `ObjectMapper` methods which accept Strings
* [#1954](../../jackson-databind/issues/1954): Add Builder pattern for creating configured `ObjectMapper` instances
    * also add `JsonMapper` as explicit type, through which builders are created and that exposes JSON-specific configuration
* [#1995](../../jackson-databind/issues/1995): Limit size of `DeserializerCache`, auto-flush on exceeding
* [#2059](../../jackson-databind/issues/2059): Remove `final` modifier for `TypeFactory`
* [#2077](../../jackson-databind/issues/2077): `JsonTypeInfo` with a subtype having `JsonFormat.Shape.ARRAY` and no fields generates `{}` not `[]`
* [#2115](../../jackson-databind/issues/2115): Support naive deserialization of `Serializable` values as "untyped", same as `java.lang.Object`
* [#2116](../../jackson-databind/issues/2116): Make NumberSerializers.Base public and its inherited classes not final
* [#2126](../../jackson-databind/issues/2126): `DeserializationContext.instantiationException()` throws `InvalidDefinitionException`
* [#2129](../../jackson-databind/issues/2129): Add `SerializationFeature.WRITE_ENUM_KEYS_USING_INDEX`, separate from value setting
* [#2149](../../jackson-databind/issues/2149): Add `MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES`
* [#2153](../../jackson-databind/issues/2153): Add `JsonMapper` to replace generic `ObjectMapper` usage
* [#2187](../../jackson-databind/issues/2187): Make `JsonNode.toString()` use shared `ObjectMapper` to produce valid json
* [#2195](../../jackson-databind/issues/2195): Add abstraction `PolymorphicTypeValidator`, for limiting subtypes allowed by default typing, `@JsonTypeInfo`
* [#2204](../../jackson-databind/issues/2204): Add `JsonNode.isEmpty()` as convenience alias
* [#2211](../../jackson-databind/issues/2211): Change of behavior (2.8 -> 2.9) with `ObjectMapper.readTree(input)` with no content
* [#2220](../../jackson-databind/issues/2220): Force serialization always for `convertValue()`; avoid short-cuts
* [#2223](../../jackson-databind/issues/2223): Add `missingNode()` method in `JsonNodeFactory`
* [#2230](../../jackson-databind/issues/2230):  `WRITE_BIGDECIMAL_AS_PLAIN` is ignored if `@JsonFormat` is used
* [#2236](../../jackson-databind/issues/2236): Type id not provided on `Double.NaN`, `Infinity` with `@JsonTypeInfo`
* [#2237](../../jackson-databind/issues/2237): JsonNode improvements: "required"
* [#2241](../../jackson-databind/issues/2241): Add `PropertyNamingStrategy.LOWER_DOT_CASE` for dot-delimited names
* [#2273](../../jackson-databind/issues/2273): Add basic Java 9+ module info
* [#2309](../../jackson-databind/issues/2309): READ_ENUMS_USING_TO_STRING doesn't support null values
* [#2331](../../jackson-databind/issues/2331): `JsonMappingException` through nested getter with generic wildcard return type
* [#2336](../../jackson-databind/issues/2336): `MapDeserializer` can not merge `Map`s with polymorphic values
* [#2348](../../jackson-databind/issues/2348): Add sanity checks for `ObjectMapper.readXXX()` methods
* [#2390](../../jackson-databind/issues/2390): `Iterable` serialization breaks when adding `@JsonFilter` annotation
* [#2392](../../jackson-databind/issues/2392): `BeanDeserializerModifier.modifyDeserializer()` not applied to custom bean deserializers
* [#2393](../../jackson-databind/issues/2393): `TreeTraversingParser.getLongValue()` incorrectly checks `canConvertToInt()`
* [#2398](../../jackson-databind/issues/2398): Replace recursion in `TokenBuffer.copyCurrentStructure()` with iteration
* [#2415](../../jackson-databind/issues/2415): Builder-based POJO deserializer should pass builder instance, not type, to `handleUnknownVanilla()`
* [#2416](../../jackson-databind/issues/2416): Optimize `ValueInstantiator` construction for default `Collection`, `Map` types
* [#2422](../../jackson-databind/issues/2422): `scala.collection.immutable.ListMap` fails to serialize since 2.9.3
* [#2424](../../jackson-databind/issues/2424): Add global config override setting for `@JsonFormat.lenient()`
* [#2430](../../jackson-databind/issues/2430): Change `ObjectMapper.valueToTree()` to convert `null` to `NullNode`
* [#2432](../../jackson-databind/issues/2432): Add support for module bundles
* [#2442](../../jackson-databind/issues/2442): `ArrayNode.addAll()` adds raw `null` values which cause NPE on `deepCopy()`
* [#2458](../../jackson-databind/issues/2458): `Nulls` property metadata ignored for creators
* [#2467](../../jackson-databind/issues/2467): Accept `JsonTypeInfo.As.WRAPPER_ARRAY` with no second argument to
deserialize as "null value"

### Changes, data formats

#### Avro

* [#168](../../jackson-dataformats-binary/issues/168): `JsonMappingException` for union types with multiple Record types
* [#174](../../jackson-dataformats-binary/issues/173): Improve Union type serialization performance
* [#177](../../jackson-dataformats-binary/issues/177): Deserialization of "empty" Records as root values fails

#### CBOR

* [#139](../../jackson-dataformats-binary/issues/139): Incorrect `BigDecimal` fraction representation
* [#155](../../jackson-dataformats-binary/issues/155): Inconsistent support for FLUSH_PASSED_TO_STREAM

#### CSV

* [#108](../../jackson-dataformats-text/issues/108): Add new `CsvParser.Feature.ALLOW_COMMENTS` to replace use of deprecated
 `JsonParser.Feature.ALLOW_YAML_COMMENTS`
* [#134](../../jackson-dataformats-text/issues/134): `CSVParserBootstrapper` creates `UTF8Reader` which is incorrectly not auto-closed
 (reported by iuliu-b@github)

#### Properties

* [#100](../../jackson-dataformats-text/issues/100): Add an option to specify properties prefix
* [#139](../../jackson-dataformats-text/issues/139): Support for Map<String, String> in `JavaPropsMapper`

#### Protobuf

* [#148](../../jackson-dataformats-binary/issues/148): Add `ProtobufMapper.generateSchemaFor(TypeReference<?>)` overload

#### XML

* [#242](../../jackson-dataformat-xml/issues/242): Deserialization of class inheritance depends on attributes order
* [#336](../../jackson-dataformat-xml/issues/336): WRITE_BIGDECIMAL_AS_PLAIN Not Used When Writing Pretty
* [#350](../../jackson-dataformat-xml/issues/350): Wrap Xerces/Stax (JDK-bundled) exceptions during parser initialization
* [#351](../../jackson-dataformat-xml/issues/351): XmlBeanSerializer serializes AnyGetters field even with FilterExceptFilter
* [#354](../../jackson-dataformat-xml/issues/354): Support mapping `xsi:nil` marked elements as `null`s (`JsonToken.VALUE_NULL`)

#### YAML

* [#50](../../jackson-dataformats-text/issues/50): Empty string serialized without quotes if MINIMIZE_QUOTES is enabled
* [#101](../../jackson-dataformats-text/issues/101): Use latest SnakeYAML version 1.24 and get rid of deprecated methods
* [#116](../../jackson-dataformats-text/issues/116): Error handling "null" String when Feature.MINIMIZE_QUOTES is active
* [#129](../../jackson-dataformats-text/issues/129): Convert YAML string issue
* [#140](../../jackson-dataformats-text/issues/140): Implement `JsonGenerator.writeFieldId(...)` for `YAMLGenerator`

### Changes, datatypes

#### [Collections](../../jackson-datatypes-collections)

* [#34](../../jackson-datatypes-collections/issues/34): (guava) Allow use of Guava versions up to 25 (from 22)
* [#37](../../jackson-datatypes-collections/issues/37): (eclipse-collections) Implement pair deserialization
* [#48](../../jackson-datatypes-collections/issues/48): (all) Add simple module-info for JDK9+, using Moditect
* [#50](../../jackson-datatypes-collections/issues/50): (guava) Add Serializer and Deserializer for `RangeSet`
* [#53](../../jackson-datatypes-collections/issues/53): (guava) GuavaImmutableCollectionDeserializer` cannot deserialize an empty `Optional` from null
* [#56](../../jackson-datatypes-collections/issues/56): (guava) Range property name (de)serialisation doesn't respect property naming strategy
* [#58](../../jackson-datatypes-collections/issues/58): (guava) Drop support for Guava v10 - v13 to simplify `RangeFactory`

#### [Hibernate](../../jackson-datatype-hibernate)

* [#125](../../jackson-datatype-hibernate/issues/125): Ignore missing entities when lazy loading is enabled, if newly added `Feature.WRITE_MISSING_ENTITIES_AS_NULL` is enabled

### Changes, JVM languages

#### [Kotlin](../../jackson-module-kotlin)

* [#239](../../jackson-module-kotlin/issues/239): Auto-detect sealed classes (similar to `@JsonSubTypes`)

#### [Scala](../../jackson-module-scala)

Full support for Scala 2.13 (and still supports 2.10, 2.11, 2.12).
Fixes from [2.9.10](Jackson-release-2.9.10) merged, plus:

* (via `databind#2422`) `scala.collection.immutable.ListMap` fails to serialize since 2.9.3

### Changes, other modules

#### Java 8 support

* [#51](../../jackson-modules-java8/issues/51): (datetime) `YearKeyDeserializer` doesn't work with non-padded year values 
* [#69](../../jackson-modules-java8/issues/69): (datetime) `ZonedDateTime` for times before the epoch do not serialize correctly
* [#75](../../jackson-modules-java8/issues/75): (datetime) Use `SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS` for configuring `Duration` serialization
* [#82](../../jackson-modules-java8/issues/82): (datetime) Typo in YearMothKeyDeserializer class name
* [#105](../../jackson-modules-java8/issues105/): `LocalTime` should generate "time" schema instead of "date-time"
* [#121](../../jackson-modules-java8/issues/121): Array representation of `MonthDay` can not be deserialized
* [#126](../../jackson-modules-java8/issues/126): Change auto-registration in 2.10 to provide "new" (JavaTimeModule) instead of legacy module
* [#129](../../jackson-modules-java8/issues/129): Support `lenient` setting with `LocalDateDeserializer`

#### Mr Bean

* [#52](../../jackson-modules-base/issues/52): Interfaces may have non-abstract methods (since java8)

### Changes, JAX-RS

#### JAX-RS, base

* [#111](../../jackson-jaxrs-providers/issues/111): AnnotationBundleKey equality fails for Parameter Annotations

### Changes, jackson-jr

* [#60](../../jackson-jr/issues/60): Add support for reading "root value" streams (linefeed separated/concatenated)
* [#63](../../jackson-jr/issues/63): Change default for `JSON.Feature.USE_FIELDS` to `true` (from false) in 2.10
* [#65](../../jackson-jr/issues/65): Allow registration of custom readers, writers (to support 3rd party, custom types)
* [#66](../../jackson-jr/issues/66): Add `Json.mapOfFrom(Class)` to support binding POJO-valued maps
