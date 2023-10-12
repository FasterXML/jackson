Version 2.11 of Jackson was released on April 26, 2020.

This wiki page gives a list of links to all changes (with brief descriptions); there is also [Jackson 2.11 features](https://medium.com/@cowtowncoder/jackson-2-11-features-40cdc1d2bdf3) blog post that covers some of the highlights.

## Status

Branch is still open for new minor versions as of March 2020, but it is likely that 2.11.4 will be the last full patch set released.

## Patches

Beyond initial 2.11.0 (described here), following patch releases have been made:

* [2.11.1](Jackson-Release-2.11.1) (25-Jun-2020)
* [2.11.2](Jackson-Release-2.11.2) (02-Aug-2020)
* [2.11.3](Jackson-Release-2.11.3) (02-Oct-2020)
* [2.11.4](Jackson-Release-2.11.4) (12-Dec-2020)

## Micro-patches

None released yet.

## Documentation

* [Jackson 2.11 features](https://medium.com/@cowtowncoder/jackson-2-11-features-40cdc1d2bdf3)

## Changes, JDK requirements

No changes to minimum JDK baselines for use since [2.10](Jackson-Release-2.10)

JDK 8 is required to build all components, however, as module info inclusion plug-in requires it (note: publishing to Maven Central also requires JDK 8), but runtime environment of JDK/JVM 7 is needed with exception of:

* `jackson-annotations`, `jackson-core`, `jackson-jr` only require JDK/JVM 6
* Kotlin, Scala and Java 8 modules require JDK/JVM 8 or higher

## Changes, behavior

* Default serialization ordering now considers `@JsonProperty(index = )` (see `databind#2555` below)
* `Avro` format backend now identifies as capable of embedding binary data: will change schema type indicated for `byte[]`, `java.util.UUID` (see `dataformats-binary/avro#179` below)
* Timezone offset in default `java.util.Date`, `java.util.Calendar` serialization will now include colon (like `+00:00`) by default (see `databind#2643` below)

## New Modules, status changes

* New datatype module -- `jackson-datatype-joda-money` for [Joda Money](https://www.joda.org/joda-money/) datatypes
    * Added as part of multi-project [Jackson Misc Datatypes](https://github.com/FasterXML/jackson-datatypes-misc) (https://github.com/FasterXML/jackson-datatypes-misc) repo
    * Contributed by Iurii Ignatko
* `jackson-jr` now has one more component, `jr-annotation-support` (see issue `jackson-jr#32` below)

## Major features of 2.11

### Improve handling of `java.util.UUID`, binary types

Need to properly serialize/deserialize `UUID`s in Avro was determined late, but will also affect Protobuf.
Beyond more compact binary representation, generating usable schema, it would be nice to also allow Shape
overrides in case textual representation is needed.

### More support for safe Polymorphic deserialization

New `MapperFeature`, `BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES` added (see [#2587](../../jackson-databind/issues/2587)): if enabled, will impose limits on allowed base types for:

1. Legacy Default Typing enabling methods (`ObjectMapper.enableDefaultTyping()`), which, while deprecated may still be in use
2. Polymorphic deserialization using `@JsonTypeInfo` with class name as is, without explicitly configured `PolymorphicTypeValidator`

This is basically a one-line addition that can force safe defaults, similar to ones that Jackson 3.0 will have by default: overridable as needed, but strict(er) by default.

```
ObjectMapper mapper = JsonMapper.builder()
    .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
    .build();
```

### Jackson-jr support for (some of) core Jackson annotations

With new `jr-annotation-support` component (`JacksonAnnotationExtension`), it is now possible use some of core Jackson annotations for changing basic property inclusion/ignoral and naming rules.
See `jackson-jr#32` below for details; basic usage is by registering extension like so:

```
JSON j = JSON.builder().register(JacksonAnnotationExtension.std).build();
MyValue v = j.beanFrom(source, MyValue.class);
```

## Significant other features of 2.11

Databind:

* `@JsonAlias` works for Enums ([2352](../../jackson-databind/issues/2352))
* `@JsonSerialize(keyUsing)` and `@JsonDeserialize(keyUsing)` work on (Map) key class ([#2503](../../jackson-databind/issues/2503))

-----

## Full Change list

### Changes, core

#### [Annotations](../../jackson-annotations)

No changes since 2.10.

#### [Streaming](../../jackson-core)

* [#504](../../jackson-core/issues/504): Add a String Array write method in the Streaming API
* [#565](../../jackson-core/issues/565): Synchronize variants of `JsonGenerator#writeNumberField` with `JsonGenerator#writeNumber`
* [#587](../../jackson-core/issues/587): Add JsonGenerator#writeNumber(char[], int, int) method
* [#606](../../jackson-core/issues/606): Do not clear aggregated contents of `TextBuffer` when `releaseBuffers()` called
* [#609](../../jackson-core/issues/609): `FilteringGeneratorDelegate` does not handle `writeString(Reader, int)`
* [#611](../../jackson-core/issues/611): Optionally allow leading decimal in float tokens

#### [Databind](../../jackson-databind)

* [#953](../../jackson-databind/issues/953): i-I case conversion problem in Turkish locale with case-insensitive deserialization
* [#962](../../jackson-databind/issues/962): `@JsonInject` fails on trying to find deserializer even if inject-only
* [#1983](../../jackson-databind/issues/1983): Polymorphic deserialization should handle case-insensitive Type Id property name if `MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES` is enabled
* [#2049](../../jackson-databind/issues/2049): `TreeTraversingParser` and `UTF8StreamJsonParser` create contexts differently
* [#2352](../../jackson-databind/issues/2352): Support use of `@JsonAlias` for enum values
* [#2365](../../jackson-databind/issues/2365): `declaringClass` of "enum-as-POJO" not removed for `ObjectMapper` with a naming strategy
* [#2480](../../jackson-databind/issues/2480): Fix `JavaType.isEnumType()` to support sub-classes
* [#2487](../../jackson-databind/issues/2487): `BeanDeserializerBuilder` Protected Factory Method for Extension
* [#2503](../../jackson-databind/issues/2503): Support `@JsonSerialize(keyUsing)` and `@JsonDeserialize(keyUsing)` on Key class
* [#2511](../../jackson-databind/issues/2511): Add `SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL`
* [#2515](../../jackson-databind/issues/2515): `ObjectMapper.registerSubtypes(NamedType...)` doesn't allow to register the same POJO for two different type ids
* [#2522](../../jackson-databind/issues/2522): `DeserializationContext.handleMissingInstantiator()` throws `MismatchedInputException` for non-static inner classes
* [#2525](../../jackson-databind/issues/2525): Incorrect `JsonStreamContext` for `TokenBuffer` and `TreeTraversingParser`
* [#2527](../../jackson-databind/issues/2527): Add `AnnotationIntrospector.findRenameByField()` to support Kotlin's "is-getter" naming convention
* [#2555](../../jackson-databind/issues/2555): Use `@JsonProperty(index)` for sorting properties on serialization
* [#2565](../../jackson-databind/issues/2565): Java 8 `Optional` not working with `@JsonUnwrapped` on unwrappable type
* [#2587](../../jackson-databind/issues/2587): Add `MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES` to allow blocking use of unsafe base type for polymorphic deserialization
* [#2589](../../jackson-databind/issues/2589): `DOMDeserializer`: setExpandEntityReferences(false) may not prevent external entity expansion in all cases [CVE-2020-25649]
* [#2592](../../jackson-databind/issues/2592): `ObjectMapper.setSerializationInclusion()` is ignored for `JsonAnyGetter`
* [#2608](../../jackson-databind/issues/2608): `ValueInstantiationException` when deserializing using a builder and `UNWRAP_SINGLE_VALUE_ARRAYS`
* [#2627](../../jackson-databind/issues/2627): JsonIgnoreProperties(ignoreUnknown = true) does not work on field and method level
* [#2632](../../jackson-databind/issues/2632): Failure to resolve generic type parameters on serialization
* [#2636](../../jackson-databind/issues/2636): ObjectReader readValue lacks Class<T> argument
* [#2643](../../jackson-databind/issues/2643): Change default textual serialization of `java.util.Date`/`Calendar` to include colon in timezone offset
* [#2647](../../jackson-databind/issues/2647): Add `ObjectMapper.createParser()` and `createGenerator()` methods
* [#2657](../../jackson-databind/issues/2657): Allow serialization of `Properties` with non-String values
* [#2663](../../jackson-databind/issues/2663): Add new factory method for creating custom `EnumValues` to pass to `EnumDeserializer
* [#2668](../../jackson-databind/issues/2668): `IllegalArgumentException` thrown for mismatched subclass deserialization
* [#2693](../../jackson-databind/issues/2693): Add convenience methods for creating `List`, `Map` valued `ObjectReader`s (ObjectMapper.readerForListOf())

### Changes, data formats

#### [Avro](../../jackson-dataformats-binary/)

* [#179](../../jackson-dataformats-binary/issues/179): Add `AvroGenerator.canWriteBinaryNatively()` to support binary writes, fix `java.util.UUID` representation
* [#195](../../jackson-dataformats-binary/issues/195): Remove dependencies upon Jackson 1.X and Avro's JacksonUtils

#### [CBOR](../../jackson-dataformats-binary/)

* [#201](../../jackson-dataformats-binary/issues/201): `CBORGenerator.Feature.WRITE_MINIMAL_INTS` does not write most compact form for all integers

#### [CSV](../../jackson-dataformats-text/)

* [#7](../../jackson-dataformats-text/issues/7): Add `CsvParser.Feature.EMPTY_STRING_AS_NULL` to allow coercing empty Strings into `null` values
* [#115](../../jackson-dataformats-text/issues/115): JsonProperty index is not honored by CsvSchema builder
(actually fixed by `databind#2555`)
* [#174](../../jackson-dataformats-text/issues/174): `CsvParser.Feature.SKIP_EMPTY_LINES` results in a mapping error
* [#191](../../jackson-dataformats-text/issues/191): `ArrayIndexOutOfBoundsException` when skipping empty lines, comments
* [#195](../../jackson-dataformats-text/issues/195): Add schema creating csv schema with View

#### [Ion](../../jackson-dataformats-binary/)

* [#192](../../jackson-dataformats-binary/pull/192): Allow `IonObjectMapper` with class name annotation introspector to deserialize generic subtypes

#### [YAML](../../jackson-dataformats-text/)

* [#180](../../jackson-dataformats-text/issues/180): YAMLGenerator serializes string with special chars unquoted when using `MINIMIZE_QUOTES` mode

### Changes, datatypes

#### [Java 8 date/time](../../jackson-modules-java8/datetime)

* [#58](../../jackson-modules-java8/issues/58): Should not parse `LocalDate`s from number (timestamp), or at least should have an option preventing
* [#128](../../jackson-modules-java8/issues/128): Timestamp keys from `ZonedDateTime`
* [#138](../../jackson-modules-java8/issues/138): Prevent deserialization of "" as `null` for `Duration`, `Instant`, `LocalTime`, `OffsetTime` and `YearMonth` in "strict" (non-lenient) mode
* [#148](../../jackson-modules-java8/issues/148): Allow strict `LocalDate` parsing
* Add explicit `ZoneId` serializer to force use of `ZoneId` as Type Id, and not inaccessible subtype (`ZoneRegion`): this to avoid JDK9+ Module Access problem

#### [Joda)](../../jackson-datatype-joda)

* [#104](../../jackson-datatype-joda/issues/104): Deserializing Interval discards timezone information

#### [JSR-353 (JSON-P)](../../jackson-datatype-jsr353)

* [#13](../../jackson-datatype-jsr353/issues/13): Support for `JsonPatch` and `JsonMergePatch` defined in JSON-P 1.1

### Changes, JVM Languages

#### [Kotlin](../../jackson-module-kotlin)

* [#281](../../jackson-module-kotlin/issues/281): Hide singleton deserialization support behind a setting on the module, `singletonSupport` and enum `SingletonSupport`.  Defaults to pre-2.10 behavior.
* [#284](../../jackson-module-kotlin/issues/284): Use `AnnotationIntrospector.findRenameByField()` to support "is properties"
* [#321](../../jackson-module-kotlin/issues/321): `MissingKotlinParameterException` should extend `MismatchedInputException`
* Add Builder for KotlinModule

#### [Scala](../../jackson-module-scala)

* [#87](../../../../jackson-module-scala/87): Support for default parameter values

### Changes, other

#### [jackson-jr](../../jackson-jr)

* [#32](../../jackson-jr/issues/32): Add support for subset of jackson annotations
* [#70](../../jackson-jr/issues/70): Add extension point (`ReaderWriterModifier`) to allow more customization of
POJO readers, writers


