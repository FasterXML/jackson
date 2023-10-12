[Jackson Version](Jackson-Releases) 2.12 was released on November 28, 2020: two release candidates (`2.12.0-rc1`, `2.12.0-rc2`) were released prior to the final 2.12.0.

This wiki page gives a list of links to all changes (with brief descriptions) included.

Aside from detailed change notes below, there is a separate [2.12 Acknowledgements](Acknowledgements-2.12) page for special thanks.

## Status

Branch is nominally open but it is not likely that there will be full patch releases beyond (2.12.7).
Micro-patches are possible.

## Patches

* [2.12.1](Jackson-Release-2.12.1) (08-Jan-2021)
* [2.12.2](Jackson-Release-2.12.2) (03-Mar-2021)
* [2.12.3](Jackson-Release-2.12.3) (12-Apr-2021)
* [2.12.4](Jackson-Release-2.12.4) (06-Jul-2021)
* [2.12.5](Jackson-Release-2.12.5) (27-Aug-2021)
* [2.12.6](Jackson-Release-2.12.6) (15-Dec-2021)
* [2.12.7](Jackson-Release-2.12.7) (26-May-2022)

### Micro-patches

Following micro-patches have been released:

* `jackson-databind` `2.12.6.1` (26-Mar-2022) -- with `jackson-bom` version `2.12.6.20220326`
    * [#2816](../../jackson-databind/issues/2816): Optimize UntypedObjectDeserializer wrt recursion (CVE-2020-36518)
* `jackson-databind` `2.12.7.1` (12-Oct-2022) -- with `jackson-bom` version `2.12.7.20221012`
    * [#3582](../../jackson-databind/issues/3582): Add check in `BeanDeserializer._deserializeFromArray()` to prevent use of deeply nested arrays [CVE-2022-42004]
    * [#3590](../../jackson-databind/issues/3590): Add check in primitive value deserializers to avoid deep wrapper array nesting wrt `UNWRAP_SINGLE_VALUE_ARRAYS` [CVE-2022-42003]

-----

## Documentation

* [Jackson 2.12 features](https://cowtowncoder.medium.com/jackson-2-12-features-eee9456fec75) (Overview)
    * [Jackson 2.12: Deduction-based Polymorphism](https://cowtowncoder.medium.com/jackson-2-12-most-wanted-1-5-deduction-based-polymorphism-c7fb51db7818)
    * [Jackson 2.12: @JsonIncludeProperties](https://cowtowncoder.medium.com/jackson-2-12-most-wanted-2-5-bc45ef53ede7)
    * [Jackson 2.12: `ConstructorDetector`](https://cowtowncoder.medium.com/jackson-2-12-most-wanted-3-5-246624e2d3d0)
    * [Jackson 2.12: `CoercionConfig` system](https://cowtowncoder.medium.com/jackson-2-12-most-wanted-4-5-cbc91c00bcd2)
    * [Jackson 2.12: Support `java.lang.Record`](https://cowtowncoder.medium.com/jackson-2-12-most-wanted-5-5-a32c28c345b5)
* [Jackson 2.12: Improved XML handling](https://cowtowncoder.medium.com/jackson-2-12-improved-xml-b9487889a23f)

## Changes, compatibility

### Compatibility: JDK requirements

JDK baseline for use since [2.11](Jackson-Release-2.11) is retained with following exceptions:

1. `Ion` dataformat module (part of `jackson-dataformats-binary`) now requires Java 8 due to new optional `IonJavaTimeModule`
2. `Eclise-collections` datatype module (part of `jackson-datatypes-collections`) requires Java 8 (was the case before but not documented)
3. `Guava` datatype module (part of `jackson-datatypes-collections`) requires Java 8 due to Guava 21 dependency upgrades baseline

JDK 8 is required to build all components, however, as module info inclusion plug-in requires it (note: publishing to Maven Central also requires JDK 8), but the minimum runtime version is JDK/JVM 7, with following exceptions:

* `jackson-annotations`, `jackson-core`, `jackson-jr` only require JDK/JVM 6
* JDK/JVM 8 higher needed for:
    * Kotlin and Scala language modules
    * Java 8 modules (datatypes, parameter-names, jsr310 date/time)
    * Ion dataformat module
    * Eclipse-collections datatype module

### Compatibility: other

#### Android 4.4 (API Level 19)

Due to use of `java.util.Objects`, part of JDK 7, minimum Android version supported appears to be 4.4 / API Level 19:

    https://developer.android.com/reference/kotlin/java/util/Objects

#### Kotlin (`jackson-module-kotlin`)

Jackson Kotlin module is now compiled against (and is designed to work with) Kotlin 1.4.

#### Scala support (`jackson-module-scala_VERSION`)

Support for Scala 2.10 is dropped (so that Jackson 2.11 is the last version of with Scala 2.10 support): Jackson 2.12 will support following Scala versions:

* 2.11
* 2.12
* 2.13

## Changes, behavior

### Databind

#### 1-field Record types

Addition of explicit support for `java.lang.Record` changes handling slightly for one specific case: if you have a `Record` type with 1 property, like:

```java
public record MyValueRecord(String value) {}
```

it would be assumed to use "Delegating" style of parameter passing, and would (only) accept JSON String to bind. With 2.12 all `Records` default to "Properties" style binding so a single-property JSON Object is expected instead. Note that it is possible to annotate constructor explicitly:

```java
public record MyValueRecord(String value) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public MyValueRecord(String value) {
        this.value = value;
    }
}
```

to produce pre-2.12 behavior, as necessary.
See [jackson-databind#2980](https://github.com/FasterXML/jackson-databind/issues/2980) for details.

### XML module

Default for `FromXmlParser.Feature.EMPTY_ELEMENT_AS_NULL` changed from `true` (2.9 - 2.11) to `false`, so that no automatic coercion done from empty elements like `<empty/>` into `null`

* see [jackson-dataformat-xml#411](https://github.com/FasterXML/jackson-dataformat-xml/issues/411)

It also looks like handling of "empty values" (zero-length String) for scalar values like numbers changed, as per [jackson-dataformat-xml#473](https://github.com/FasterXML/jackson-dataformat-xml/issues/473), but this issue should be resolved in 2.12.4.

### JSR-353

* Input `null` value will be deserialized as `JsonValue.NULL`
    * see [datatypes-misc#2](https://github.com/FasterXML/jackson-datatypes-misc/issues/2)

## New Modules, status changes

* New Base module -- [jackson-datatype-blackbird](https://github.com/FasterXML/jackson-modules-base/tree/master/blackbird)
    * (Future) Replacement for Afterburner module: solves same use case, speeding up of POJO databind
    * Works better than Afterbuern with newer JVMs (Java 9 and later)
    * Contributed by Steven Schlansker (@stevenschlansker)
* Alternate jars with `jakarta` classifier for modules that rely on JAX-xxx APIs, to support new "Jakarta" namespaced apis:
    * JAX-RS modules (`jackson-jaxrs-XXX-provider`) and JAXB annotations module (`jackson-module-jaxb-annotations`) have both "regular" jar  and variant with classifier of `jakarta`
    * Existing ("old") jars rely on 2.x version of JAX-WS, JAX-RS APIs, in existing `javax.` namespace
    * New "jakarta" variants (with classifier of `jakarta`) will refer to repackaged "Jakarta" variants of APIs
    * Users will need to specify classifier, for now, if they want/need to use newer dependencies 

## Major features of 2.12

### `CoercionConfig` system

[#2113](../../jackson-databind/issues/2113) adds `CoercionConfig` system which allows indicating which of 4 `CoercionAction`s to take for given input shape, target type:

* `Fail`: not allowed, throw exception
* `AsNull`: allow, coerce to `null` (although may be further mapped via other mechanisms)
* `AsEmpty`: allow, coerce to "empty" value of type (empty `Collection`, POJO with no properties set)
* `TryConvert`: allow if there is logical conversion (for example String "123" can be parsed, converted to `int` value `123`)

Input shapes are defined with `CoercionInputShape` enum which roughly corresponds to `JsonToken` values, but also has 3 logical types for "empty" String, Array and Object as special cases.

Target type is specific both by concrete (specific type), `Class` and new `LogicalType` that has a smaller set of values.

Rules can be targeted at 3 levels:

1. For specific concrete type (`Class`), input shape
2. For logical type (like `LogicalType.Boolean`) -- covers `boolean`, `Boolean`, elements in `boolean[]`, `AtomicBoolean` -- and input shape
3. Default action for coercions from input shape, used if no per-type (concrete or logical) specified -- most commonly used for input shape of `EmptyString`

This feature allows defining coercion rules like:

* Let empty String value become POJO similar to being deserialized from `{ }` JSON Input (especially useful for XML)
* Let empty String value become `null` for specified type(s)
* Prevent coercion from JSON Numbers into Java `boolean`s (by default non-zero JSON Integers map to Boolean values as `true`)

### `@JsonIncludeProperties`

This is issue [databind#1296](https://github.com/FasterXML/jackson-databind/issues/1296): explained in bit more detail on [Jackson 2.12: @JsonIncludeProperties](https://cowtowncoder.medium.com/jackson-2-12-most-wanted-2-5-bc45ef53ede7) blog post

### XML handling improvements

* Support for "Arrays" with Tree Model, `JsonNode`, "untyped"/`List`/`Map` (nominal `java.lang.Object`)
    * [dataformat-xml#205](https://github.com/FasterXML/jackson-dataformat-xml/issues/205):  `XmlMapper`/`UntypedObjectDeserializer` swallows duplicated elements in
    * [dataformat-xml#403](https://github.com/FasterXML/jackson-dataformat-xml/issues/403): Make `JsonNode` implicitly create `ArrayNode`s for repeated XML Elements (aka "Make JsonNode work with XML")
* Basic support for "Mixed Content" (element(s) AND text for given XML element)
    * [dataformat-xml#405](https://github.com/FasterXML/jackson-dataformat-xml/issues/405): Mixed content not exposed through `FromXmlParser`, lost by `JsonNode`
    * As with array support, usable via `JsonNode` and "untyped" (`Map`/`List`/`Object`)
    * Does not retain full ordering so further work needed for 100% high fidelity mapping
* Fully support root values, including scalar types
    * [dataformat-xml#121](https://github.com/FasterXML/jackson-dataformat-xml/issues/121): `XmlMapper` not deserializing root-level Enums
    * [dataformat-xml#254](https://github.com/FasterXML/jackson-dataformat-xml/issues/254): No String-argument constructor/factory method to deserialize from String value when it is a Integer
    * [dataformat-xml#412](https://github.com/FasterXML/jackson-dataformat-xml/issues/412): Coercion from element-with-attribute-and-text only works for `String`, not other scalar types
* Support deserialization of scalars even for elements that contain additional attributes

### Java 14 Record type support

Pretty much as expected, see [https://github.com/FasterXML/jackson-databind/issues/2709](https://github.com/FasterXML/jackson-databind/issues/2709) for details:
reading and writing of `java.lang.Record` should work when running on Java 14 or later, using expected accessors,
constructors, and annotation overrides (if any).

### Polymorphic type by deduction (field existence)

The oldest open issue, [databind#43](../../jackson-databind/issues/43) is now implemented.
It basically allows omitting of actual Type Id field or value, as long as the subtype can be deduced
(`@JsonTypeInfo(use=DEDUCTION)`) from existence of fields. That is, every subtype has a distinct set of fields they included, and so during deserialization type can be uniquely and reliably detected.

See [Jackson 2.12: Deduction-based Polymorphism](https://cowtowncoder.medium.com/jackson-2-12-most-wanted-1-5-deduction-based-polymorphism-c7fb51db7818) for longer explanation.

### Annotation-less Constructor Auto-Detection

Issue [databind#1498](https://github.com/FasterXML/jackson-databind/issues/1498) addresses one remaining case where Creator (constructor) auto-detection was not possible: that of single-argument Constructors (due to ambiguity between Properties-based and Delegating choices).
2.12 allows configuration of `ContructorDetector` to resolve this aspect.

See [Jackson 2.12: `ConstructorDetector`](https://cowtowncoder.medium.com/jackson-2-12-most-wanted-3-5-246624e2d3d0) for longer explanation.

## Significant other features of 2.12

### Ion 

* [dataformats-binary#213](../../jackson-dataformats-binary/213): There is new optional `IonJavaTimeModule` that allows use of native Ion datatypes with Java 8 Date/Time types (JSR-310)

-----

## Planned features for 2.12 -- but postponed till 2.13+

### Planned features: Considered important

* `JsonNodeFeature`/`-Config` [JSTEP-3](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-3): configuring reading/writing of `JsonNode`
* `@PreSerialize` / `@PostDeserialize` method annotations (https://github.com/FasterXML/jackson-databind/issues/2045)
* Other format module fixes:
    * Protobuf: proto3, use of [Wire/schema](https://github.com/square/wire/tree/master/wire-library/wire-schema) for protoc parsing
    * CSV, structured?
    * Logical types: Avro, CBOR

-----

## Full Change list

### Changes, core

#### [Annotations](../../jackson-annotations)

* [#171](../../jackson-annotations/pull/171): `JsonSubType.Type` should accept array of names
* [#173](../../jackson-annotations/pull/173): Jackson version alignment with Gradle 6
* [#174](../../jackson-annotations/pull/174): Add `@JsonIncludeProperties`
* [#175](../../jackson-annotations/pull/175): Add `@JsonTypeInfo(use=DEDUCTION)`
* [#177](../../jackson-annotations/pull/177): Ability to use `@JsonAnyGetter` on fields
* [#179](../../jackson-annotations/pull/179): Add `@JsonKey` annotation
* [#180](../../jackson-annotations/pull/180): Allow repeated calls to `SimpleObjectIdResolver.bindItem()` for same mapping
* [#181](../../jackson-annotations/pull/181): Add `namespace` property for `@JsonProperty` (for XML module)

#### [Streaming](../../jackson-core)

* [#500](../../jackson-core/issues/500): Allow "optional-padding" for `Base64Variant`
* [#573](../../jackson-core/issues/573): More customizable TokenFilter inclusion (using `Tokenfilter.Inclusion`)
* [#618](../../jackson-core/issues/618): Publish Gradle Module Metadata
* [#619](../../jackson-core/issues/619): Add `StreamReadCapability` for further format-based/format-agnostic handling improvements
* [#627](../../jackson-core/issues/627): Add `JsonParser.isExpectedNumberIntToken()` convenience method
* [#630](../../jackson-core/issues/630): Add `StreamWriteCapability` for further format-based/format-agnostic
handling improvements
* [#631](../../jackson-core/issues/631): Add `JsonParser.getNumberValueExact()` to allow precision-retaining buffering
* [#639](../../jackson-core/issues/639): Limit initial allocated block size by `ByteArrayBuilder` to max block size

#### [Databind](../../jackson-databind)

* [#43](../../jackson-databind/issues/43): Add option to resolve type from multiple existing properties, `@JsonTypeInfo(use=DEDUCTION)`
* [#921](../../jackson-databind/issues/921): Deserialization Not Working Right with Generic Types and Builders
* [#1296](../../jackson-databind/issues/1296): Add `@JsonIncludeProperties(propertyNames)` (reverse of `@JsonIgnoreProperties`)
* [#1458](../../jackson-databind/issues/1458): `@JsonAnyGetter` should be allowed on a field
* [#1498](../../jackson-databind/issues/1498): Allow handling of single-arg constructor as property based by default
* [#1852](../../jackson-databind/issues/1852): Allow case insensitive deserialization of String value into `boolean`/`Boolean` (esp for Excel)
* [#1886](../../jackson-databind/issues/1886): Allow use of `@JsonFormat(with=JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)` on Class
* [#1919](../../jackson-databind/issues/1919): Abstract class included as part of known type ids for error message
* [#2091](../../jackson-databind/issues/2091): `ReferenceType` does not expose valid containedType
* [#2113](../../jackson-databind/issues/2113): : Add `CoercionConfig[s]` mechanism for configuring allowed coercions
* [#2118](../../jackson-databind/issues/2118): `JsonProperty.Access.READ_ONLY` does not work with "getter-as-setter" `Collection`s
* [#2215](../../jackson-databind/issues/2215): Support `BigInteger` and `BigDecimal` creators in `StdValueInstantiator`
* [#2283](../../jackson-databind/issues/2283): `JsonProperty.Access.READ_ONLY` fails with collections when a property name is specified
* [#2675](../../jackson-databind/issues/2675): Support use of `Void` valued properties (`MapperFeature.ALLOW_VOID_VALUED_PROPERTIES`)
* [#2683](../../jackson-databind/issues/2683): Explicitly fail (de)serialization of `java.time.*` types in absence of registered custom (de)serializers
* [#2707](../../jackson-databind/issues/2707): Improve description included in by `DeserializationContext.handleUnexpectedToken()`
* [#2709](../../jackson-databind/issues/2709): Support for JDK 14 record types
* [#2715](../../jackson-databind/issues/2715): `PropertyNamingStrategy` class initialization depends on its subclass, this can lead to class loading deadlock
* [#2719](../../jackson-databind/issues/2719): `FAIL_ON_IGNORED_PROPERTIES` does not throw on `READONLY` properties with an explicit name
* [#2726](../../jackson-databind/issues/2726): Jackson version alignment with Gradle 6
* [#2732](../../jackson-databind/issues/2732): Allow `JsonNode` auto-convert into `ArrayNode` if duplicates found (for XML)
* [#2733](../../jackson-databind/issues/2733): Allow values of "untyped" auto-convert into `List` if duplicates found (for XML)
* [#2775](../../jackson-databind/issues/2775): Disabling `FAIL_ON_INVALID_SUBTYPE` breaks polymorphic deserialization of Enums
* [#2804](../../jackson-databind/issues/2804): Throw `InvalidFormatException` instead of `MismatchedInputException` for ACCEPT_FLOAT_AS_INT coercion failures
* [#2871](../../jackson-databind/issues/2871): Add `@JsonKey` annotation (similar to `@JsonValue`) for customizable
serialization of Map keys
* [#2873](../../jackson-databind/issues/2873): `MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS` should work for enum as keys
* [#2879](../../jackson-databind/issues/2879): Add support for disabling special handling of "Creator properties" wrt alphabetic property ordering
* [#2885](../../jackson-databind/issues/2885): Add `JsonNode.canConvertToExactIntegral()` to indicate whether floating-point/BigDecimal values could be converted to integers losslessly
* [#2895](../../jackson-databind/issues/2895): Improve static factory method generic type resolution logic
* [#2909](../../jackson-databind/issues/2909): `@JsonValue` not considered when evaluating inclusion
* [#2910](../../jackson-databind/issues/2910): Make some java platform modules optional
* [#2925](../../jackson-databind/issues/2925): Add support for serializing `java.sql.Blob`
* [#2928](../../jackson-databind/issues/2928): `AnnotatedCreatorCollector` should avoid processing synthetic static (factory) methods
* [#2932](../../jackson-databind/issues/2932): Problem with implicit creator name detection for constructor detection

### Changes, data formats

#### [Avro](../../jackson-dataformats-binary/avro)

#### [CBOR](../../jackson-dataformats-binary/cbor)

* [#222](../../jackson-dataformats-binary/issues/222): Add `CBORGenerator.Feature.LENIENT_UTF_ENCODING` for lenient handling of Unicode surrogate pairs on writing
* [#228](../../jackson-dataformats-binary/issues/228): Add support for decoding unassigned "simple values" (type 7)

#### [CSV](../../jackson-dataformats-text/csv)

* [#199](../../jackson-dataformats-text/issues/199): Empty Lists can only be String-typed in CSV
* [#222](../../jackson-dataformats-text/issues/222): `JsonParser.Feature.EMPTY_STRING_AS_NULL` does not work when text is parsed as `String[]`

#### [Ion](../../jackson-dataformats-binary)

* [#212](../../jackson-dataformats-binary/212): Optimize `IonParser.getNumberType()` using `IonReader.getIntegerSize()`
* [#213](../../jackson-dataformats-binary/213): Add support to (de)serialize Ion timestamps to/from `java.time` classes

#### [XML](../../jackson-dataformat-xml)

* [#97](../../jackson-dataformat-xml/issues/97): Weird Exception during read with Type info
* [#121](../../jackson-dataformat-xml/issues/121): `XmlMapper` not deserializing root-level Enums
* [#124](../../jackson-dataformat-xml/issues/124): Deserialization of an empty list (with empty XML tag) results in `null`
* [#205](../../jackson-dataformat-xml/issues/205): `XmlMapper`/`UntypedObjectDeserializer` swallows duplicated elements in XML documents
* [#226](../../jackson-dataformat-xml/issues/226): XML to JSON - IOException when parsing XML with XMLMapper
* [#252](../../jackson-dataformat-xml/issues/252): Empty (or self-closing) Element representing `List` is incorrectly deserialized as null, not Empty List
* [#254](../../jackson-dataformat-xml/issues/254): No String-argument constructor/factory method to deserialize from
String value when it is a Integer
* [#257](../../jackson-dataformat-xml/issues/257): Deserialization fails of lists containing elements with `xml:space` attribute
* [#262](../../jackson-dataformat-xml/issues/262): Make `ToXmlGenerator` non-final
* [#273](../../jackson-dataformat-xml/issues/273): Input mismatch with case-insensitive properties
* [#307](../../jackson-dataformat-xml/issues/307): Missing collection item when they are not wrapped during unmarshal
with multiple namespaces
* [#314](../../jackson-dataformat-xml/issues/314): Jackson gets confused by parent list element
* [#318](../../jackson-dataformat-xml/issues/318): `XMLMapper` fails to deserialize null (POJO reference) from blank tag
* [#319](../../jackson-dataformat-xml/issues/319): Empty root tag into `List` deserialization bug
* [#360](../../jackson-dataformat-xml/issues/360): Add a feature to support writing `xsi:nil` attribute for `null` values
* [#374](../../jackson-dataformat-xml/issues/374): Deserialization fails with `XmlMapper` and `DeserializationFeature.UNWRAP_ROOT_VALUE`
* [#377](../../jackson-dataformat-xml/issues/377): `ToXmlGenerator` ignores `Base64Variant` while serializing `byte[]`
* [#380](../../jackson-dataformat-xml/issues/380): Unable to deserialize root-level `Instant` value from XML
* [#390](../../jackson-dataformat-xml/issues/390): Unexpected attribute at string fields causes extra objects to be created in parent list
* [#397](../../jackson-dataformat-xml/issues/397): `XmlReadContext` does not keep track of array index
* [#403](../../jackson-dataformat-xml/issues/403): Make `JsonNode` implicitly create `ArrayNode`s for repeated XML Elements
* [#405](../../jackson-dataformat-xml/issues/405): Mixed content not exposed through `FromXmlParser`, lost by `JsonNode`
* [#411](../../jackson-dataformat-xml/issues/411): Change default setting of `FromXmlParser.Feature.EMPTY_ELEMENT_AS_NULL` from `true` to `false`
* [#412](../../jackson-dataformat-xml/issues/412): Coercion from element-with-attribute-and-text only works for `String` not other scalar types
* [#422](../../jackson-dataformat-xml/issues/422): Elements containing <CDATA/> parsed incorrectly when at the end of another element
* [#434](../../jackson-dataformat-xml/issues/434): Add missing `ElementType.ANNOTATION_TYPE` for Jackson xml annotations to allow bundling 
* Add Gradle Module Metadata (https://blog.gradle.org/alignment-with-gradle-module-metadata)
* Upgrade Woodstox dependency to 6.2.3 (<- 6.2.1)

#### [YAML](../../jackson-dataformats-text/yaml)

* [#71](../../jackson-dataformats-text/issues/71): Hex number as an entry of an Object causing problem(s) with binding to POJO
* [#130](../../jackson-dataformats-text/issues/130): Empty String deserialized as `null` instead of empty string
* [#175](../../jackson-dataformats-text/issues/175): Add `YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR` to indent by 2 spaces
* [#226](../../jackson-dataformats-text/issues/226): Quote 'y'/'Y'/'n'/'N' as names too (to avoid problems with Boolean keys)
* [#229](../../jackson-dataformats-text/issues/229): Allow configuring the way "must quote" is determined for property names, String values
* [#231](../../jackson-dataformats-text/issues/231): Typed object with anchor throws Already had POJO for id (note: actual fix in `jackson-annotations`)
* [#232](../../jackson-dataformats-text/issues/232): Typed object throws "Missing type id" when annotated with '@JsonIdentityInfo'
* [#233](../../jackson-dataformats-text/issues/233): Support decoding Binary, Octal and Hex numbers as integers

### Changes, datatypes

#### [Collections](../../jackson-datatypes-collections)

* [#25](../../jackson-datatypes-collections/issues/25): (guava) SetMultimap should be deserialized to a LinkedHashMultimap by default
* [#79](../../jackson-datatypes-collections/issues/79): (guava)  Guava's RangeHelper causing NPE in PropertyNamingStrategy
* (guava) Update "preferred" Guava version to 21.0
* (guava) Require Java 8 due to Guava 21 baseline (JRE variant)

#### [Hibernate](../../jackson-datatype-hibernate)

* [#136](../../jackson-datatype-hibernate/issues/136): Add feature `WRAP_IDENTIFIER_IN_OBJECT` to allow disabling of wrapping
  of "id" attribute

#### [Java 8 date/time](../../jackson-modules-java8/datetime)

* [#94](../../jackson-modules-java8/issues/94): Deserialization of timestamps with UTC timezone to LocalDateTime doesn't yield correct time
* [#165](../../jackson-modules-java8/issues/165): Problem in serializing negative Duration values
* [#166](../../jackson-modules-java8/issues/166): Cannot deserialize `OffsetDateTime.MIN` or `OffsetDateTime.MAX` with `ADJUST_DATES_TO_CONTEXT_TIME_ZONE` enabled
* [#175](../../jackson-modules-java8/issues/175): ObjectMapper#setTimeZone ignored by jsr-310/datetime types during serialization when using `@JsonFormat` annotation
* [#184](../../jackson-modules-java8/issues/184): `DurationDeserializer` should use `@JsonFormat.pattern` (and config override) to support configurable `ChronoUnit`
* [#189](../../jackson-modules-java8/issues/189): Support use of "pattern" (`ChronoUnit`) for `DurationSerializer` too

#### [Joda](../../jackson-datatype-joda)

* [#116](../../jackson-datatype-joda/issues/116): Improve schema support for DateTimeZone type
* [#117](../../jackson-datatype-joda/issues/117): Timestamp deserialization not working for CSV, Properties or XML

#### [JSR-353 (JSON-P)](../../jackson-datatype-jsr353)

### Changes, Other modules

#### Afterburner

* [#117](../../jackson-modules-base/issues/117): Use of `ToStringSerializer` via `@JsonSerialize` on `int`/`long` property does not work
* [#118](../../jackson-modules-base/issues/118): Using `@JsonFormat(shape = JsonFormat.Shape.STRING)` on `int`, `long` properties not working

#### JAXB Annotations

* [#115](../../jackson-modules-base/issues/115): Remove ` java.beans.Introspector` dependency from JAXB module (to
   get rid of `java.desktop` module dep)
* [#116](../../jackson-modules-base/issues/116): Jakarta Namespace Support
    * See notes earlier on "Jakarta" variants of module jars: users will need to opt-in by using Maven classifier `jakarta` for version with "new" JAX-WS/JAXB dependencies

#### Mr Bean

* [#100](../../jackson-modules-base/issues/100): Prevent "double-prefixing" Mr Bean generated classes

### Changes, JVM Languages

#### [Kotlin](../../jackson-module-kotlin)

* [#322](../../jackson-module-kotlin/issues/322): Added extension methods to SimpleModule addSerializer and addDeserializer to support KClass arguments that register the serializer/deserializer for both the java type and java class.
* [#356](../../jackson-module-kotlin/issues/356): Kotlin 1.4 support
* [#385](../../jackson-module-kotlin/issues/385): Add Moditect, source module info, to allow Kotlin module usage with Java Module system
- Add Gradle Module Metadata (https://blog.gradle.org/alignment-with-gradle-module-metadata)


#### [Scala](../../jackson-module-scala)

* [#370](../../jackson-module-scala/issues/370): Support jackson feature @JsonMerge
* [#449](../../jackson-module-scala/issues/449): Remove `jackson-module-paranamer` dependency. Scala 2.11 variant uses Paranamer directly still but Scala 2.12 and 2.13 releases no longer use Paranamer.
* [#455](../../jackson-module-scala/issues/455): get `ScalaAnnotationIntrospector` to ignore non-Scala classes
* [#462](../../jackson-module-scala/issues/462): Unable to deserialize Seq or Map with AS_EMPTY null handling
* [#466](../../jackson-module-scala/issues/466): Add support for `WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED`
* [#467](../../jackson-module-scala/issues/467): Serializer for Scala Iterable/Iterator converts to Java Collection - avoid this conversion
* [#480](../../jackson-module-scala/issues/480): Drop Scala 2.10 support

### Changes, other

#### [JAX-RS](../../jackson-jaxrs-providers)

* [#127](../../jackson-jaxrs-provider/issues/127): Allow multiple implementations of JAX-RS for all providers
* [#128](../../jackson-jaxrs-provider/issues/128): Module-Info Enhancements - JAX-RS updates for Jakarta Release version

#### [jackson-jr](../../jackson-jr)

