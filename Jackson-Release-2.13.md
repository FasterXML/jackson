[Jackson Version](Jackson-Releases) 2.13 was released on September 30, 2021. Two release candidates (2.13.0-rc1, 2.13.0-rc2) were released prior to the final 2.13.0.

This wiki page gives a list of links to all changes (with brief descriptions) that were included.

-----
## Status

Branch is closed as of January 2023: only micro-patches are expected after `2.13.5` release.

## Patches

* [2.13.1](Jackson-Release-2.13.1) (19-Dec-2021)
* [2.13.2](Jackson-Release-2.13.2) (06-Mar-2022)
* [2.13.3](Jackson-Release-2.13.3) (14-May-2022)
* [2.13.4](Jackson-Release-2.13.4) (03-Sep-2022)
* [2.13.5](Jackson-Release-2.13.5) (23-Jan-2023)

### Micro-patches

Following micro-patches have been released:

* `jackson-databind` `2.13.2.1` (24-Mar-2022) -- with `jackson-bom` version `2.13.2.20220324`
    * [#2816](../../jackson-databind/issues/2816): Optimize `UntypedObjectDeserializer` wrt recursion (CVE-2020-36518)
    * **NOTE!** As per #3465 any code that sub-classes `UntypedObjectDeserializer` may break due to unintended removal of existing methods (that is, a change to add a new argument).
* `jackson-databind` `2.13.2.2` (28-Mar-2022) -- with `jackson-bom` version `2.13.2.20220328`
    * Same as `2.13.2.1` above, but correct Gradle Module Metadata (2.13.2.1 had invalid reference to parent, `jackson-bom`)
        * ^^^ See [jackson-bom#52](https://github.com/FasterXML/jackson-bom/issues/52) for details
    * **NOTE!** As per #3465 any code that sub-classes `UntypedObjectDeserializer` may break due to unintended removal of existing methods (that is, a change to add a new argument).
* `jackson-databind` `2.13.4.1` (12-Oct-2022) -- with `jackson-bom` version `2.13.4.20221012`
    * [#3590](../../jackson-databind/issues/3590) Add check in primitive value deserializers to avoid deep wrapper array nesting wrt `UNWRAP_SINGLE_VALUE_ARRAYS` [CVE-2022-42003]
* `jackson-databind` `2.13.4.2` (13-Oct-2022) -- with `jackson-bom` version `2.13.4.20221013`
    * [#3627](../../jackson-databind/issues/3627): Fix a Gradle Module Metadata problem
    * Only relevant to Gradle users

## Documentation

* [Sneak Peek at Jackson 2.13](https://cowtowncoder.medium.com/sneak-peek-at-jackson-2-13-6f3009ce2d79)

## Changes, compatibility

### Compatibility: JDK requirements

JDK baseline for most components has been raised to Java 8 for `jackson-databind` and other components that so far
(up until 2.12) had only required Java 7 (but not including ones that only require Java 6 -- `jackson-annotations`, `jackson-core` and `jackson-jr` -- which will retain Java 6 minimum).

Benefits include:

1. Ability to embed one or both of "Java 8 datatypes" (`Optional` and related) and "Java 8 parameter names" (auto-discovery of constructor parameters) -- eventually, but not in 2.13
2. Convergence with 3.0 plans: can add configuration methods that take closures (modifications of "Config Overrides" and "Coercion Configs" in particular)
3. Ability to use Java 8 functional aspects in API in general for both 2.x and 3.0

### Compatibility: Build changes

The new `jackson-jakarta-rs-providers` requires JDK 11 to build (due to Jetty test dependency); other modules still only need JDK 8.

### Compatibility: Module changes

* `jackson-jaxrs-providers` no longer has a work-around to (try to) allow use with JAX-RS 1.x implementations (see issue #134 below). It is doubtful usage would work regardless, but complex hack was is removed from 2.13.0.
* `module-info.class` is now included under `META-INF/versions/11/` in jars, instead of at root level (see [jackson-bom#39](https://github.com/FasterXML/jackson-bom/issues/39))
* `jackson-dataformat-xml` no longer has direct dependency on `jackson-module-jaxb-annotations`!
    * Helps resolve issues wrt "JAXB vs Jakarta" problems

### Compatibility: Dependency changes

* Jackson jr `jr-stree` dependency to `jr-objects` now optional, as per [jackson-jr#88](../../jackson-jr/issues/88) (since there is only one reference, and that from a class use for which means caller already uses `jr-objects` directly anyway)

## Changes, behavior

### Databind

* Change [databind#3117](../../jackson-databind/issues/3117) reduces introspection visibility for "JDK Types" (classes in `java.*` and `javax.*` packages) to "only public fields, accessors" (see issue for details) to improve JDK compatibility with newer JDKs. This results in the following reported observable changes:
    * [databind#3493](../../jackson-databind/issues/3493): `java.io.ByteArrayOutputStream` happened to serialize previously, as a POJO (with properties `buf` and `count`); it will no longer be serializable out-of-the-box.
        * A follow-up issue [databind#3522](../../jackson-databind/issues/3522) filed for possibly allowing serialization, but as proper Binary value
* To fix [databind#3130](../../jackson-databind/issues/3130) (StackOverflow on JDK 11+ when serializing `java.lang.Thread`), serialization of `java.lang.ClassLoader` is now empty Object (`{ }`) -- if users want different serialization, need to add custom serializer
* To fix [databind#3244](../../jackson-databind/issues/3244) a set of formerly unserializable Jackson types like `ObjectMapper` are now serialized as Empty Object value (see [databind#3302](../../jackson-databind/issues/3302) for details)
* Fix for [databind#3271](../../jackson-databind/issues/3271) to remove unintended coercion of JSON `null` into Java String "null" (in context of reading Type Id) had the downstream effect of breakage for DropWizard (reported as [databind#3313](../../jackson-databind/issues/3313)).
    * New behavior is considered correct and will remain in 2.13 and later versions.
* As reported in [databind#3510](../../jackson-databind/issues/3510) earlier "accidental" support for deserializing `javax.mail.internet.InternetAddress` no longer exists (likely removed from 2.12.0 already) due to security improvements. Since this type is nore part of Core JDK, support would need to be added as a Module (cannot add new library dependencies in `jackson-databind`)
    * No module currently exists: users will need to, for now, provide a custom deserializer to deserialize this type.

## New Modules

### TOML Dataformat Module

New module -- `jackson-dataformat-toml` -- now included in [jackson-dataformats-text](https://github.com/FasterXML/jackson-dataformats-text), supports [TOML](https://en.wikipedia.org/wiki/TOML).

### Jakarta variants to replace Javax modules

To allow working around the Oracle-created "Javax vs Jakarta" problem, Jackson 2.13 introduces hard forks of 3 kinds of existing
modules (see below). You (or your platform/framework) will need to change dependencies appropriately, to get the module
needed for your Jakarta-or-Javax APIs (hooray for interoperability).

Note that these modules will obsolete Jackson 2.12 use of "-jakarta" classifier for JAXB and JAX-RS provider modules.

#### Jakarta XML Bind (`jakarta.xml.bind`) annotations

New module -- `jackson-module-jakarta-xmlbind-annotations` -- added in "jackson-modules-base", as alternative to existing
"old" JAXB Annotations module.

#### Jakarta-RS providers

New modules with names like `jackson-jakarta-rs-json-provider` (for `cbor`, `smile`, `xml` and `yaml`) as alternatives to
existing "jackson-jaxrs-[FORMAT]-provider" providers.

#### Jakarta JSONP datatype

New module, `jackson-datatype-jakarta-jsonp` added as the replacement for Javax/JSON-P supporting `jackson-datatype-jsr353`.

#### Jakarta Mail datatype

New module, `jackson-datatype-jakarta-mail` added to support a small subset (one class, `jakarta.mail.internet.InternetAddress`) of Jakarta Mail (ex "Java Mail") library.

### "No Constructor" Deserializer module

New module -- `jackson-module-no-ctor-deser` -- now included in [jackson-modules-base](https://github.com/FasterXML/jackson-modules-base) -- added to support a very specific use case of POJOs that do not have either:

1. No-arguments ("default") constructor, nor
2. `@JsonCreator` annotated constructor or factory method

in which case module can force instantiation of values without using any constructor, using JDK-internal implementation (included to support JDK serialization itself).
Note that this module may stop functioning in future, but appears to work at least until JDK 14.

## Module deprecation, removal

### Hibernate 3 module: drop

Due to low-to-no usage of Hibernate 3 module (as compared to Hibernate 4 and 5), Hibernate 3 module
(`jackson-datatype-hibernate3`)
will be dropped from 2.13, as per [datatype-hibernate#139](https://github.com/FasterXML/jackson-datatype-hibernate/issues/139).

(NOTE: whether Hibernate module will be released for Jackson 3.x is an open question too, due to the lack of maintainers)

-----

## Major focus areas planned -- but postponed due to lack of time

### (Finally) Rewrite Creator Detection wrt Property Discovery

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

### Configurability ideas similarly deferred

Separate configuration settings for:

#### JsonNode config/feature

Part of/related to [JSTEP-3](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-3), it'd be good to have a set of simple on/off features to configure read/write/transform aspects of `JsonNode`, distinct from POJOs.

These settings should ideally be per-call, similar to `[De]SerializationFeature`.

#### Date/Time config/feature(s)

Part of/related to [JSTEP-5](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-5) (and [JSTEP-6](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-6)), there are things that should be easily configurable as on/off "features", or similar configuration object(s).

One challenging part is that some of the settings should probably be per-call ones (i.e. can change on specific read/write); whereas others must be per-mapper (unchangeable after construction)

#### Enum read/write features

Handling of `Enum` value reading, writing, is currently configured with a hodge-podge of `SerializationFeature` / `DeserialiationFeature`s, but as with `JsonNode` and Date/Time values, that's not a good place as those should be for more general aspects of handling.
This is covered to some degree in [JSTEP-6](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-6).

So we should like have `EnumFeature`s too, changeable on per-call basis; partly to replace existing `[De]SerializationFeature`s (for 3.0), and partly to expose new ones.

### Processing Limits (deferred as well)

Mentioned as one future JSTEP on https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP, there really should be limits to maximum size and/or complexity of input to process, mostly to prevent potential DoS attacks (as well as accidental "intern brought down our system by broken script" cases). There is some prior art in Woodstox, for example (see [Woodstox-specific settings ("limits")](https://cowtowncoder.medium.com/configuring-woodstox-xml-parser-woodstox-specific-properties-1ce5030a5173)).

-----

Full set of changes included in 2.13.0 release.

## Full Change list

### Changes, core

#### [Annotations](../../jackson-annotations)

No changes since 2.12

#### [Streaming](../../jackson-core)

* [#652](../../jackson-core/issues/652): Misleading exception for input source when processing byte buffer
with start offset
* [#658](../../jackson-core/issues/658): Escape contents of source document snippet for `JsonLocation._appendSourceDesc()`
* [#664](../../jackson-core/issues/664): Add `StreamWriteException` type to eventually replace `JsonGenerationException`
* [#671](../../jackson-core/issues/671): Replace `getCurrentLocation()`/`getTokenLocation()` with `currentLocation()`/`currentTokenLocation()` in `JsonParser`
* [#673](../../jackson-core/issues/673): Replace `JsonGenerator.writeObject()` (and related) with `writePOJO()`
* [#674](../../jackson-core/issues/674): Replace `getCurrentValue()`/`setCurrentValue()` with `currentValue()`/`assignCurrentValue()` in `JsonParser`/`JsonGenerator`
* [#677](../../jackson-core/issues/677): Introduce O(n^1.5) BigDecimal parser implementation
* [#687](../../jackson-core/issues/687): ByteQuadsCanonicalizer.addName(String, int, int) has incorrect handling
for case of q2 == null
* [#692](../../jackson-core/issues/692): UTF32Reader ArrayIndexOutOfBoundsException
* [#694](../../jackson-core/issues/694): Improve exception/JsonLocation handling for binary content: don't
show content, include byte offset
* [#700](../../jackson-core/issues/700): Unable to ignore properties when deserializing. TokenFilter seems broken

#### [Databind](../../jackson-databind)

* [#1850](../../jackson-databind/issues/1850): `@JsonValue` with integer for enum does not deserialize correctly
* [#2509](../../jackson-databind/issues/2509): `AnnotatedMethod.getValue()/setValue()` doesn't have useful exception message
* [#2828](../../jackson-databind/issues/2828): Add `DatabindException` as intermediate subtype of `JsonMappingException`
* [#2900](../../jackson-databind/issues/2900): Jackson does not support deserializing new Java 9 unmodifiable collections
* [#2989](../../jackson-databind/issues/2989): Allocate TokenBuffer instance via context objects (to allow format-specific buffer types)
* [#3001](../../jackson-databind/issues/3001): Add mechanism for setting default `ContextAttributes` for `ObjectMapper`
* [#3002](../../jackson-databind/issues/3002): Add `DeserializationContext.readTreeAsValue()` methods for more convenient conversions for deserializers to use
* [#3011](../../jackson-databind/issues/3011): Clean up support of typed "unmodifiable", "singleton" Maps/Sets/Collections
* [#3033](../../jackson-databind/issues/3033): `MapperFeature` to use `long` internally
* [#3035](../../jackson-databind/issues/3035): Add `removeMixIn()` method in `MapperBuilder`
* [#3036](../../jackson-databind/issues/3036): Backport `MapperBuilder` lambda-taking methods: `withConfigOverride()`, `withCoercionConfig()`, `withCoercionConfigDefaults()`
* [#3080](../../jackson-databind/issues/3080): configOverrides(boolean.class) silently ignored, whereas .configOverride(Boolean.class)
works for both primitives and boxed boolean values
* [#3082](../../jackson-databind/issues/3082): Dont track unknown props in buffer if `ignoreAllUnknown` is true
* [#3091](../../jackson-databind/issues/3091): Should allow deserialization of java.time types via opaque
`JsonToken.VALUE_EMBEDDED_OBJECT`
* [#3101](../../jackson-databind/issues/3101): Add AnnotationIntrospector.XmlExtensions interface for decoupling javax dependencies
* [#3110](../../jackson-databind/issues/3110): Custom SimpleModule not included in list returned by ObjectMapper.getRegisteredModuleIds() after registration
* [#3117](../../jackson-databind/issues/3117): Use more limiting default visibility settings for JDK types (java.*, javax.*)
* [#3122](../../jackson-databind/issues/3122): Deep merge for `JsonNode` using `ObjectReader.readTree()`
* [#3125](../../jackson-databind/issues/3125): IllegalArgumentException: Conflicting setter definitions for property
with more than 2 setters
* [#3130](../../jackson-databind/issues/3130): Serializing `java.lang.Thread` fails on JDK 11+
* [#3143](../../jackson-databind/issues/3143): String-based `Map` key deserializer is not deterministic when there is no single arg constructor
* [#3154](../../jackson-databind/issues/3154): Add ArrayNode#set(int index, primitive_type value)
* [#3160](../../jackson-databind/issues/3160): JsonStreamContext "currentValue" wrongly references to @JsonTypeInfo annotated object
* [#3174](../../jackson-databind/issues/3174): DOM `Node` serialization omits the default namespace declaration
* [#3177](../../jackson-databind/issues/3177): Support `suppressed` property when deserializing `Throwable`
* [#3187](../../jackson-databind/issues/3187): `AnnotatedMember.equals()` does not work reliably
* [#3193](../../jackson-databind/pulls/3193): Add MapperFeature.APPLY_DEFAULT_VALUES, initially for Scala module
* [#3214](../../jackson-databind/pulls/3214): For an absent property Jackson injects `NullNode` instead of `null` to a JsonNode-typed constructor argument of a `@ConstructorProperties`-annotated constructor
* [#3217](../../jackson-databind/pulls/3217): `XMLGregorianCalendar` doesn't work with default typing
* [#3227](../../jackson-databind/pulls/3227): Content `null` handling not working for root values
* [#3234](../../jackson-databind/pulls/3234): StdDeserializer rejects blank (all-whitespace) strings for ints
* [#3235](../../jackson-databind/pulls/3235): `USE_BASE_TYPE_AS_DEFAULT_IMPL` not working with `DefaultTypeResolverBuilder`
* [#3238](../../jackson-databind/pulls/3238): Add PropertyNamingStrategies.UpperSnakeCaseStrategy (and UPPER_SNAKE_CASE constant)
* [#3244](../../jackson-databind/pulls/3244): StackOverflowError when serializing JsonProcessingException
* [#3259](../../jackson-databind/pulls/3259): Support for BCP 47 `java.util.Locale` serialization/deserialization
* [#3271](../../jackson-databind/pulls/3271): String property deserializes null as "null" for JsonTypeInfo.As.EXISTING_PROPERTY
* [#3302](../../jackson-databind/pulls/3302): Serialize formerly unserializable Jackson types like ObjectMapper as Empty Object value
* [#3397](../../jackson-databind/pulls/3397): Optimize `JsonNodeDeserialization` wrt recursion

### Changes, data formats

#### Avro

* [#283](../../jackson-dataformats-binary/issues/283): Add `logicalType` support for some `java.time` types; add `AvroJavaTimeModule` for native ser/deser
* [#290](../../jackson-dataformats-binary/issues/290): Generate logicalType switch

#### CBOR

* [#239](../../jackson-dataformats-binary/issues/239): Should validate UTF-8 multi-byte validity for short decode path too
* [#253](../../jackson-dataformats-binary/issues/253): Make `CBORFactory` support `JsonFactory.Feature.CANONICALIZE_FIELD_NAMES`
* [#264](../../jackson-dataformats-binary/issues/264): Handle case of BigDecimal with Integer.MIN_VALUE for scale gracefully
* [#272](../../jackson-dataformats-binary/issues/272): Uncaught exception in CBORParser._nextChunkedByte2 (by ossfuzze
* [#273](../../jackson-dataformats-binary/issues/273): Another uncaught exception in CBORParser._nextChunkedByte2 (by ossfuzzer)
* [#284](../../jackson-dataformats-binary/issues/284): Support base64 strings in `getBinaryValue()` for CBOR and Smile
* [#289](../../jackson-dataformats-binary/issues/289): `ArrayIndexOutOfBounds` for truncated UTF-8 name

#### CSV

* [#240](../../jackson-dataformats-text/issues/240): Split `CsvMappingException` into `CsvReadException`/`CsvWriteException`
* [#270](../../jackson-dataformats-text/issues/270): Should not quote with strict quoting when line starts with `#` but comments are disabled
* [#283](../../jackson-dataformats-text/issues/283): `CsvSchema.getColumnDesc()` returns unpaired square bracket when columns are empty

#### Ion

* [#295](../../jackson-dataformats-binary/issues/295): `jackson-dataformat-ion` does not handle null.struct deserialization correctly

#### Smile

* [#252](../../jackson-dataformats-binary/issues/252): Make `SmileFactory` support `JsonFactory.Feature.CANONICALIZE_FIELD_NAMES`
* [#276](../../jackson-dataformats-binary/issues/276): Add `SmileGenerator.Feature.LENIENT_UTF_ENCODING` for lenient handling of broken Unicode surrogate pairs on writing
* [#284](../../jackson-dataformats-binary/issues/284): Support base64 strings in `getBinaryValue()` for CBOR and Smile
* [#291](../../jackson-dataformats-binary/issues/291): `ArrayIndexOutOfBounds` for truncated UTF-8 name

#### TOML

* [#219](../../jackson-dataformats-text/issues/219): Add TOML (https://en.wikipedia.org/wiki/TOML) support!

#### XML

* [#441](../../jackson-dataformat-xml/issues/441): Add `ToXmlGenerator.Feature.UNWRAP_ROOT_OBJECT_NODE` (to avoid root `ObjectNode` wrapper element)
* [#442](../../jackson-dataformat-xml/issues/442): Missing START_OBJECT token in complex element starting with text
* [#462](../../jackson-dataformat-xml/issues/462): Remove `jackson-module-jaxb-annotations` runtime dependency (leave as a test dep). Also upgrade to use new "Jakarta" variety of annotations
* [#463](../../jackson-dataformat-xml/issues/463): NPE via boundary condition, document with only XML declaration
* [#465](../../jackson-dataformat-xml/issues/465): ArrayIndexOutOfBoundsException in UTF8Reader (ossfuzz)
* [#467](../../jackson-dataformat-xml/issues/467): Ignore contents of elements annotated with xsi:nil="true" (when xsi:nil handling enabled)
* [#468](../../jackson-dataformat-xml/issues/468): Add `FromXmlParser.Feature.PROCESS_XSI_NIL` to allow disabling processing of `xsi:nil` attributes on reading
* [#474](../../jackson-dataformat-xml/issues/474): Empty String ("") parsed as 0 for int even if DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES enabled (note: actual fix in `jackson-databind)
* [#483](../../jackson-dataformat-xml/issues/483): Explicitly pass ClassLoader of XmlFactory when creating Stax input/output factory,
   instead of context ClassLoader
* [#485](../../jackson-dataformat-xml/issues/485): Deserialization with XmlMapper and DeserializationFeature.UNWRAP_ROOT_VALUE no longer works in 2.12

### Changes, datatypes

#### [Hibernate](../../jackson-datatypes-hibernate)

* [#139](../jackson-datatype-hibernate/issues/139): Drop support for Hibernate 3.x from Jackson 2.13
* [#144](../jackson-datatype-hibernate/issues/144): Add new module (`jackson-datatype-hibernate5-jakarta`) to support Jakarta EE for Hibernate 5.5

#### [Java 8 date/time](../../jackson-modules-java8/datetime)

* [#131](../../jackson-modules-java8/issues/131): Deserializing ZonedDateTime with basic TZ offset notation (0000)
* [#212](../../jackson-modules-java8/issues/212): Make LocalDateDeserializer consider strict/lenient on accepting (or not)
of "time" part

#### [Joda Money](../../jackson-datatypes-misc)

* [#8](../../jackson-datatypes-misc/issues/8): Improve error handling of "joda-money" `MoneyDeserializer`, `CurrencyUnitDeserializer`

### Changes, Other modules

#### Blackbird

* [#141](../../jackson-modules-base/issues/141): Blackbird fails to deserialize varargs array

#### JAXB

* [#130](../../jackson-modules-base/issues/130): Addition of new `jackson-module-jakarta-xmlbind-annotations` module
    * Supports `jakarta.xml.bind` flavor of "new" JAXB annotations, instead of old `javax.xml.bind` annotations (which are still covered by existing "old" `jackson-module-jaxb-annotations` module)

### Changes, [JAX-RS providers](../../jackson-jaxrs-providers)

* [#134](../../jackson-jaxrs-providers/issues/134): Remove work-around for JAX-RS 1.x wrt JAX-RS 2 type `NoContentException`
* [#146](../../jackson-jaxrs-providers/issues/146): Add separate "Jakarta [format] provider"s for 2.13
    * Provides new `jakarta.ws.rs` ("Jakarta-RS") provider implementations; replacements for "javax"/JAX-RS providers

### Changes, JVM Languages

#### [Kotlin](../../jackson-module-kotlin)

* [#438](../../jackson-module-kotlin/issues/438): Fixed mapping failure when `private` `companion object` is named
* [#447](../../jackson-module-kotlin/issues/447): Fix edge case when dealing with sealed classes
* [#468](../../jackson-module-kotlin/issues/468): Improved support for value classes
* [#477](../../jackson-module-kotlin/issues/477): Improved documentation for KotlinFeature
* [#489](../../jackson-module-kotlin/issues/489): Extension functions for JsonNode, ArrayNode and ObjectNode
* [#490](../../jackson-module-kotlin/issues/490): Fix deserialization of missing value (was `NullNode`, now literal `null`)
* [#494](../../jackson-module-kotlin/issues/494): Improved documentation for ProGuard users
* [#496](../../jackson-module-kotlin/issues/496): Fix type erasure in treeToValue() extension function

#### [Scala](../../jackson-module-scala)

* [#211](../../jackson-module-scala/issues/211): Deserialization of case object creates a new instance of case object
* [#296](../../jackson-module-scala/issues/296): JsonSerializable gets ignored on classes implementing Map
* [#382](../../jackson-module-scala/issues/382): Option field deserialization failure
* [#443](../../jackson-module-scala/issues/443): Objects in tuples are not serialized as ids
* [#479](../../jackson-module-scala/issues/479): Scala3 support
* [#503](../../jackson-module-scala/issues/503): big improvement to ClassTagExtensions, the Scala3 friendly replacement for ScalaObjectMapper. Big thanks to Gaël Jourdan-Weil.
* [#512](../../jackson-module-scala/issues/512): add support for recognising Scala3 classes (TastyUtil)
* [#514](../../jackson-module-scala/issues/514): support MapperFeature.APPLY_DEFAULT_VALUES (defaults to true)
* [#545](../../jackson-module-scala/issues/545): (Experimental) support for registering reference types (when they can't be inferred)

### Changes, other

#### [Jackson-jr](../../jackson-jr)

* [#79](../../jackson-jr/issues/79): Reuse of ClassKey in ValueWriterLocator not working
* [#80](../../jackson-jr/issues/80): Case-insensitive property, enum deserialization should be supported
* [#81](../../jackson-jr/issues/81): `JsrValue` should implement `equals()`
* [#83](../../jackson-jr/issues/83): Support `@JsonProperty` annotation on enum values
* [#84](../../jackson-jr/issues/84): Public static fields are included in serialized output
* [#88](../../jackson-jr/issues/88): Make `jr-stree` dependency to `jr-objects` optional





