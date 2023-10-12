[Jackson Version](Jackson-Releases) 2.14 was released on November 5, 2022. Three release candidates (2.14.0-rc1, -rc2 and -rc3) were released prior to the final 2.14.0.

This wiki page gives a list of links to all changes (with brief descriptions) that are included, as well as about original plans for bigger changes (and in some cases changes to plans, postponing).

## Status

Branch is open (as of April 2023) and new patch releases are expected.

## Patches

* [2.14.1](Jackson-Release-2.14.1) (21-Nov-2022)
* [2.14.2](Jackson-Release-2.14.2) (28-Jan-2023)
* [2.14.3](Jackson-Release-2.14.3) (05-May-2023)
* [2.14.4](Jackson-Release-2.14.4) (not yet released)

## Documentation

### Blog posts, artcles

* [Jackson 2.14 sneak peek](https://cowtowncoder.medium.com/jackson-2-14-sneak-peek-79859babaa4) -- preview of 2.14 features before release

## Changes, compatibility

### Compatibility: JDK requirements

JDK baseline has been raised to Java 8 for `jackson-core` and `jackson-jr` components, leaving `jackson-annotations` the only component that only requires Java 6: all other components already required Java 8.

### Compatibility: min Android SDK

Due to changes after 2.13 (see [databind#3412](../../jackson-databind/pull/3412)) the minimum Android SDK version required is now 26
(released in 2017; see https://en.wikipedia.org/wiki/Android_version_history for details).

Older Android SDK versions are still supported by Jackson 2.13 (TODO: figure out exact supported Android SDK version).

## Changes, behavior

### Planned changes

None

### Observed/Reported changes

* Handling of conflicting/ambiguous `@JsonIgnore`/`@JsonProperty` annotations for a single property (but possible across inheritance hierarchy, different accessors) changed with [databind#3357](../../jackson-databind/issues/3357)
    * Reported as [#3722](../../jackson-databind/issues/3722)
    * Formerly (2.13.x and before) `@JsonIgnore` would have been often ignored giving `@JsonProperty` priority; with 2.14 active `@JsonIgnore` will have predence ("ignoral wins")

-----

## Major focus areas planned

### Configurability ideas

Separate configuration settings for `JsonNode`, `Enum`, Date/Time.

NOTE: Covered in-detail here:

* https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-7

-----

## Major focus areas planned -- but postponed due to lack of time

Unfortunately 2 major features were again postponed from 2.14 due to other work.

### Rewrite Creator Detection wrt Property Discovery (postponed already from 2.13)

A class of impossible-to-fix problems related to Creator methods (constructors, factory methods) is due to "duality" of Creator discovery and General property discovery. Problem is that the process goes like this:

1. General property discovery is performance by `POJOPropertiesCollector`: this is based on finding regular accessors (Fields, Getter/Setter methods) using both name-based auto-discovery and annotations, but also includes (annotated) Creator property parameters -- but notably not parameters of possible implicit (auto-discovered) Constructors. Accessors are combined into logical properties, expressed as (and accessed through) `BasicBeanDescription` container.
2. `BeanDeserializerFactory` takes `BasicBeanDescription` and introspect possible Creators: this starts from scratch and finds potential Creators both by explicit annotations and possible auto-discovery. It will also try to combine already known Properties (from step 1) with now-located Creator parameters.

... and the problem is that "implicit" Creators -- ones with no explicitly annotated parameters (note: Not related to annotation of Creators method itself, but to parameter annotations!) -- will not be known during step (1) and as such will:

* Not be renamed by `PropertyNamingStrategy`
* Not get annotations from related accessors (normally annotation in one of accessors is essentially applied to all), nor contribute annotations to be used for others

Fixing this general problem by either:

1. Moving Creator discovery as part of `POJOPropertiesCollector` (preferred), or
2. Trying to combine pieces in step 2 more throughly (could resolve some of the issues)

would fix some of existing failing tests, and in particular help with newly found problems with `Record` handling (added in 2.12).

### Processing Limits

Mentioned as one future JSTEP on https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP, there really should be limits to maximum size and/or complexity of input to process, mostly to prevent potential DoS attacks (as well as accidental "intern brought down our system by broken script" cases). There is some prior art in Woodstox, for example (see [Woodstox-specific settings ("limits")](https://cowtowncoder.medium.com/configuring-woodstox-xml-parser-woodstox-specific-properties-1ce5030a5173)).

NOTE: these limits are becoming more and more important over time -- a few DoS style issues have been resolved, but eventually it'd be good to have such "guard rails" built in core processing, as a baseline protection.

-----

Changes included in the release

## Full Change list

### Changes, core

#### [Annotations](../../jackson-annotations)

* [#204](../../jackson-annotations/issues/204): Allow explicit `JsonSubTypes` repeated names check

#### [Streaming](../../jackson-core)

* [#478](../../jackson-core/issues/478): Provide implementation of async JSON parser fed by `ByteBufferFeeder`
* [#577](../../jackson-core/issues/577): Allow use of faster floating-point number parsing (Schubfach) with `StreamReadFeature.USE_FAST_DOUBLE_PARSER`
* [#684](../../jackson-core/issues/684): Add "JsonPointer#appendProperty" and "JsonPointer#appendIndex"
* [#715](../../jackson-core/issues/715): Allow `TokenFilter`s to keep empty arrays and objects
* [#717](../../jackson-core/issues/717): Hex capitalization for JsonWriter should be configurable (add `JsonWriteFeature.WRITE_HEX_UPPER_CASE`)
* [#733](../../jackson-core/issues/733): Add `StreamReadCapability.EXACT_FLOATS` to indicate whether parser reports exact floating-point values
* [#736](../../jackson-core/pull/736): `JsonPointer` quadratic memory use: OOME on deep inputs
* [#745](../../jackson-core/pull/745): Change minimum Java version to 8
* [#749](../../jackson-core/pull/749): Allow use of faster floating-point number serialization (`StreamWriteFeature.USE_FAST_DOUBLE_WRITER`)
* [#751](../../jackson-core/issues/751): Remove workaround for old issue with a particular double
* [#753](../../jackson-core/issues/753): Add `NumberInput.parseFloat()`
* [#762](../../jackson-core/pull/762): Make `JsonPointer` `java.io.Serializable`
* [#763](../../jackson-core/issues/763): `JsonFactory.createParser()` with `File` may leak `InputStream`s
* [#764](../../jackson-core/issues/764): `JsonFactory.createGenerator()` with `File` may leak `OutputStream`s
* [#773](../../jackson-core/pull/773): Add option to accept non-standard trailing decimal point (`JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS`)
* [#774](../../jackson-core/pull/774): Add a feature to allow leading plus sign (`JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS`)
* [#788](../../jackson-core/pull/788): `JsonPointer.empty()` should NOT indicate match of a property with key of ""
* [#798](../../jackson-core/pull/798): Avoid copy when parsing `BigDecimal`
* [#811](../../jackson-core/pull/811): Add explicit bounds checks for `JsonGenerator` methods that take `byte[]`/`char[]`/String-with-offsets input
* [#814](../../jackson-core/pull/814): Use `BigDecimalParser` for BigInteger parsing very long numbers
* [#818](../../jackson-core/pull/818): Calling `JsonPointer.compile(...)` on very deeply nested expression throws `StackOverflowErrror`
* [#828](../../jackson-core/pull/828): Make `BigInteger` parsing lazy
* [#830](../../jackson-core/pull/830): Make `BigDecimal` parsing lazy

#### [Databind](../../jackson-databind)

* [#1980](../../jackson-databind/issues/1980): Add method(s) in `JsonNode` that works like combination of `at()` and `with()`: `withObject(...)` and `withArray(...)`
* [#2541](../../jackson-databind/issues/2541): Cannot merge polymorphic objects
* [#3013](../../jackson-databind/issues/3013): Allow disabling Integer to String coercion via `CoercionConfig`
* [#3212](../../jackson-databind/issues/3212): Add method `ObjectMapper.copyWith(JsonFactory)`
* [#3311](../../jackson-databind/issues/3311): Add serializer-cache size limit to avoid Metaspace issues from caching Serializers
* [#3338](../../jackson-databind/issues/3338): `configOverride.setMergeable(false)` not supported by `ArrayNode`
* [#3357](../../jackson-databind/issues/3357): `@JsonIgnore` does not work if together with `@JsonProperty` or `@JsonFormat`
* [#3373](../../jackson-databind/issues/3373): Change `TypeSerializerBase` to skip `generator.writeTypePrefix()` for `null` typeId
* [#3394](../../jackson-databind/issues/3394): Allow use of `JsonNode` field for `@JsonAnySetter`
* [#3405](../../jackson-databind/issues/3405): Create `DataTypeFeature` abstraction (for JSTEP-7) with placeholder features
* [#3417](../../jackson-databind/issues/3417): Allow (de)serializing records using Bean(De)SerializerModifier even when reflection is unavailable
* [#3419](../../jackson-databind/issues/3419): Improve performance of `UnresolvedForwardReference` for forward reference resolution
* [#3421](../../jackson-databind/issues/3421): Implement `JsonNodeFeature.READ_NULL_PROPERTIES` to allow skipping of JSON `null` values on reading
* [#3443](../../jackson-databind/issues/3443): Do not strip generic type from `Class<C>` when resolving `JavaType`
* [#3447](../../jackson-databind/issues/3447): Deeply nested JsonNode throws StackOverflowError for toString()
* [#3475](../../jackson-databind/issues/3475): Support use of fast double parser
* [#3476](../../jackson-databind/issues/3476): Implement `JsonNodeFeature.WRITE_NULL_PROPERTIES` to allow skipping JSON `null` values on writing
* [#3481](../../jackson-databind/issues/3481): Filter method only got called once if the field is null when using `@JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = SomeFieldFilter.class)`
* [#3484](../../jackson-databind/issues/3484): Update `MapDeserializer` to support `StreamReadCapability.DUPLICATE_PROPERTIES`
* [#3497](../../jackson-databind/issues/3497): Deserialization of Throwables with PropertyNamingStrategy does not work
* [#3500](../../jackson-databind/issues/3500): Add optional explicit `JsonSubTypes` repeated names check
* [#3503](../../jackson-databind/issues/3503): `StdDeserializer` coerces ints to floats even if configured to fail
* [#3503](../../jackson-databind/pull/3505): Fix deduction deserializer with `DefaultTypeResolverBuilder`
* [#3528](../../jackson-databind/issues/3528): `TokenBuffer` defaults for parser/stream-read features neither passed from parser nor use real defaults
* [#3530](../../jackson-databind/issues/3530): Change LRUMap to just evict one entry when maxEntries reached
* [#3530](../../jackson-databind/issues/3533): Deserialize missing value of `EXTERNAL_PROPERTY` type using custom `NullValueProvider`
* [#3535](../../jackson-databind/issues/3535): Replace `JsonNode.with()` with `JsonNode.withObject()`
* [#3559](../../jackson-databind/issues/3559): Support `null`-valued `Map` fields with "any setter"
* [#3568](../../jackson-databind/issues/3568): Change `JsonNode.with(String)` and `withArray(String)` to consider argument as `JsonPointer` if valid expression
* [#3590](../../jackson-databind/issues/3590): Add check in primitive value deserializers to avoid deep wrapper array nesting wrt `UNWRAP_SINGLE_VALUE_ARRAYS` [CVE-2022-42003]
* [#3609](../../jackson-databind/issues/3609): Allow non-boolean return type for "is-getters" with `MapperFeature.ALLOW_IS_GETTERS_FOR_NON_BOOLEAN`
* [#3613](../../jackson-databind/issues/3613): Implement `float` and `boolean` to `String` coercion config
* [#3624](../../jackson-databind/issues/3624): Legacy `ALLOW_COERCION_OF_SCALARS` interacts poorly with Integer to Float coercion
* [#3633](../../jackson-databind/issues/3633): Expose `translate()` method of standard `PropertyNamingStrategy` implementations

### Changes, data formats

#### Avro

* [#310](../../jackson-dataformats-binary/issues/310): Avro schema generation: allow override namespace with new `@AvroNamespace` annotation

#### CBOR

* [#301](../../jackson-dataformats-binary/issues/301): Missing configuration methods for format-specific parser/generator features
* [#312](../../jackson-dataformats-binary/issues/312): Short NUL-only keys incorrectly detected as duplicates
* [#338](../../jackson-dataformats-binary/issues/338): Use passed "current value" in `writeStartObject()` overload

#### CSV

* [#285](../../jackson-dataformats-text/issues/285): Missing columns from header line (compare to `CsvSchema`) not detected when reordering columns (add `CsvParser.Feature.FAIL_ON_MISSING_HEADER_COLUMNS`)
* [#297](../../jackson-dataformats-text/issues/297): CSV schema caching POJOs with different views
* [#314](../../jackson-dataformats-text/issues/314): Add fast floating-point parsing, generation support
* [#351](../../jackson-dataformats-text/issues/351): Make CSVDecoder use lazy parsing of BigInteger/BigDecimal

#### Ion

* [#311](../../jackson-dataformats-binary/issues/311): `IonObjectMapper` does not throw JacksonException for some invalid Ion
* [#325](../../jackson-dataformats-binary/pull/325): Ensure `IonReader` instances created within `IonFactory` are always resource-managed

#### Properties

* [#169](../../jackson-dataformats-text/issues/169): Need a way to escape dots in property keys (add path separator configuration)

#### Smile

* [#301](../../jackson-dataformats-binary/issues/301): Missing configuration methods for format-specific parser/generator features
* [#312](../../jackson-dataformats-binary/issues/312): Short NUL-only keys incorrectly detected as duplicates

#### XML

* [#491](../../jackson-dataformat-xml/issues/491): `XmlMapper` 2.12 regression: no default no-arg ctor found
* [#498](../../jackson-dataformat-xml/issues/498): `XmlMapper` fails to parse XML array when the array only has one level
* [#531](../../jackson-dataformat-xml/issues/531): Add mechanism for processing invalid XML names (transforming to valid ones)
* [#538](../../jackson-dataformat-xml/issues/538): Required attribute of `@JsonProperty` is ignored when deserializing from XML
* [#545](../../jackson-dataformat-xml/issues/545): `@JacksonXmlText` does not work when paired with `@JsonRawValue`

#### YAML

* [#244](../../jackson-dataformats-text/issues/244): Add `YAMLGenerator.Feature.ALLOW_LONG_KEYS` to allow writing keys longer than 128 characters (default)
* [#335](../../jackson-dataformats-text/issues/335)/[#346](../../jackson-dataformats-text/issues/346): Update to SnakeYAML 1.33
* [#337](../../jackson-dataformats-text/issues/337): Allow overriding of file size limit for YAMLParser by exposing SnakeYAML `LoaderOptions`
* [#345](../../jackson-dataformats-text/issues/345): Support configuring SnakeYAML DumperOptions directly

### Changes, datatypes

#### [Java 8 date/time](../../jackson-modules-java8/datetime)

* [#224](../../jackson-modules-java8/issues/224): `DurationSerializer` ignores format pattern if nano-second serialization enabled
* [#230](../../jackson-modules-java8/issues/230): Change `LocalDateTimeSerializer` constructor protected from private
* [#240](../../jackson-modules-java8/issues/240): `LocalDateDeserializer` should consider coercionConfig settings
* [#242](../../jackson-modules-java8/issues/242): Fix InstantSerializer ignoring the JsonFormat shape
* [#249](../../jackson-modules-java8/issues/249): `YearMonthDeserializer` fails for year > 9999

#### [Joda Date/time](../../jackson-datatype-joda)

* [#124](../../jackson-datatype-joda/issues/124): Add no-arg constructor for DateTimeDeserializer

#### [Joda Money](../../jackson-datatypes-misc)

* [#17](https://github.com/FasterXML/jackson-datatypes-misc/pull/17): Add configurable amount representations for Joda Money module

#### [JSR-353/JSONP](../../jackson-datatypes-misc)

* [#19](../../jackson-datatypes-misc/issues/19): `JsonValue.NULL` deserialization has different behaviours with constructor properties vs public properties

### Changes, JVM Languages

#### [Kotlin](../../jackson-module-kotlin)

* [#582](../../jackson-module-kotlin/issues/582): Ignore open-ended ranges in `KotlinMixins.kt` (to help with Kotlin 1.7.20+)

#### [Scala](../../jackson-module-scala)

* [#572](../../jackson-module-scala/issues/572): reuse Java code to parse Scala BigDecimal and BigInt
* [#576](../../jackson-module-scala/issues/576): add flag in type cache to track which c
lasses are not Scala (to avoid having to check them again)
* [#590](../../jackson-module-scala/issues/590): support BitSet deserialization (disabled by default) - enabling this is strongly discouraged as BitSets can use a lot of memory
* [#593](../../jackson-module-scala/issues/593): support IntMap/LongMap deserialization
* [#594](../../jackson-module-scala/issues/594): make scala 3.2 the minimum supported scala 3 version
* [#603](../../jackson-module-scala/issues/603): ignore generated methods for defaulted parameters

### Changes, Other modules

#### Blackbird

* [#138](../../jackson-modules-base/issues/138): Blackbird doesn't work on Java 15+
* [#187](../../jackson-modules-base/issues/187): Remove stack trace from Blackbirds warnings wrt missing `MethodHandles.lookup()` (on Java 8)

#### [JDK 8](../../jackson-modules-java8/datetime) (`Optional` etc)

* [#251](../../jackson-modules-java8/issues/251): Allow `Optional` deserialization for "absent" value as Java `null` (like other Reference types), not "empty"

### Changes, other

#### [Jackson-jr](../../jackson-jr)

* [#91](../../jackson-jr/issues/91): Annotation support should allow `@JsonValue`/`JsonCreator` on `enum`

