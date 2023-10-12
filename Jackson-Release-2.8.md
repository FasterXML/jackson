Version 2.8 was released 4th of July, 2016.

## Status

2.8 branch is closed and no new (full) patch releases are planned: micro-patch have been released but will NOT be released after end of 2020.

## Patches

Beyond initial 2.8.0 (described here), following patch releases have been made or are planned:

* [2.8.1](Jackson-Release-2.8.1) (20-Jul-2016)
* [2.8.2](Jackson-Release-2.8.2) (30-Aug-2016)
* [2.8.3](Jackson-Release-2.8.3) (18-Sep-2016)
* [2.8.4](Jackson-Release-2.8.4) (14-Oct-2016)
* [2.8.5](Jackson-Release-2.8.5) (14-Nov-2016)
* [2.8.6](Jackson-Release-2.8.6) (12-Jan-2017)
* [2.8.7](Jackson-Release-2.8.7) (21-Feb-2017)
* [2.8.8](Jackson-Release-2.8.8) (05-Apr-2017)
* [2.8.9](Jackson-Release-2.8.9) (12-Jun-2017)
* [2.8.10](Jackson-Release-2.8.10) (24-Aug-2017)
* [2.8.11](Jackson-Release-2.8.11) (24-Dec-2017)

At this point branch is not open any more (that is, no more full patch releases are planned).
As usual, micro-patches possible after this point for individual components: these may be made for critical security or stability issues.

Following micro-patches have been released:

* `jackson-databind` `2.8.11.1` (11-Feb-2018)
    * [#1872](../../jackson-databind/issues/1872): `NullPointerException` in `SubTypeValidator.validateSubType` when validating Spring interface
    * [#1899](../../jackson-databind/issues/1899): Another two gadgets to exploit default typing issue in jackson-databind
    * [#1931](../../jackson-databind/issues/1931): Two more `c3p0` gadgets to exploit default typing issue
* `jackson-databind` `2.8.11.2` (08-Jun-2018)
    * [#1941](../../jackson-databind/issues/1941): `TypeFactory.constructFromCanonical()` throws NPE for Unparameterized generic canonical strings
    * [#2032](../../jackson-databind/issues/2032): CVE-2018-11307: Potential information exfiltration with default typing, serialization gadget from MyBatis
    * [#2052](../../jackson-databind/issues/2052): CVE-2018-12022: Block polymorphic deserialization of types from Jodd-db library
    * [#2058](../../jackson-databind/issues/2058): CVE-2018-12023: Block polymorphic deserialization of types from Oracle JDBC driver
* `jackson-databind` `2.8.11.3` (23-Nov-2018)
    * [#2097](../../jackson-databind/issues/2097): Block more classes from polymorphic deserialization (CVE-2018-14718 - CVE-2018-14721)
    * [#2109](../../jackson-databind/issues/2109): Canonical string for reference type is built incorrectly
    * [#2186](../../jackson-databind/issues/2186): Block more classes from polymorphic deserialization (CVE-2018-19360, CVE-2018-19361, CVE-2018-19362)
* `jackson-databind` `2.8.11.4` (25-Jul-2019)
    * [#2326](../../jackson-databind/issues/2326): Block class for CVE-2019-12086
    * [#2334](../../jackson-databind/issues/2334): Block class for CVE-2019-12384
    * [#2341](../../jackson-databind/issues/2341): Block class for CVE-2019-12814
    * [#2387](../../jackson-databind/issues/2387): Block class for CVE-2019-14379
    * [#2389](../../jackson-databind/issues/2389): Block class for CVE-2019-14439
* `jackson-databind` `2.8.11.5` (10-Feb-2020)
    * [#2410](../../jackson-databind/issues/): Block one more gadget type (HikariCP, CVE-2019-14540)
    * [#2420](../../jackson-databind/issues/): Block one more gadget type (cxf-jax-rs)
    * [#2449](../../jackson-databind/issues/): Block one more gadget type (HikariCP, CVE-2019-14439 / CVE-2019-16335)
    * [#2460](../../jackson-databind/issues/): Block one more gadget type (ehcache, CVE-2019-17267)
    * [#2462](../../jackson-databind/issues/): Block two more gadget types (commons-configuration/-2)
    * [#2469](../../jackson-databind/issues/): Block one more gadget type (xalan2)
    * [#2478](../../jackson-databind/issues/): Block two more gadget types (commons-dbcp, p6spy, CVE-2019-16942 / CVE-2019-16943)
    * [#2498](../../jackson-databind/issues/): Block one more gadget type (apache-log4j-extras/1.2, CVE-2019-17531)
    * [#2526](../../jackson-databind/issues/): Block two more gadget types (ehcache/JNDI - CVE-2019-20330)
    * [#2620](../../jackson-databind/issues/): Block one more gadget type (xbean-reflect/JNDI - CVE-2020-8840)
* `jackson-databind` `2.8.11.6` (10-Mar-2020)
    * [#2526](../../jackson-databind/issues/2526): Block two more gadget types (ehcache/JNDI - CVE-2019-20330)
    * [#2620](../../jackson-databind/issues/2620): Block one more gadget type (xbean-reflect/JNDI - CVE-2020-8840)
    * [#2631](../../jackson-databind/issues/2631): Block one more gadget type (shaded-hikari-config, CVE-2020-9546)
    * [#2634](../../jackson-databind/issues/2634): Block two more gadget types (ibatis-sqlmap, anteros-core; CVE-2020-9547 / CVE-2020-9548)
    * [#2642](../../jackson-databind/issues/2642): Block one more gadget type (javax.swing, CVE-to-be-allocated)
    * [#2648](../../jackson-databind/issues/2648): Block one more gadget type (shiro-core, CVE-to-be-allocated)

* `jackson-module-kotlin` `2.8.11.1` (10-Feb-2018)
    * Upgrade to work Kotlin 1.2(.21)

and the latest `jackson-bom` that contain the very latest set including above micro-patches is: 

* [2.8.11.20200310](https://mvnrepository.com/artifact/com.fasterxml.jackson/jackson-bom/2.8.11.20200310) -- up to `jackson-databind` 2.8.11.6, `jackson-module-kotlin` 2.8.11.1

## JDK Compatibility

JDK compatibility changed since Jackson 2.7 so that

* JDK baseline: JDK 7 (up from JDK 6 for 2.7) for most components
* JDK 6 still for: `jackson-annotations`, `jackson-core`
* JDK 8 still for: Java 8 - specific modules (Java 8 datatypes, parameter names, date/time)

## New Modules, status changes

### Dataformat: Java Properties

First official version of https://github.com/FasterXML/jackson-dataformat-properties!

### Datatypes: JAX-RS 2 types module

A minor inclusion within JAX-RS providers sub-project, there is now `jackson-datatype-jaxrs`
module, which adds support for compact serialization of `javax.ws.rs.core.Link`

### Possible maintenance changes

Support: following modules may become "unsupported" if no maintainers are found:

* Hibernate
* JSON Schema

Additionally, following modules are looking for more developers:

* Date/time modules (JSR-310, Joda)

(note: a new owner was found for Scala module so it is not at risk)

## Changes: compatibility

All language features (and `Throwable.addSuppressed()` for try-catch) of Java 7 will become available for components other than `jackson-annotations` and `jackson-core` (streaming).
Bytecode target level is now `1.7`, so that jar CAN NOT be used on JDK 6 any more, JDK 7 is required.
This means that for Android 4, API-level 19 (Android 4.4?) is needed (see [this SO entry](http://stackoverflow.com/questions/20480090/does-android-support-jdk-6-or-7/22303654#22303654) for details)

Despite initial plans, 2.8 does NOT contain any of Java 8 features embedded, partly due to problems ensuring that Creator handling works without backwards compatibility issues. Creator handling is hoped to be rewritten in 2.9, at which point inclusion could be reconsidered.

Other changes that may have compatibility consequences:

* [Databind](../../jackson-databind):
    * Changes to many `JsonMappingException`, due to unified handling as part of improvements to `DeserializationProblemHandler`, including changes to wording of exception message. Code that relies on exact exception message wording of earlier versions may not match new wording. We will try to minimize wording changes going forward; 2.8 should be the new stable baseline
* [JAX-RS](../../jackson-jaxrs-providers):
    * To resolve issue [#22](../../jackson-jaxrs-providers/issues/22) (see below) will remove `@JsonProvider` annotation from `ExceptionMapper`s, to make it possible to omit mappers Jackson provides. But this will also mean that anyone relying on automatic registration of these mappers via classpath introspection will be missing mappers.
* [XML](../../jackson-dataformat-xml)
    * Made [Woodstox](https://github.com/FasterXML/woodstox) Maven dependency `compile` (instead of `test`), since Woodstox is superior for XML handling compared to JDK-bundled default Stax implementation.
        * Should not cause significant behavioral changes -- especially no changes to the logical XML content produced -- but may change some aspects like choice of default namespace prefixes, or choice of empty vs open/close tags, or optional escaping, and thereby break unit tests that use exact String comparison for results
* [YAML](../../jackson-dataformat-yaml)
    * `SnakeYAML` dependency not shaded any more (see [#31](../../jackson-dataformat-yaml/issues/31)); also now wraps all SnakeYAML exceptions as regular Jackson exceptions
* JDK types
    * `java.nio.Path` serialization slightly changed, as per [#1235](../../jackson-databind/issues/1235), to support wider range of possible paths.

## Major features of 2.8

### Configuration Overrides

One major improvement area that was not tackled in 2.7 was the ability to specify per-type configuration settings for things like formatting and serialization inclusion, such that settings:

1. Do override global defaults (such as default date format)
2. Are based on declared property type
3. May be further overridden by per-property annotations (direct or mix-ins)

Some examples include:

* Ability to specify `DateFormat` to use only for `java.util.Date` but not Java 8 or Joda date types (or vice versa)
* Ability to specify that every `String[]` valued property allows "deserialize single value as Array"
* Inclusion criteria for POJO type `MyType` so that properties with that type are not included if they are `null`
* Enable "ignore unknown properties" for `LooseType`-valued properties
* Specify `InternalType` as "ignored type", similar to having been annotated with `@JsonIgnoreType`

Chosen implementation mechanism is the new "type config overrides" mechanism, which may be used like so

```java
ObjectMapper mapper = ...;
mapper.configOverride(Wrapped.class).setIsIgnoredType(true);
```

to handle the last listed use case: in this case, make sure that all properties of type `Wrapped` will be excluded from serialization and deserialization.

Other examples include:

```java
mapper.configOverride(String[].class).setFormat(JsonFormat.Value.empty()
    .withFeature(JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED));
```

This new "configuration override" mechanism supports a subset of Class annotations, and does not yet have anything for per-property annotations. It should, however, be relatively straight-forward to extend similar approach to overrides of actual property.
Loose equivalence is included for following annotation types:

* `@JsonFormat`
* `@JsonInclude`
* `@JsonIgnoreType`
* `@JsonIgnoreProperties`

### Major additions to `DeserializationProblemHandler`

(and related changes in `DeserializationContext`)

One are of improvement that was added recently (and not "inherited" from 2.7) is that of explicit error recovery. Jackson already had pluggable handler, `DeserializationProblemHandler` for application code to use,
but it only contained one handling method (`handleUnknownProperty`) and was only capable of recovering from that one type of issue.

With 2.8 things were radically refactored to add following handler methods:

* `handleWeirdKey`: called when key for `Map` valued property can not be mapped to expected key type
* `handleWeirdStringValue`: called when specific JSON String can not be deserialized into value, in cases where other String values are acceptable
* `handleWeirdNumberValue`: called when specific JSON Number can not be deserialized into value, in cases where other String values are acceptable
* `handleUnexpectedToken`: called when specific `JsonToken` is not of supported type for target Java type (like `START_ARRAY` for Java String
* `handleInstantiationProblem` / `handleMissingInstantiator`: when either call via constructor/factory method failed, or no instantiator was available
* `handleUnknownTypeId`: called when Type Id (for polymorphic deserialization) can not be converted (missing mapping, for example)

These methods will get invoked via `DeserializationContext` that has similarly named methods that are called by actual deserializers; support for handling therefore requires some co-operation of deserializer implementations.

Handler methods may try to recover from the failure (either for full processing, or to keep track of all failure types), or provide more meaningful exception messages based on contextual information.
It is possible that with future versions we may try to add some form of "do not fail on first problem", aggregation of multiple failures, but with 2.8 this is just foundational support for building such support externally.

### Parser support `DataInput` source (for some formats)

One potentially significant improvement is possibility of using `java.io.DataInput` as input source.
Due to strict limitations with look-ahead, this input source can only be supported for some formats;
and with 2.8 following formats now support it:

* JSON

In addition it seems plausible that at least following format backends may be retrofitted if there is demand:

* Avro
* CBOR
* Smile
* XML (but only theoretically as this requires XML parser support)

As to others, following are unlikely to be supported due to format limitations:

* Protobuf (no framing for main-level records)
* CSV, Properties, YAML (similarly open-ended, no end marker)

### Generator support for `DataOutput`

As opposed to `DataInput`, adding ability to write using `DataOutput` is trivial, so support has been added for all backends

### Generator support for "array writes": `int[]`, `long[]`, `double[]`

Since some binary data formats have optimized handling for things like packed numeric arrays, a small set of extension methods (name `writeArray()`) were added in `JsonGenerator`, along with default implementation that simply delegates to matching write methods.

But for a small set of dataformats:

* CBOR
* Protobuf
* Smile

additional support was included to make actual operation significantly more efficient (and in case of CBOR, also uses definite count arrays instead of start/end marker). These methods may be called directly (when working directly on Streaming API), but they are also used for properties with value of:

* `int[]`
* `long[]`
* `double[]`

In addition it would be possible to add explicit support for Avro backend; and if/when CBOR specification is augmented with support for "packed arrays", `jackson-dataformat-cbor` may be augmented to make use of such format improvement.

## Postponed Features (at least until 2.9)

Version 2.7 version had ambitious goals, but many of features originally planned had to be postponed to ensure that the main features started could be completed in time.
Some of these features did not make it into 2.8 either; here's an overview.

### Per-property "merge" option

Currently all property values are newly created "virgin" Objects. But sometimes it would be useful to see if property already had a default value (set by parent POJO in constructor, typically), and if so, modify that value.

Attempt will be made to include this in 2.9.

### Add per-property custom Serialization Inclusion mechanism

While it is possible to use `@JsonInclude` on properties, all choices use pre-defined rules; or, in case of `NON_EMPTY` (and by extension, `NON_ABSENT`) require matching `JsonSerializer.isEmpty(...)` to be implemented.

But it should be possible to have a simpler external exclusion handling. This could work by adding support for something like:

```java
public class POJO {
  @JsonInclude(value=Include.MATCHING, matcher=MyMatcher.class)
  public Value value;
```

where an instance of `MyMatcher` will be created, and for each property value, `MyMatchr.equals()` is called to see if value matches: if it does (return `true`), value will be included; if not (`false`), it will be excluded.

Such an approach has the main benefit that since `Object.equals()` is defined for all types, no new types (interface, class) are needed. We can also easily use something like `Void.class` as the default value.

There are drawbacks as well:

1. No context object can be passed so determination must be context-free (note: annotation types also can not refer to `DatabindContext`, so context, if any, would need to be untyped as well)
2. Use of `equals()` is an abuse of semantics of the method.

Another approach would be to just create a new interface to fix the issues. Perhaps that makes more sense.

Attempt will be made to include this in 2.9.

### Allow pre-defining Object Ids; pluggable Object Id converters?

One of the things that has turned out problematic is resolution of Object References via Object Ids.
There are a few improvements that could be considered:

1. Allow pre-defining set of Object Id to instance mappings
2. Allow custom resolution of Object Id references to instances

### Fix Creator introspection

Although the hope was to add support for "annotation-less" constructors in 2.6, via implied naming, it turned out that there was one remaining big stumbling block: too-late discovery of "creator-capable" constructors.

Basically while it is possible to detect constructors that may act as Creators, due to all parameters having implicit names, this detection happens too late in processing: currently candidate properties (getters, setters, fields, constructor parameters) are collected first, and during this phase, only explicitly annotation constructors are considered.
At a later point, all constructors are scanned again, and they are matched to properties that are backed by constructor. But "implied" creators found at this point can not be matched to properties, since they were not constructed during initial pass.

So discovery needs to be rewritten to either make implied-creator detection happen earlier altogether, or to speculative add properties from constructors first, and then decide which constructor, if any, is the primary; properties attached to other constructors will then be trimmed.
Either way could potentially work; neither is trivially easy to implement.

Attempt will be made to include this in 2.9.

### Protobuf: Schema generation from POJO, building by hand

Protobuf support added in 2.6 was planned to allow 3 methods for defining `protoc` schema:

1. Read from an external representation (File, String)
2. Generate from POJO
3. Create programmatically

but due to schedule, only (1) was initially included. 2.8 does include (2), but (3) is still missing.

## Change list

### Changes, core

#### [Annotations](../../jackson-annotations)

* [#65](../../jackson-annotations/issues/65): Add new choice for `JsonFormat.Shape`, `NATURAL`
* [#79](../../jackson-annotations/issues/79): Change `@JsonTypeInfo.defaultImpl` default value to deprecate `JsonTypeInfo.None.class`
* [#83](../../jackson-annotations/issues/83): Add `@JsonEnumDefaultValue` for indicating default enum choice if no real match found
* [#87](../../jackson-annotations/issues/87): Add `@JsonEnumDefaultValue` for indicating default enum choice if no real match found
* [#89](../../jackson-annotations/issues/89): Add `JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES`
* [#95](../../jackson-annotations/issues/95): Add `JsonFormat.Feature#ADJUST_DATES_TO_CONTEXT_TIME_ZONE`

#### [Streaming](../../jackson-core)

* [#86](../../jackson-core/issues/86): Allow inclusion of request body for `JsonParseException`
* [#117](../../jackson-core/issues/117): Add `JsonParser.Feature.ALLOW_MISSING_VALUES` to support for missing values
* [#136](../../jackson-core/issues/136): Add `JsonpCharacterEscapes` for easier handling of potential problems
 with JSONP and rare but technically allowed \u2028 and \u2029 linefeed characters
* [#253](../../jackson-core/issues/253): Add `JsonGenerator. writeEmbeddedObject()` to allow writes of opaque native types
* [#257](../../jackson-core/issues/257): Add `writeStartObject(Object pojo)` to streamline assignment of current value
* [#265](../../jackson-core/issues/265): `JsonStringEncoder` should allow passing `CharSequence`
* [#276](../../jackson-core/issues/276): Add support for serializing using `java.io.DataOutput`
* [#277](../../jackson-core/issues/277): Add new scalar-array write methods for `int`/`long`/`double` cases
* [#279](../../jackson-core/issues/279): Support `DataInput` for parsing
* [#280](../../jackson-core/issues/280): Add `JsonParser.finishToken()` to force full, non-lazy reading of current token
* [#281](../../jackson-core/issues/281): Add `JsonEOFException` as sub-class of `JsonParseException`
* [#282](../../jackson-core/issues/282): Fail to report error for trying to write field name outside Object (root level)
* [#285](../../jackson-core/issues/285): Add `JsonParser.getText(Writer)`
* [#290](../../jackson-core/issues/290): Add `JsonGenerator.canWriteFormattedNumbers()` for introspection
* [#294](../../jackson-core/issues/294): Add `JsonGenerator.writeFieldId(long)` method to support binary formats with non-String keys
* [#296](../../jackson-core/issues/296): JsonParserSequence skips a token on a switched Parser

#### [Databind](../../jackson-databind)

* [#621](../../jackson-databind/issues/621]: Allow definition of "ignorable types" without annotation (using
  `Mapper.configOverride(type).setIsIgnoredType(true)`
* [#867](../../jackson-databind/issues/867): Support `SerializationFeature.WRITE_EMPTY_JSON_ARRAYS` for `JsonNode`
* [#903](../../jackson-databind/issues/903): Add `JsonGenerator` reference to `SerializerProvider`
* [#931](../../jackson-databind/issues/931): Add new method in `Deserializers.Base` to support `ReferenceType`
* [#960](../../jackson-databind/issues/960): `@JsonCreator` not working on a factory with no arguments for an enum type
* [#990](../../jackson-databind/issues/990): Allow failing on `null` values for creator (add `DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES`)
* [#1017](../../jackson-databind/issues/#1017): Add new mapping exception type ('InvalidTypeIdException') for subtype resolution errors
* [#1028](../../jackson-databind/issues/1028): Ignore `USE_BIG_DECIMAL_FOR_FLOATS` for `NaN`/`Infinity`
* [#1047](../../jackson-databind/issues/1047): Allow use of `@JsonAnySetter` on a Map-valued field, no need for setter
* [#1082](../../jackson-databind/issues/1082): Can not use static Creator factory methods for `Enum`s, with `JsonCreator.Mode.PROPERTIES`
* [#1084](../../jackson-databind/issues/1084): Change `TypeDeserializerBase` to take `JavaType` for `defaultImpl`, not `Class`
* [#1126](../../jackson-databind/issues/1126): Allow deserialization of unknown Enums using a predefined value (annotated with `@JsonEnumDefaultValue`)
* [#1136](../../jackson-databind/issues/1136): Implement `TokenBuffer.writeEmbeddedObject(Object)`
* [#1184](../../jackson-databind/issues/1184): Allow overriding of `transient` with explicit inclusion with `@JsonProperty`
* [#1207](../../jackson-databind/issues/1207): Add new method(s) in `DeserializationProblemHandler` to allow handling of format mismatch problems
* [#1221](../../jackson-databind/issues/1221): Use `Throwable.addSuppressed()` directly and/or via try-with-resources
* [#1232](../../jackson-databind/issues/1232): Add support for `JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES`
* [#1233](../../jackson-databind/issues/1233): Add support for `JsonFormat.Feature.WRITE_SORTED_MAP_ENTRIES`
* [#1235](../../jackson-databind/issues/1235): `java.nio.file.Path` support incomplete

### Changes, [JAX-RS Providers](../../jackson-jaxrs-providers)

* [#22](../../jackson-jaxrs-providers/issues/22): Remove `@Provider` annotation from `JsonParseExceptionMapper` and `JsonMappingExceptionMapper`
* [#48](../../jackson-jaxrs-providers/issues/48): Support compact serialization of `javax.ws.rs.core.Link`, deserialization
* [#82](../../jackson-jaxrs-providers/issues/82): Upgrade JAX-RS dependency to 2.0

### Changes, datatypes

#### [Java 8 Date/time](../../jackson-datatype-jsr310)

* [#78](../../jackson-datatype-jsr310/issues/78): output LocalDate JSON Schema format as DATE
* [#80](../../jackson-datatype-jsr310/issues/80): Add Support for `JsonFormat.Feature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE`

#### [Joda](../../jackson-datatype-joda)

* [#83](../../jackson-datatype-joda/issues/83): `WRITE_DATES_WITH_ZONE_ID` feature not working when applied on `@JsonFormat` annotation
* [#87](../../jackson-datatype-joda/issues/87): Add support for `JsonFormat.Feature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE`

### Changes, data formats

#### [CBOR](../../jackson-dataformats-binary)

* [#16](../../jackson-dataformats-binary/issues/16): Implement `JsonGenerator.writeArray()` methods added in `jackson-core`
* [#17](../../jackson-dataformats-binary/issues/17): Support parsing of `BigInteger`, `BigDecimal`, not just generating
* [#18](../../jackson-dataformats-binary/issues/18): Fail to report error for trying to write field name outside Object (root level)
* [#24](../../jackson-dataformats-binary/issues/24): Incorrect coercion for int-valued Map keys to String

#### [Smile](../../jackson-dataformats-binary)

* [#](../../jackson-dataformats-binary/issues/19): Fail to report error for trying to write field name outside Object (root level)

#### [XML](../jackson-dataformat-xml)

* [#196](../jackson-dataformat-xml/issues/196): Mixed content not supported if there are child elements

#### [YAML](../jackson-dataformat-yaml)

* [#31](../jackson-dataformat-yaml/issues/31): SnakeYAML is shaded and pulled in transitively)
* [#60](../jackson-dataformat-yaml/issues/60): YAML serializer reports wrong exception


### Changes, [Jackson jr](../../jackson-jr)

* [#43](../../jackson-jr/issues/43): Add convenience read method (`treeFrom()`) for reading trees via `JSON`
* [#26](../../jackson-jr/issues/26): Allow use of public fields for getting/setting values

#### Changes, [Kotlin Module](https://github.com/FasterXML/jackson-module-kotlin)

* Update to Kotlin 1.0.3
* [#26](https://github.com/FasterXML/jackson-module-kotlin/issues/26): Default values for primitive parameters
* [#29](https://github.com/FasterXML/jackson-module-kotlin/issues/29): Problems deserializing object when default values for constructor parameters are used
* Added checks explicitly for nullable values being used in constructor or creator static methods to not allow NULL values into non-nullable types