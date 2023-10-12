[Jackson Version](Jackson-Releases) 2.15 was released on April 23, 2023.
Three release candidates (2.15.0-rc1, -rc2 and -rc3) were released prior to the final 2.15.0.

This wiki page gives a list of links to all changes (with brief descriptions) that are included, as well as about original plans for bigger changes (and in some cases changes to plans, postponing).

## Status

Branch is open (as of May 2023) and new patch releases are expected.

## Patches

* [2.15.1](Jackson-Release-2.15.1) (2023-05-16)
* [2.15.2](Jackson-Release-2.15.2) (2023-05-30)
* [2.15.3](Jackson-Release-2.15.3) (not yet released)

## Documentation

### Articles, Blog posts

* [Jackson 2.15 Overview](https://cowtowncoder.medium.com/jackson-2-15-release-overview-ecd10926aa83)
    * [Jackson 2.15 floating-point read performance improvements](https://cowtowncoder.medium.com/jackson-2-15-yet-faster-floating-point-reads-fd6d5a0b769a)
    * [Jackson 2.15 floating-point write performance improvements](https://medium.com/@cowtowncoder/jackson-2-15-faster-floating-point-writes-too-19958e310185)

## New Modules

### Hibernate 6

[Hibernate](../../jackson-datatype-hibernate) repo now provides `jackson-datatype-hibernate6` to work with Hibernate 6: it requires JDK 11.

### Jakarta-variant of JSON-Schema

[JSON Schema](../../jackson-module-jsonSchema/) module now provides both JAXB API-based "old" `jackson-module-jsonSchema` and new `jackson-module-jsonSchema-jakarta` (Jakarta API) modules.

## Changes, compatibility

### Compatibility: platform requirements

#### JDK

Same as [Jackson 2.14](Jackson-Release-2.14)

#### Kotlin

Jackson 2.15 no longer supports Kotlin 1.4 -- supported versions are 1.5 - 1.8

jackson-module-kotlin changes the serialization result of getter-like functions starting with 'is'. For example, a function defined as `fun isValid(): Boolean`, which was previously output with the name valid, is now output with the name `isValid` ([KOTLIN#670](https://github.com/FasterXML/jackson-module-kotlin/issues/670)).

### Compatibility: transitive dependencies

#### YAML format module

As per [YAML#390](../../jackson-dataformats-text/pull/390) `SnakeYAML` dependency upgrade to Snakeyaml 2.0 from 1.33, to resolve [CVE-2022-1471](https://nvd.nist.gov/vuln/detail/CVE-2022-1471).

Despite seeming major version upgrade, should NOT affect compatibility of Jackson YAML format module -- SnakeYAML version scheme only uses 2 digits so this is more like a minor version upgrade, affecting API that Jackson does not use.
Jackson YAML module will still work with older version of SnakeYAML (such as 1.33) so if necessary, users can forcible downgrade it if necessary for compatibility reasons with other libraries, frameworks.

##### Guava Module

Default/baseline Guava dependency now `23.6.1-jre` (was `21.0` in 2.14), but module still works with full range of Guava versions from `14.0` to the latest (`31.1-jre` as of writing this)

### Compatibility: build/artifact changes

#### Build JDK changes

* Hibernate module build now requires JDK 11 (due to Hibernate 6 module)

#### Jar changes

* jackson-core is now a [Multi-Release jar](https://openjdk.org/jeps/238) to support more optimal handling for newer JDKs wrt number parsing.

## Changes, behavior

### Processing Limits

#### General

2.15 adds maximum processing limits for certain aspects of parsing as described below.
Issues were included under umbrella issue [#637](../../jackson-core/issues/637).

Implemented limits are:

* Expressed in input units -- `byte`s or `char`s -- depending on input source
* Defined as longest allowed length, but not necessarily imposed at 100% accuracy: that is, if maximum allowed length is specified as 1000 units, something with length of, say 1003 may not cause exception (but 1500 would)
* Defined in new `StreamReadConstraints` class, configurable on per-`JsonFactory` basis

#### Maximum number token lengths

Implementation of [jackson-core#815](../../jackson-core/issues/815) sets up upper limit on maximum length of numeric tokens read from input.
Default limit is:

* Maximum 1000 for both integral and floating-point numbers.

Note that dataformat modules need to add support for enforcing the limits so coverage may vary: as usual, JSON parser will have the widest coverage initially.

#### Maximum String value length

Implementation of [jackson-core#863](../../jackson-core/issues/863) sets upper limit on maximum length of String values read from input. Default limit is:

* 20_000_000 (20 million) input units bytes/chars depending on input source) in 2.15.1, via [jackson-core#1014](../../jackson-core/issues/1014)
    * Initial maximum was 5_000_000 (5 million) input units in 2.15.0 relase

#### Maximum Input nesting depth

Implementation of [jackson-core#943](../../jackson-core/pull/943) sets upper limit on maximum input nesting (Objects, Arrays) read from input. Default limit is:

* 1000 levels

### Changes, behavior, other

* Java 8 Date/Time handling:
     * https://github.com/FasterXML/jackson-modules-java8/pull/267: Normalize zone id during ZonedDateTime deserialization

-----

## Major focus areas, features included

### Processing limits

* Implemented limits -- as explained earlier -- for
    * Maximum Number and String token lengths ([core#815](../../jackson-core/issues/815), [core#863](../../jackson-core/issues/863))
    * Maximum input nesting ([core#943](../../jackson-core/pull/943))

### Further number decoding performance optimizations

Use of [FastDoubleParser](https://github.com/wrandelshofer/FastDoubleParser) library in more places, more widely (2.14 already used it in some places) may yield incremental performance improvements. Also uses the latest release of FDP.

### Other Most Wanted Issues included

* [#2667](../../jackson-databind/issues/2667): Add `@EnumNaming`, `EnumNamingStrategy` to allow use of naming strategies for Enums
* [#2968](../../jackson-databind/issues/2968): Deserialization of `@JsonTypeInfo` annotated type fails with missing type id even for explicit concrete subtypes

## Major focus areas planned -- but postponed due to lack of time

### Rewrite Creator Detection wrt Property Discovery

Postponed already since at least 2.13, needs to become priority for 2.16

### Some other processing limits

* Writer-side max-nesting was planned, did not make it
* Maximum input (input doc) size also planned but not included

-----

## Full Change list

### Changes, core

#### [Annotations](../../jackson-annotations)

* [#211](../../jackson-annotations/issues/211): Add `JsonFormat.Feature`s: READ_UNKNOWN_ENUM_VALUES_AS_NULL, READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE
* [#214](../../jackson-annotations/issues/214): Add NOTICE file with copyright information
* [#221](../../jackson-annotations/issues/221): Add `JsonFormat.Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS`

#### [Streaming](../../jackson-core)

* [#815](../../jackson-core/issues/815): Add numeric value size limits via `StreamReadConstraints` (fixes `sonatype-2022-6438`)
* [#844](../../jackson-core/issues/844): Add SLSA provenance via build script
* [#851](../../jackson-core/pull/851): Add `StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER` to enable faster `BigDecimal`, `BigInteger` parsing
* [#863](../../jackson-core/issues/863): Add `StreamReadConstraints` limit for longest textual value to allow (default: 5M)
* [#865](../../jackson-core/issues/865): Optimize parsing 19 digit longs
* [#897](../../jackson-core/issues/897): Note that jackson-core 2.15 is now a multi-release jar (for more optimized number parsing for JDKs beyond 8)
* [#898](../../jackson-core/issues/898): Possible flaw in `TokenFilterContext#skipParentChecks()`
* [#902](../../jackson-core/issues/902): Add `Object JsonParser.getNumberValueDeferred()` method to allow for deferred decoding in some cases
* [#921](../../jackson-core/issues/921): Add `JsonFactory.Feature.CHARSET_DETECTION` to disable charset detection 
* [#943](../../jackson-core/pull/943): Add `StreamReadConstraints.maxNestingDepth()` to constraint max nesting depth (default: 1000)
* [#948](../../jackson-core/issues/948): Use `StreamConstraintsException` in name canonicalizers
* [#962](../../jackson-core/issues/962): Offer a way to directly set `StreamReadConstraints` via `JsonFactory` (not just Builder)
* [#968](../../jackson-core/issues/968): Prevent inefficient internal conversion from `BigDecimal` to `BigInteger` wrt ultra-large scale
* [#984](../../jackson-core/issues/984): Add `JsonGenerator.copyCurrentEventExact` as alternative to `copyCurrentEvent()`
* Build uses package type "jar" but still produces valid OSGi bundle (changed needed to keep class timestamps with Reproducible Build)

#### [Databind](../../jackson-databind)

* [#2536](../../jackson-databind/issues/2536): Add `EnumFeature.READ_ENUM_KEYS_USING_INDEX` to work with existing "WRITE_ENUM_KEYS_USING_INDEX"
* [#2667](../../jackson-databind/issues/2667): Add `@EnumNaming`, `EnumNamingStrategy` to allow use of naming strategies for Enums
* [#2968](../../jackson-databind/issues/2968): Deserialization of `@JsonTypeInfo` annotated type fails with missing type id even for explicit concrete subtypes
* [#2974](../../jackson-databind/issues/2974): Null coercion with `@JsonSetter` does not work with `java.lang.Record`
* [#2992](../../jackson-databind/issues/2992): Properties naming strategy do not work with Record
* [#3053](../../jackson-databind/issues/3053): Allow serializing enums to lowercase (`EnumFeature.WRITE_ENUMS_TO_LOWERCASE`)
* [#3180](../../jackson-databind/issues/3180): Support `@JsonCreator` annotation on record classes
* [#3262](../../jackson-databind/issues/3262): `InvalidDefinitionException` when calling `mapper.createObjectNode().putPOJO`
* [#3297](../../jackson-databind/issues/3297): `@JsonDeserialize(converter = ...)` does not work with Records
* [#3342](../../jackson-databind/issues/3342): `JsonTypeInfo.As.EXTERNAL_PROPERTY` does not work with record wrappers
* [#3352](../../jackson-databind/issues/3352): Do not require the usage of opens in a modular app when using records
* [#3566](../../jackson-databind/issues/3566): Cannot use both `JsonCreator.Mode.DELEGATING` and `JsonCreator.Mode.PROPERTIES` static creator factory methods for Enums
* [#3637](../../jackson-databind/issues/3637): Add enum features into `@JsonFormat.Feature`
* [#3638](../../jackson-databind/issues/3638): Case-insensitive and number-based enum deserialization are (unnecessarily) mutually exclusive
* [#3651](../../jackson-databind/issues/3651): Deprecate "exact values" setting from `JsonNodeFactory`, replace with `JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES`
* [#3654](../../jackson-databind/issues/3654): Infer `@JsonCreator(mode = Mode.DELEGATING)` from use of `@JsonValue`)
* [#3676](../../jackson-databind/issues/3676): Allow use of `@JsonCreator(mode = Mode.PROPERTIES)` creator for POJOs with "empty String" coercion
* [#3680](../../jackson-databind/issues/3680): Timestamp in classes inside jar showing 02/01/1980
* [#3682](../../jackson-databind/issues/3682): Transient `Field`s are not ignored as Mutators if there is visible Getter
* [#3690](../../jackson-databind/issues/3690): Incorrect target type for arrays when disabling coercion
* [#3708](../../jackson-databind/issues/3708): Seems like `java.nio.file.Path` is safe for Android API level 26
* [#3730](../../jackson-databind/issues/3730): Add support in `TokenBuffer` for lazily decoded (big) numbers
* [#3736](../../jackson-databind/issues/3736): Try to avoid auto-detecting Fields for Record types
* [#3742](../../jackson-databind/issues/3742): schemaType of `LongSerializer` is wrong
* [#3745](../../jackson-databind/issues/3745): Deprecate classes in package `com.fasterxml.jackson.databind.jsonschema`
* [#3748](../../jackson-databind/issues/3748): `DelegatingDeserializer` missing override of `getAbsentValue()` (and couple of other methods)
* [#3771](../../jackson-databind/issues/3771): Classloader leak: DEFAULT_ANNOTATION_INTROSPECTOR holds annotation reference
* [#3791](../../jackson-databind/pull/3791): Flush readonly map together with shared on `SerializerCache.flush()`
* [#3796](../../jackson-databind/issues/3796): Enum Deserialisation Failing with Polymorphic type validator
* [#3809](../../jackson-databind/issues/3809): Add Stream-friendly alternative to `JsonNode.fields()`: `Set<Map.Entry<String, JsonNode>> properties()`
* [#3814](../../jackson-databind/issues/3814): Enhance `StdNodeBasedDeserializer` to support `readerForUpdating`
* [#3816](../../jackson-databind/issues/3816): `TokenBuffer`does not implement `writeString(Reader reader, int len)`
* [#3819](../../jackson-databind/pull/3819): Add convenience method `SimpleBeanPropertyFilter.filterOutAll()` as symmetric counterpart of `serializeAll()`
* [#3836](../../jackson-databind/issues/3836): `Optional<Boolean>` is not recognized as boolean field
* [#3853](../../jackson-databind/issues/3853): Add `MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES` to enable/disable strict subtype Type Id handling
* [#3876](../../jackson-databind/issues/3876): `TypeFactory` cache performance degradation with `constructSpecializedType()`

### Changes, data formats

#### CBOR

* [#347](../../jackson-dataformats-binary/issues/347): Add support for CBOR stringref extension (`CBORGenerator.Feature.STRINGREF`)
* [#356](../../jackson-dataformats-binary/issues/356): Add `CBORGenerator.Feature.WRITE_MINIMAL_DOUBLES` for writing `double`s as `float`s if safe to do so
* [#373](../../jackson-dataformats-binary/issues/373): Remove optimized `CBORParser.nextTextValue()` implementation

#### TOML

* [#387](../../jackson-dataformats-text/issues/387): Stack overflow (50083) found by OSS-Fuzz
* [#411](../../jackson-dataformats-text/issues/411): Fuzzer-found issue #57237 (buffer boundary condition)

#### [XML](../../jackson-dataformat-xml)

* [#286](../../jackson-dataformat-xml/issues/286): Conflict between `@JsonIdentityInfo` and Unwrapped Lists
* [#533](../../jackson-dataformat-xml/issues/533): (Android) java.lang.NoClassDefFoundError: Failed resolution of: Ljavax/xml/stream/XMLInputFactory
* [#542](../../jackson-dataformat-xml/issues/542): `XmlMapper` does not find no-argument record constructor for deserialization of empty XML
* [#547](../../jackson-dataformat-xml/issues/547): Parsing empty tags without default no-arguments constructor fails in 2.14
* [#560](../../jackson-dataformat-xml/issues/560): Add `DefaultXmlPrettyPrinter.withCustomNewLine()` to configure linefeed for XML pretty-printing
* [#578](../../jackson-dataformat-xml/issues/578): `XmlMapper` serializes `@JsonAppend` property twice
* [#584](../../jackson-dataformat-xml/issues/584):  Deserialization of `null` String values in Arrays / `Collection`s not working as expected

#### YAML

* [#373](../../jackson-dataformats-text/issues/373): Positive numbers with plus sign not quoted correctly with `ALWAYS_QUOTE_NUMBERS_AS_STRINGS`
* [#388](../../jackson-dataformats-text/issues/388): Add `YAMLParser.Feature.PARSE_BOOLEAN_LIKE_WORDS_AS_STRINGS` to allow parsing "boolean" words as strings instead of booleans
* [#390](../../jackson-dataformats-text/pull/390): Upgrade to Snakeyaml 2.0 (resolves CVE-2022-1471)
* [#415](../../jackson-dataformats-text/pull/415): Use `LoaderOptions.allowDuplicateKeys` to enforce duplicate key detection

### Changes, datatypes

#### Guava

* [#7](../../jackson-datatypes-collections/issues/7): Add support for `WRITE_SORTED_MAP_ENTRIES` for Guava `Multimap`s
* [#92](../../jackson-datatypes-collections/issues/92): `@JsonDeserialize.contentConverter` does not work for non-builtin collections
* [#102](../../jackson-datatypes-collections/issues/102): accept lowerCase enums for `Range` `BoundType` serialization
* [#105](../../jackson-datatypes-collections/issues/105): Update default Guava dependency for Jackson 2.15 from Guava 21.0 to 23.6.1-jre

#### [Hibernate](../../jackson-datatype-hibernate)

* [#158](../../jackson-datatype-hibernate/issues/158): Add `jackson-datatype-hibernate6` for Hibernate 6

#### [Java 8 date/time](../../jackson-modules-java8/datetime)

* [#259](../../jackson-modules-java8/issues/259): Wrong module auto-registered when using JPMS
* [#266](../../jackson-modules-java8/pull/266): Optimize `InstantDeserializer` method `replaceZeroOffsetAsZIfNecessary()`
* [#267](../../jackson-modules-java8/pull/267): Normalize zone id during ZonedDateTime deserialization

#### [JSONP/JSR-353](../../jackson-datatypes-misc)

* [#31](../../jackson-datatypes-misc/pull/31): Fix issue with `BigInteger` handling
* [#34](../../jackson-datatypes-misc/pull/34): Upgrade `jakarta.json-api` dependency to 2.1.1 (from 2.0.0)

#### [org.json](../../jackson-datatypes-misc)

* [#35](../../jackson-datatypes-misc/issues/35): Update `org.json` dependency from `20190722` to `20230227`


### Changes, Other modules

#### Afterburner

* [#190](../../jackson-modules-base/issues/190): Filter annotated by JsonInclude.Include.CUSTOM does not get called if the field is null with Afterburner/Blackbird module registered 

#### [JSON Schema](../../jackson-module-jsonSchema/)

* [#151](../../jackson-module-jsonSchema/pull/151): Support jakarta EE 9: split into 2 modules, old `jackson-module-jsonSchema` and new `jackson-module-jsonSchema-jakarta`

### Changes, JVM Languages

#### [Kotlin](../../jackson-module-kotlin)

* [#396](../../jackson-module-kotlin/issues/396): (regression) no default no-arguments constructor found
* [#554](../../jackson-module-kotlin/issues/554): Add extension function for addMixin.
* [#580](../../jackson-module-kotlin/issues/580): Lazy load UNIT_TYPE
* [#627](../../jackson-module-kotlin/issues/627): Merge creator cache for Constructor and Method
* [#628](../../jackson-module-kotlin/issues/628): Remove unnecessary cache
* [#629](../../jackson-module-kotlin/issues/629): Changed to not cache valueParameters
* [#631](../../jackson-module-kotlin/issues/631): Fix minor bugs in SimpleModule.addSerializer/addDeserializer
* [#634](../../jackson-module-kotlin/issues/634): Fix ReflectionCache to be serializable
* [#641](../../jackson-module-kotlin/issues/641): Fixed is-getter names to match parameters and fields (NB: this changes behavior for some use cases)
* [#646](../../jackson-module-kotlin/issues/646): Drop Kotlin 1.4 support from Kotlin module 2.15
* [#647](../../jackson-module-kotlin/issues/647): Added deprecation to MissingKotlinParameterException
* [#654](../../jackson-module-kotlin/issues/654): Change MKPE.parameter property to transient(fixes #572)

#### [Scala](../../jackson-module-scala)

* [#622](../../jackson-module-scala/pull/622): Remove use of `ClassKey`
* [#628](../../jackson-module-scala/issues/628): Cannot deserialize `None` values in a tuple

### Changes, Providers

#### [JAX-RS Providers](../../jackson-jaxrs-providers)

* [#170](../../jackson-jaxrs-providers/issues/170): Add `JaxRsFeature.READ_FULL_STREAM` to consume all content, on by default

#### [Jakarts-RS Providers](../../jackson-jakarta-rs-providers)

* [#16](../../jackson-jakarta-rs-providers/issues/16): Add `JakartaRsFeature.READ_FULL_STREAM` to consume all content, on by default

