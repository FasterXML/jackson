Version 2.6.0 was released July 17th, 2015.
It is a "minor" release following 2.5, meaning that it adds new functionality but be backwards compatible with earlier 2.x releases.

## Status

Branch is closed for all new releases: the last micro-patches will be released by end of 2020, after which no releases will be made.

## New Modules

This version introduced 2 new FasterXML-maintained standard modules:

* [Protobuf](../../jackson-dataformat-protobuf) data format module for handling [Protocol Buffers](https://en.wikipedia.org/wiki/Protocol_Buffers) encoded data
* [OSGi](../../jackson-module-osgi) module for injecting helper objects (via `@JacksonInject`) using OSGi registry

## Patches

Beyond initial 2.6.0 (described here), following patch releases have been made or are planned:

* [2.6.1](Jackson-Release-2.6.1) (09-Aug-2015)
* [2.6.2](Jackson-Release-2.6.2) (15-Sep-2015)
* [2.6.3](Jackson-Release-2.6.3) (12-Oct-2015)
* [2.6.4](Jackson-Release-2.6.4) (07-Dec-2015)
* [2.6.5](Jackson-Release-2.6.5) (19-Jan-2016)
* [2.6.6](Jackson-Release-2.6.6) (05-Apr-2016)
* [2.6.7](Jackson-Release-2.6.7) (05-Jun-2016)

At this point, branch was officially closed for full releases, but there are further micro-patches of:

* [Databind 2.6.7.x](Jackson-Release-2.6.7.x)

### Changes: compatibility

No changes to JDK requirements or baseline requirements/supports for external platforms (like Android or Scala versions)

### Changes, core

#### [Annotations](../../jackson-annotations)

* [#43](../../jackson-annotations/issues/43): Add `@JsonFormat(with=Feature.xxx)` to support things like `DeserializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED` on per-property basis.
* [#58](../../jackson-annotations/issues/58): Add new properties for `@JsonIgnoreProperties`, "allowGetters", "allowSetters"
* [#60](../../jackson-annotations/issues/60): Add new value type, `OptBoolean`, for "optional booleans", to support proper handling and usage of default values, not just explicit true/false.
* [#61](../../jackson-annotations/issues/61): Add new property, `@JsonProperty.access` (and matching enum) to support read-only/write-only properties
* [#64](../../jackson-annotations/issues/64): Add `@Documented` for `@JsonPropertyDescription`

#### [Streaming](../../jackson-core)

* [#137](../../jackson-core/issues/137): Allow filtering content read via `JsonParser` by specifying `JsonPointer`; uses new class `com.fasterxml.jackson.core.filter.FilteringParserDelegate` (and related, `TokenFilter`)
* [#177](../../jackson-core/issues/177): Add a check so `JsonGenerator.writeString()` won't work if `writeFieldName()` expected.
* [#182](../../jackson-core/issues/182): Inconsistent `TextBuffer.getTextBuffer()` behavior
* [#185](../../jackson-core/issues/185): Allow filtering content written via `JsonGenerator` by specifying `JsonPointer`; uses new class `com.fasterxml.jackson.core.filter.FilteringGeneratorDelegate` (and related, `TokenFilter`)
* [#188](../../jackson-core/issues/188): `JsonParser.getValueAsString()` should return field name for `JsonToken.FIELD_NAME`, not `null`
* [#189](../../jackson-core/issues/189): Add `JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING` (default: true), which may be disabled to prevent use of ThreadLocal-based buffer recyling.
* [#195](../../jackson-core/issues/#195): Add `JsonGenerator.getOutputBuffered()` to find out amount of content buffered, not yet flushed.
* [#196](../../jackson-core/issues/196): Add support for `FormatFeature` extension, for format-specifc Enum-backed parser/generator options
* Other:
    * Minor improvement to construction of "default PrettyPrinter": now overridable by data format modules
    * Implement a new yet more optimized symbol table for byte-backed parsers
    * Add `JsonParser.Feature.IGNORE_UNDEFINED`, useful for data formats like protobuf

#### [Databind](../../jackson-databind)

* [#77](../../jackson-databind/issues/77): Allow injection of 'transient' fields
* [#95](../../jackson-databind/issues/95): Allow read-only properties with `@JsonIgnoreProperties(allowGetters=true)`
* [#296](../../jackson-databind/issues/296): Serialization of transient fields with public getters (add `MapperFeature.PROPAGATE_TRANSIENT_MARKER`)
* [#312](../../jackson-databind/issues/312): Support Type Id mappings where two ids map to same Class
* [#348](../../jackson-databind/issues/348): `ObjectMapper.valueToTree()` does not work with `@JsonRawValue`
* [#504](../../jackson-databind/issues/504): Add `DeserializationFeature.USE_LONG_FOR_INTS`
* [#624](../../jackson-databind/issues/624): Allow setting external `ClassLoader` to use, via `TypeFactory`
* [#649](../../jackson-databind/issues/649): Make `BeanDeserializer` use new `parser.nextFieldName()` and `.hasTokenId()` methods
* [#664](../../jackson-databind/issues/664): Add `DeserializationFeature.ACCEPT_FLOAT_AS_INT` to prevent coercion of floating point numbers int `int`/`long`/`Integer`/`Long`
* [#677](../../jackson-databind/issues/677): Specifying `Enum` value serialization using `@JsonProperty`
* [#688](../../jackson-databind/issues/688): Provide a means for an ObjectMapper to discover mixin annotation classes on demand
* [#689](../../jackson-databind/issues/689): Add `ObjectMapper.setDefaultPrettyPrinter(PrettyPrinter)`
* [#696](../../jackson-databind/issues/696): Copy constructor does not preserve `_injectableValues`
* [#698](../../jackson-databind/issues/698): Add support for referential types (`ReferenceType`)
* [#700](../../jackson-databind/issues/700): Cannot Change Default Abstract Type Mapper from LinkedHashMap
* [#725](../../jackson-databind/issues/725): Auto-detect multi-argument constructor with implicit names if it is the only visible creator
* [#727](../../jackson-databind/issues/727): Improve `ObjectWriter.forType()` to avoid forcing base type for container types
* [#734](../../jackson-databind/issues/734): Add basic error-recovery for `ObjectReader.readValues()`
* [#737](../../jackson-databind/issues/737): Add support for writing raw values in `TokenBuffer`
* [#740](../../jackson-databind/issues/740): Ensure proper `null` (as empty) handling for `AtomicReference`
* [#743](../../jackson-databind/issues/743): Add `RawValue` helper type, for piping raw values through `TokenBuffer`
* [#756](../../jackson-databind/issues/756): Disabling SerializationFeature.FAIL_ON_EMPTY_BEANS does not affect `canSerialize()`
* [#762](../../jackson-databind/issues/762): Add `ObjectWriter.withoutRootName()`, `ObjectReader.withoutRootName()`
* [#777](../../jackson-databind/issues/777): Allow missing build method if its name is empty ("")
* [#781](../../jackson-databind/issues/781): Support handling of `@JsonProperty.required` for Creator methods
* [#787](../../jackson-databind/issues/787): Add `ObjectMapper setFilterProvider(FilterProvider)` to allow chaining
* [#790](../../jackson-databind/issues/790): Add `JsonNode.equals(Comparator<JsonNode>, JsonNode)` to support configurable/external equality comparison
* [#794](../../jackson-databind/issues/794): Add `SerializationFeature.WRITE_DATES_WITH_ZONE_ID` to allow inclusion/exclusion of timezone id for date/time values (as opposed to timezone offset)
* [#795](../../jackson-databind/issues/795): Converter annotation not honored for abstract types
* [#810](../../jackson-databind/issues/810): Force value coercion for `java.util.Properties`, so that values are `String`s
* [#811](../../jackson-databind/issues/811): Add new option, `JsonInclude.Include.NON_ABSENT` (to support exclusion of JDK8/Guava Optionals)
* [#813](../../jackson-databind/issues/813): Add support for new property of `@JsonProperty.access` to support read-only/write-only use cases
* [#820](../../jackson-databind/issues/820): Add new method for `ObjectReader`, to bind from JSON Pointer position
* [#824](../../jackson-databind/issues/824): Contextual `TimeZone` changes don't take effect wrt `java.util.Date`, `java.util.Calendar` serialization
* [#827](../../jackson-databind/issues/827): Fix for polymorphic custom map key serializer
* [#840](../../jackson-databind/issues/840): Change semantics of `@JsonPropertyOrder(alphabetic)` to only count `true` value
* [#848](../../jackson-databind/issues/848): Custom serializer not used if POJO has `@JsonValue`
* [#849](../../jackson-databind/issues/849): Possible problem with `NON_EMPTY` exclusion, `int`s, `Strings`
* [#868](../../jackson-databind/issues/868): Annotations are lost in the case of duplicate methods

### Changes, Data formats

#### [CSV](../../jackson-dataformat-csv)

* [#72](../../jackson-dataformat-csv/issues/72): Recognize the configured "null value" (String) also in reader-infrastructure.
* [#74](../../jackson-dataformat-csv/issues/74): Problems with ordering, `@JsonPropertyOrder` losing alphabetic ordering
* [#83](../../jackson-dataformat-csv/issues/83): Serializing List with null values leads to corrupt CSV

#### [Protobuf](../../jackson-dataformat-protobuf)

**The very first official release!**

#### [YAML](../../jackson-dataformat-yaml)

* [#35](../../jackson-dataformat-yaml/issues/35): Add `YAMLGenerator.Feature.SPLIT_LINES` to allow controlling whether `SnakeYAML` splits longer text blocks with line breaks or not
    * Also: upgrade to SnakeYAML 1.15

### Changes, Data types

#### [Guava](../../jackson-datatype-guava)

* [#64](../../jackson-datatype-guava/issues/64): `@JsonUnwrapped` annotation is ignored when a field is an `Optional`
* [#66](../../jackson-datatype-guava/issues/66): Add `GuavaModule.configureAbsentsAsNulls(boolean)` to change whether `Optional.absent()` is to be handled same as Java null during serialization (default: true) or not.
* [#67](../../jackson-datatype-guava/issues/67): Support deserializing `ImmutableSetMultimap`s
* [#68](../../jackson-datatype-guava/issues/68): Add support for `JsonInclude.Include.NON_ABSENT`, to compensate for #66
* [#70](../../jackson-datatype-guava/issues/70): Change OSGi manifest entries to import guava 15.0 or greater
* [#74](../../jackson-datatype-guava/issues/74): Multimap serializer ignores `_valueTypeSerializer`

#### [Hibernate](../../jackson-datatype-hibernate)

* [#67](../../jackson-datatype-hibernate/issues/67): Provide support for Hibernate 5.x (hibernate5 maven sub-module; Hibernate5Module Jackson module)

#### [HPPC](../../jackson-datatype-hppc)

* Update hppc dependency to 0.7.1
    * NOTE! Due to HPPC API incompatibilities, this means that module can NOT be used with HPPC versions earlier than 0.7.0.
* Minimum JDK version now 1.7, as per minimum by HPPC

#### [JDK8](../../jackson-datatype-jdk8)

* [#8](../../jackson-datatype-jdk8/issues/8): JDK8 module should respect JsonInclude.Include.NON_ABSENT
* [#11](../../jackson-datatype-jdk8/issues/11): Add `configureAbsentsAsNulls` config setting, for better compatibility with Guava module

#### [Joda](../../jackson-datatype-joda)

* [#49](../../jackson-datatype-joda/issues/49): testDateMidnightSerWithTypeInfo test dependent on $TZ
* [#58](../../jackson-datatype-joda/issues/58): Support timezone configuration for Interval deserializer
* [#62](../../jackson-datatype-joda/issues/62): Allow use of numbers-as-Strings for LocalDate (in array)
* [#64](../../jackson-datatype-joda/issues/64): Support `@JsonFormat(pattern=...)` for deserialization
* [#66](../../jackson-datatype-joda/issues/66): Support `SerializationFeature.WRITE_DATES_WITH_ZONE_ID`
* [#68](../../jackson-datatype-joda/issues/68): TimeZone in DeserializationContext is ignored with `SerializationFeature.WRITE_DATES_WITH_ZONE_ID`

#### [JSR-310](../../jackson-datatype-jsr310) (aka Java8 Dates)

* [#18](../../jackson-datatype-jsr310/issues/18): Support serializing and deserializing Maps with jsr310 types as keys
* [#26](../../jackson-datatype-jsr310/issues/26): ISO strings with time-component cause exception when deserializing to LocalDate
* [#29](../../jackson-datatype-jsr310/issues/29): Add support for `@JsonFormat` for `Instant`
* [#30](../../jackson-datatype-jsr310/issues30/): Make `ZonedDateTime` serializer support `SerializationFeature.WRITE_DATES_WITH_ZONE_ID`

### Changes, Jackson jr

* [#24](../../jackson-jr/issues/24): `String`/`byte[]` composers can not write POJOs (ObjectCodec not linked)
* Other
    * Minor performance optimizations (+10-20% throughput), using new jackson-core 2.6 methods for reading, writing

### Changes, JAX-RS

* [#39](../../jackson-jaxrs-providers/issues/39): Build alternate jars with qualifier "no-metainf-services", which do NOT include `META-INF/services` metadata for auto-registration

### Changes, other modules

#### [Afterburner](../../jackson-module-afterburner)

* [#46](../../jackson-module-afterburner/issues/46): Log-warning "Disabling Afterburner deserialization for type" using wrong logger-name
* [#53](../../jackson-module-afterburner/issues/53): Include checksum in generated class names (to resolve #52)

#### [JSON Schema](../../jackson-module-jsonSchema)

* [#53](../../jackson-module-jsonSchema/issues/53): Add support to oneOf
* [#57](../../jackson-module-jsonSchema/issues/57): Enum types should generate a schema that includes the possible enum values
* [#60](../../jackson-module-jsonSchema/issues/60): Add `readonly` property to `JsonSchema`
* [#67](../../jackson-module-jsonSchema/issues/67): Unable to deserialize (extended/custom) Schema
* [#69](../../jackson-module-jsonSchema/issues/69): Add support for @Pattern annotations in String schemas
*  Added `JsonSchemaGenerator(ObjectWriter)` to allow use of (re-)configured `ObjectWriter` instead of `ObjectMapper` which can not be configured.

#### [Mr Bean](../../jackson-module-mrbean)

* [#21](../../jackson-module-mrbean/issues/21): Change default package used for materialized classes to reflect Jackson 2.x

#### [OSGi](../../jackson-module-osgi)

**The very first official release!**

#### [Parameter Names](../../jackson-module-parameter-names) (Java 8 parameter names)

* [#21](../../jackson-module-parameter-names/issues/21): Unable to associate parameter name with single non-annotated constructor argument
