[Jackson Version](Jackson-Releases) 2.9 was released on July 30th 2017.

There is a longer [blog entry](https://medium.com/@cowtowncoder/jackson-2-9-features-b2a19029e9ff) on major features, above and beyond information here.

## Status

Branch is not open for general releases and `2.9.10` was the last full patch release.
Multiple micro-patch versions have been released (see below), but with release of `2.9.10.8` on January 6, 2021, the branch is now officially closed and no releases of any kind are planned.

## Patches

Beyond initial 2.9.0 (described here), following patch versions were released:

* [2.9.1](Jackson-Release-2.9.1) (07-Sep-2017)
* [2.9.2](Jackson-Release-2.9.2) (14-Oct-2017)
* [2.9.3](Jackson-Release-2.9.3) (09-Dec-2017)
* [2.9.4](Jackson-Release-2.9.4) (24-Jan-2018)
* [2.9.5](Jackson-Release-2.9.5) (26-Mar-2018)
* [2.9.6](Jackson-Release-2.9.6) (12-Jun-2018)
* [2.9.7](Jackson-Release-2.9.7) (19-Sep-2018)
* [2.9.8](Jackson-Release-2.9.8) (15-Dec-2018)
* [2.9.9](Jackson-Release-2.9.9) (16-May-2019)
* [2.9.10](Jackson-Release-2.9.10) (21-Sep-2019) -- see notes below on micro-patches

### Micro-patches

Following micro-patches have been or will be released:

* `jackson-databind` `2.9.9.1` (03-Jul-2019)
    * [#2334](../../jackson-databind/issues/2334): Block one more gadget type (CVE-2019-12384)
    * [#2341](../../jackson-databind/issues/2341): Block one more gadget type (CVE-2019-12814)
    * [#2375](../../jackson-databind/issues/2374): `ObjectMapper. getRegisteredModuleIds()` throws NPE if no modules registered
* `jackson-databind` `2.9.9.2` (27-Jul-2019) -- with `jackson-bom` version `2.9.9.20190727`
    * [#2331](../../jackson-databind/issues/2331): `JsonMappingException` through nested getter with generic wildcard return type
    * [#2387](../../jackson-databind/issues/2387): Block one more gadget type (CVE-2019-14379)
    * [#2389](../../jackson-databind/issues/2389): Block one more gadget type (CVE-2019-14439)
* `jackson-databind` `2.9.9.3` (06-Aug-2019) -- with `jackson-bom` version `2.9.9.20190807`
    * [#2395](../../jackson-databind/issues/2395): `NullPointerException` from `ResolvedRecursiveType` (regression due to fix for #2331)
* `jackson-databind` `2.9.10.1` (20-Oct-2019) -- with `jackson-bom` version `2.9.10.20191020`
   * [#2478](../../jackson-databind/issues/2478): Block two more gadget types (commons-dbcp, p6spy, CVE-2019-16942 / CVE-2019-16943)
   * [#2498](../../jackson-databind/issues/2498): Block one more gadget type (log4j-extras/1.2, CVE-2019-17531)
* `jackson-databind` `2.9.10.2` (03-Jan-2020)
    * [#2526](../../jackson-databind/issues/2526): Block two more gadget types (ehcache/JNDI -CVE-2019-20330)
    * [#2544](../../jackson-databind/issues/2544): java.lang.NoClassDefFoundError Thrown for compact profile1
* `jackson-databind` `2.9.10.3` (23-Feb-2020) -- with `jackson-bom` version `2.9.10.20200223`
    * [#2620](../../jackson-databind/issues/2620): Block one more gadget type (xbean-reflect/JNDI - CVE-2020-8840)
* `jackson-databind` `2.9.10.4` (11-Apr-2020) -- with `jackson-bom` version `2.9.10.20200411`
    * [#2631](../../jackson-databind/issues/2631): Block one more gadget type (shaded-hikari-config, CVE-2020-9546)
    * [#2634](../../jackson-databind/issues/2634): Block two more gadget types (ibatis-sqlmap, anteros-core; CVE-2020-9547 / CVE-2020-9548)
    * [#2642](../../jackson-databind/issues/2642): Block one more gadget type (javax.swing, CVE-2020-10969)
    * [#2648](../../jackson-databind/issues/2648): Block one more gadget type (shiro-core)
    * [#2653](../../jackson-databind/issues/2653): Block one more gadget type (shiro-core)
    * [#2658](../../jackson-databind/issues/2658): Block one more gadget type (ignite-jta, CVE-2020-10650)
    * [#2659](../../jackson-databind/issues/2659): Block one more gadget type (aries.transaction.jms, CVE-2020-10672)
    * [#2660](../../jackson-databind/issues/2660): Block one more gadget type (com.caucho:quercus, CVE-2020-10673)
    * [#2662](../../jackson-databind/issues/2662): Block one more gadget type (bus-proxy, CVE-2020-10968)
    * [#2664](../../jackson-databind/issues/2664): Block one more gadget type (activemq-jms, CVE-2020-11111)
    * [#2666](../../jackson-databind/issues/2666): Block one more gadget type (apache/commons-proxy, CVE-2020-11112)
    * [#2670](../../jackson-databind/issues/2670): Block one more gadget type (openjpa, CVE-2020-11113)
    * [#2680](../../jackson-databind/issues/2680): Block one more gadget type (spring-jpa, CVE-2020-11619)
    * [#2682](../../jackson-databind/issues/2682): Block one more gadget type (commons-jelly, CVE-2020-11620)
* `jackson-databind` `2.9.10.5` (21-Jun-2020) -- with `jackson-bom` version `2.9.10.20200621`
    * [#2688](../../jackson-databind/issues/2688): Block one more gadget type (apache-drill, CVE-2020-14060)
    * [#2698](../../jackson-databind/issues/2698): Block one more gadget type (weblogic/oracle-aqjms, CVE-2020-14061)
    * [#2704](../../jackson-databind/issues/2704): Block one more gadget type (jaxp-ri, CVE-2020-14062)
    * [#2765](../../jackson-databind/issues/2765): Block one more gadget type (org.jsecurity, CVE-2020-14195)
* `jackson-databind` `2.9.10.6` (24-Aug-2020) -- with `jackson-bom` version `2.9.10.20200824`
    * [#2798](../../jackson-databind/issues/2798): Block one more gadget type (com.pastdev.httpcomponents, CVE-2020-24750
    * [#2814](../../jackson-databind/issues/2814): Block one more gadget type (Anteros-DBCP, CVE-2020-24616)
    * [#2826](../../jackson-databind/issues/2826): Block one more gadget type (com.nqadmin.rowset)
    * [#2827](../../jackson-databind/issues/2827): Block one more gadget type (org.arrahtec:profiler-core)
* `jackson-databind` `2.9.10.7` (02-Dec-2020) -- with `jackson-bom` version `2.9.10.20201202`
    * [#2589](../../jackson-databind/issues/2589):  `DOMDeserializer`: setExpandEntityReferences(false) may not prevent external entity expansion in all cases (CVE-2020-25649)
    * [#2854](../../jackson-databind/issues/2854): Block one more gadget type (javax.swing, CVE-2021-20190)
* `jackson-databind` `2.9.10.8` (06-Jan-2021) -- with `jackson-bom` version `2.9.10.20210106`
    * [#2986](../../jackson-databind/issues/2986): Block 2 more gadget types (commons-dbcp2, CVE-2020-35490/CVE-2020-35491)
    * [#2996](../../jackson-databind/issues/2996): Block 2 more gadget types (newrelic-agent, CVE-2020-36188/CVE-2020-36189)
    * [#2997](../../jackson-databind/issues/2997): Block 2 more gadget types (tomcat/naming-factory-dbcp, CVE-2020-36186/CVE-2020-36187)
    * [#2998](../../jackson-databind/issues/2998): Block 2 more gadget types (org.apache.tomcat/tomcat-dbcp, CVE-2020-36184/CVE-2020-36185)
    * [#2999](../../jackson-databind/issues/2999): Block 1 more gadget type (org.glassfish.web/javax.servlet.jsp.jstl, CVE-2020-35728)
    * [#3003](../../jackson-databind/issues/3003): Block one more gadget type (org.docx4j.org.apache:xalan-interpretive, CVE-2020-36183)
    * [#3004](../../jackson-databind/issues/3004): Block some more DBCP-related potential gadget classes (CVE-2020-36179 - CVE-2020-36182)

-----

## Changes, compatibility

### JDK

No changes from [2.8](Jackson-Release-2.8): Java 7 features available for all modules (meaning, need to compile on Java 7), JDK/JVM 7 is the baseline, except:

* `jackson-annotations`, `jackson-core` (streaming) and `jackson-jr` still only require JDK/JVM 6
* Java 8 modules, Kotlin, Scala module require JDK/JVM 8

Other modules could theoretically run on Java 6 with reduced features (i.e. avoid loading Java 7 types), but have compiled compiled with `-target 1.7` setting so bytecode unfortunately requires JDK 7.
It should be possible to post-process jars, however, to produce JDK 6 - compatible jars.

### Android

No changes: similar to Jackson 2.8 minimum is `Android 4.4`, API-level `19` (see [this dashboard](https://developer.android.com/about/dashboards/index.html) for example).

Note that version `2.7` will work with older Android versions; and there are some reports that even `2.8` may actually work (even if not specified to).

## New Modules, status changes

### Dataformat: Ion

First official version of `jackson-dataformat-ion` (under `jackson-dataformats-binary` repo) supports [Amazon Ion](https://amznlabs.github.io/ion-docs/) binary data format.

### Changes: compatibility

#### `java.sql.Date`

As per [databind#219](https://github.com/FasterXML/jackson-databind/issues/219) `java.sql.Date` will finally use same "timestamp-or-String" determination as `java.util.Date` and `java.util.Calendar`.
This means that with vanilla, unchanged settings, values will be serialized as numeric timestamps.
Note that the default String serialization will still default to `java.sql.Date.String()`, and not to default formatting `java.util.Date` uses.

## Major features of 2.9

### Implemented

1. Add separate exception type for "pojo configuration problem" (`InvalidDefinitionException`), distinct from "json input" problem (`MismatchedInputException`); in general to distinguish between bad configuration (server-side issue) and bad data (client issue) [#1356](https://github.com/FasterXML/jackson-databind/issues/1356)
2. Per-property custom serialization inclusion (`@JsonInclude(value=Include.CUSTOM, valueFilter=MyExcluder.class`) [#888](https://github.com/FasterXML/jackson-databind/issues/888)
3. Per-property overwrite-vs-merge annotation/handling, to allow for merging of configuration information (for example) [#1399](https://github.com/FasterXML/jackson-databind/issues/1399)
    * note: on-going work to increase coverage; most types should work
4. Ability to override handling of `null` for deserialization [#1402](https://github.com/FasterXML/jackson-databind/issues/1402)
5. Aliases, to allow migration: alternate property id names to accept (but not write) [#1029](https://github.com/FasterXML/jackson-databind/issues/1029)
6. Non-blocking JSON/Smile parser [#57](https://github.com/FasterXML/jackson-core/issues/57)
7. `DeserializationFeature` to automatically verify that value bound is full value and there is no trailing junk in input ("whole value verification") [#1583](https://github.com/FasterXML/jackson-databind/issues/1583)


## Postponed Features

From original "big items" list, a few were left out:

1. Ability to force "inject-only" variant of `@JacksonInject` [#1381](https://github.com/FasterXML/jackson-databind/issues/1381) -- also solves a few related issues wrt un-deserializable injectable values (which are common)
2. Allow pre-defining Object Ids; pluggable Object Id converters? [#693](https://github.com/FasterXML/jackson-databind/issues/693)
3. Rewrite of property/creator introspection code, to resolve most open issues wrt Creator auto-detection and name-detection
    * NOTE: partial rewrite occurred for 2.12
4. A way to force a supertype as type id to use during serialization: this is needed to avoid deserialization problems for things like concrete Hibernate collection type. [#789](https://github.com/FasterXML/jackson-databind/issues/789)
5. Protobuf: Schema building by hand
6. "Safe" `ObjectReader`, `ObjectWriter`; that is, ones that does NOT throw checked exceptions (wrap `IOException`s), to work better with Java 8 Streams [#779](https://github.com/FasterXML/jackson-databind/issues/779)
    * NOTE: decided against this approach -- instead, Jackson 3.0 will change `JsonProcessingException` to be unchecked, to prevent need for "safe" alternative
7. Support for `@JsonIncludeProperties` (opposite of `@JsonIgnoreProperties`) [#1296](https://github.com/FasterXML/jackson-databind/issues/1296)
    * NOTE: finally implement in [Jackson 2.1](Jackson-Release-2.12)

In addition, during development of 2.9, couple of new great ideas surfaced, but could not yet be implemented due to time constraints:

* Low-level `String` post-processor? [jackson-core#355](https://github.com/FasterXML/jackson-core/issues/355)
* Comma-separated "ints in a String" [#1242](https://github.com/FasterXML/jackson-databind/issues/1242)
* Support for automated `FormatSchema` configuration/lookup: [#1582](https://github.com/FasterXML/jackson-databind/issues/1582)
    * Would be especially useful for cases where caller does not have full control; for example, when using as extension for frameworks like JAX-RS
* `@JsonUnwrapped` improvements, which need major rewrite of unwrapped deserialization:
    * Support for `@JsonUnwrapped` as `@JsonCreator` parameter
    * Catching "unknown" properties

Ideally the next minor version (or, as the case may be, major...) -- 3.0 -- would start by considering these features as the starting Big Ticket item list.

-----

## Change list

### Changes, core

#### [Annotations](../../jackson-annotations)

* [#103](../../jackson-annotations/issues/103): Add `JsonInclude.Include.CUSTOM`, properties for specifying filter(s) to use
* [#104](../../jackson-annotations/issues/104): Add new properties in `@JsonSetter`: `nulls`/`contentNulls`
* [#105](../../jackson-annotations/issues/105): Add `@JsonFormat.lenient` to allow configuring lenience of date/time deserializers
* [#108](../../jackson-annotations/issues/108): Allow `@JsonValue` on fields
* [#109](../../jackson-annotations/issues/109): Add `enabled` for `@JsonAnyGetter`, `@JsonAnySetter`, to allow disabling via mix-ins
* [#113](../../jackson-annotations/issues/113): Add `@JsonMerge` to support (deep) merging of properties
* [#116](../../jackson-annotations/issues/116): Add `@JsonAlias` annotation to allow specifying alternate names for a property
* Allow use of `@JsonView` on classes, to specify Default View to use on non-annotated properties.

#### [Streaming](../../jackson-core)

* [#17](../../jackson-core/issues/17): Add 'JsonGenerator.writeString(Reader r, int charLength)'
* [#57](../../jackson-core/issues/57): Add support for non-blocking ("async") JSON parsing
* [#208](../../jackson-core/issues/208): Make use of `_matchCount` in `FilteringParserDelegate`
* [#306](../../jackson-core/issues/306): Add new method in `JsonStreamContext` to construct `JsonPointer`
* [#312](../../jackson-core/issues/312): Add `JsonProcessingException.clearLocation()` to allow clearing possibly security-sensitive information
* [#314](../../jackson-core/issues/314): Add a method in `JsonParser.isNan()` to allow checking for "NaN" values
* [#323](../../jackson-core/issues/323): Add `JsonParser.ALLOW_TRAILING_COMMA` to work for Arrays and Objects
* [#325](../../jackson-core/issues/325): `DataInput` backed parser should handle `EOFException` at end of doc
* [#356](../../jackson-core/issues/356): Improve indication of "source reference" in `JsonLocation` wrt `byte[]`,`char[]`
* [#374](../../jackson-core/issues/374): Minimal and DefaultPrettyPrinter with configurable separators 

#### [Databind](../../jackson-databind)

* [#219](../../jackson-databind/issues/219): `SqlDateSerializer` does not obey `SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS`
* [#291](../../jackson-databind/issues/291): `@JsonTypeInfo` with `As.EXTERNAL_PROPERTY` doesn't work if external type property is referenced more than once
* [#357](../../jackson-databind/issues/357): `StackOverflowError` with contentConverter that returns array type
* [#383](../../jackson-databind/issues/383): Recursive `@JsonUnwrapped` (`child` with same type) fail: "No _valueDeserializer assigned"
* [#403](../../jackson-databind/issues/403): Make FAIL_ON_NULL_FOR_PRIMITIVES apply to primitive arrays and other types that wrap primitives
* [#476](../../jackson-databind/issues/476): Allow "Serialize as POJO" using `@JsonFormat(shape=Shape.OBJECT)` class annotation
* [#507](../../jackson-databind/issues/507): Support for default `@JsonView` for a class
* [#888](../../jackson-databind/issues/888): Allow specifying custom exclusion comparator via `@JsonInclude`, using `JsonInclude.Include.CUSTOM`
* [#994](../../jackson-databind/issues/994): `DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS` only works for POJOs, Maps
* [#1029](../../jackson-databind/issues/1029): Add a way to define property name aliases
* [#1035](../../jackson-databind/issues/1035): `@JsonAnySetter` assumes key of `String`, does not consider declared type
* [#1106](../../jackson-databind/issues/1106): Add `MapperFeature.ALLOW_COERCION_OF_SCALARS` for enabling/disabling coercions
* [#1284](../../jackson-databind/issues/1284): Make `StdKeySerializers` use new `JsonGenerator.writeFieldId()` for `int`/`long` keys 
* [#1320](../../jackson-databind/issues/1320): Add `ObjectNode.put(String, BigInteger)`
* [#1341](../../jackson-databind/issues/1341): `DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY`
* [#1347](../../jackson-databind/issues/1347): Extend `ObjectMapper.configOverrides()` to allow changing visibility rules
* [#1356](../../jackson-databind/issues/1356): Differentiate between input and code exceptions on deserialization
* [#1369](../../jackson-databind/issues/1369]): Improve `@JsonCreator` detection via `AnnotationIntrospector` by passing `MappingConfig`
* [#1371](../../jackson-databind/issues/1371): Add `MapperFeature.INFER_CREATOR_FROM_CONSTRUCTOR_PROPERTIES` to allow disabling use of `@CreatorProperties` as explicit `@JsonCreator` equivalent
* [#1376](../../jackson-databind/issues/1376): Add ability to disable JsonAnySetter/JsonAnyGetter via mixin
* [#1399](../../jackson-databind/issues/1399): Add support for `@JsonSetter(merge=OptBoolean.TRUE`) to allow "deep update"
* [#1402](../../jackson-databind/issues/1402): Use `@JsonSetter(nulls=...)` to specify handling of `null` values during deserialization
* [#1406](../../jackson-databind/issues/1406): `ObjectMapper.readTree()` methods do not return `null` on end-of-input
* [#1407](../../jackson-databind/issues/1407): `@JsonFormat.pattern` is ignored for `java.sql.Date` valued properties
* [#1428](../../jackson-databind/issues/1428): Allow `@JsonValue` on a field, not just getter
* [#1454](../../jackson-databind/issues/1454): Support `@JsonFormat.lenient` for `java.util.Date`, `java.util.Calendar`
* [#1480](../../jackson-databind/issues/1480): Add support for serializing `boolean`/`Boolean` as number (0 or 1)
* [#1520](../../jackson-databind/issues/1520): Case insensitive enum deserialization feature.
* [#1522](../../jackson-databind/issues/1522): Global `@JsonInclude(Include.NON_NULL)` for all properties with a specific type
* [#1552](../../jackson-databind/issues/1552): Map key converted to byte array is not serialized as base64 string
* [#1554](../../jackson-databind/issues/1554): Support deserialization of `Shape.OBJECT` ("as POJO") for `Map`s (and map-like types)
* [#1556](../../jackson-databind/issues/1556): Add `ObjectMapper.updateValue()` method to update instance with given overrides
* [#1583](../../jackson-databind/issues/1583]): Add a `DeserializationFeature.FAIL_ON_TRAILING_TOKENS` to force reading of the whole input as single value
* [#1605](../../jackson-databind/issues/1605): Allow serialization of `InetAddress` as simple numeric host address
* [#1678](../../jackson-databind/issues/1678): Rewrite `StdDateFormat` ISO-8601 handling functionality

### Changes, data formats

#### [Avro](../../jackson-dataformats-binary)

* [#13](../../jackson-dataformats-binary/issues/13): Add support for Avro default values
* [#14](../../jackson-dataformats-binary/issues/14): Add support for Avro annotations via `AvroAnnotationIntrospector`
* [#15](../../jackson-dataformats-binary/issues/15): Add a way to produce "file" style Avro output
* [#56](../../jackson-dataformats-binary/issues/56): Replace use of `BinaryDecoder` with direct access
* [#57](../../jackson-dataformats-binary/issues/57): Add support for `@Stringable` annotation
* [#59](../../jackson-dataformats-binary/issues/59): Add support for `@AvroAlias` annotation for Record/Enum name evolution
* [#60](../../jackson-dataformats-binary/issues/60): Add support for `@Union` and polymorphic types
* [#63](../../jackson-dataformats-binary/issues/63): Implement native `float` handling for parser
* [#69](../../jackson-dataformats-binary/issues/69): Add support for @AvroEncode annotation
* [#95](../../jackson-dataformats-binary/issues/95): Add new method, `withUnsafeReaderSchema` in `AvroSchema` to allow avoiding verification exception 
* Upgrade `avro-core` dep from 1.7.7 to 1.8.1

#### [CSV](../../jackson-dataformat-csv)

* [#127](../../jackson-dataformat-csv/issues/127): Add `CsvGenerator.Feature.ALWAYS_QUOTE_EMPTY_STRINGS` to allow forced quoting of empty Strings
* [#130](../../jackson-dataformat-csv/issues/130): Add fluent addColumns operation to CsvSchema.Builder
* [#137](../../jackson-dataformat-csv/issues/137): Inject "missing" trailing columns as `null`s (`CsvParser.Feature.INSERT_NULLS_FOR_MISSING_COLUMNS`)
* [#139](../../jackson-dataformat-csv/issues/139): Add `CsvParser.Feature.ALLOW_TRAILING_COMMA` to allow enforcing strict handling
* [#140](../../jackson-dataformat-csv/issues/140): Fail for missing column values (`CsvParser.Feature.FAIL_ON_MISSING_COLUMNS`)
* [#142](../../jackson-dataformat-csv/issues/142): Add methods for appending columns of a `CsvSchema` into another
* Add new exception type `CsvMappingException` to indicate CSV-mapping issues (and give access to effective Schema)

#### [Protobuf](../../jackson-dataformats-binary)

* [#64](../../jackson-dataformats-binary/issues/64): Implement native `float` handling for parser
* [#68](../../jackson-dataformats-binary/issues/68): Getting "type not supported as root type by protobuf" for serialization of short and UUID types
* [#79](../../jackson-dataformats-binary/issues/79): Fix wire type for packed arrays

#### [Properties](../../jackson-dataformat-properties)

* [#1](../../jackson-dataformat-properties/issues/1): Add convenience method(s) for reading System properties
* [#3](../../jackson-dataformat-properties/issues/3): Write into `Properties` instance (factory, mapper) using `JavaPropsMapper.writeValue()` with `Properties` and `JavaPropsMapper.writeValueAsProperties()`
* [#4](../../jackson-dataformat-properties/issues/4): Allow binding from `Properties` instance

#### [XML](../../jackson-dataformat-xml)

* [#162](../../jackson-dataformat-xml/issues/162): XML Empty tag to Empty string in the object during xml deserialization
* [#232](../../jackson-dataformat-xml/issues/232): Implement `writeRawValue` in `ToXmlGenerator`
* [#245](../../jackson-dataformat-xml/issues/245): Default `DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT` to "enabled" for `XmlMapper`
* [#246](../../jackson-dataformat-xml/issues/246): Add new feature, `FromXmlParser.Feature.EMPTY_ELEMENT_AS_NULL`
* [#250](../../jackson-dataformat-xml/issues/250): Deserialization of `Exception` serialized as XML fails

#### [YAML](../../jackson-dataformat-yaml)

* [#67](../../jackson-dataformat-yaml/issues/67): Add `YAMLGenerator.Feature.INDENT_ARRAYS`
* [#76](../../jackson-dataformat-yaml/issues/76): Add `YAMLGenerator.Feature.LITERAL_BLOCK_STYLE` for String output

### Changes, datatypes

#### [Java 8](../../jackson-modules-java8)

* [#3](../../jackson-modules-java8/issues/3): (datatype) Add Serialization Support for Streams
* [#20](../../jackson-modules-java8/issues/20): (datetime) Allow `LocalDate` to be serialized/deserialized as number (epoch day)
* [#21](../../jackson-modules-java8/issues/21): (datetime) `DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS` not respected

### Changes, other modules

#### [JSON Schema](../../jackson-module-jsonSchema)

* [#119](../../jackson-module-jsonSchema/issues/119): `dependencies` property should not be an Array but Object
