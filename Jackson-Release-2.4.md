Version 2.4.0 was released in June 2014.
It is a "minor" release following 2.3, meaning that it adds new functionality but be backwards compatible with earlier 2.x releases.

## Status

Branch is closed for all new releases (including micro-patches)

### Patches

Beyond initial 2.4.0 (described here), following patch releases were made:

* [2.4.1](Jackson-Release-2.4.1) (16-Jun-2014)
* [2.4.2](Jackson-Release-2.4.2) (15-Aug-2014)
* [2.4.3](Jackson-Release-2.4.3) (04-Oct-2014)
* [2.4.4](Jackson-Release-2.4.4) (24-Nov-2014)
* [2.4.5](Jackson-Release-2.4.5) (13-Jan-2015)
* [2.4.6](Jackson-Release-2.4.6) (23-Apr-2015)

### Changes: compatibility

Support for oldest Android versions (2.x) will not be continued, as Jackson modules may use full feature set of JDK 1.6.

[Scala module](../../jackson-module-scala) will only support Scala 2.10 and above (and will support 2.11 for the first time)

### Changes: functional, high-level

The biggest improvement with 2.4 is the ability to finally resolve so-called Object Id Forward References between objects: that is, declaration of an Object Id need not come before reference in document order (or be directly reference from within same object).

Resolution of property name conflicts has been improved: "cleaved" properties are not considered to be conflicts, as long there are no ambiguities in renaming (with 2.3 and earlier some cases with alternate names are considered errors even if intent can be decuded and there is no actual ambiguity).
Part of the fix includes distinguishing between implicit and explicit names for parameters as well, in cases where implicit names can be introspector (using Paranameter, or JDK8 feature).

### Changes, core

#### [Core Annotations](../../jackson-annotations)

* [#31](../../jackson-annotations/issues/31): Allow use of `@JsonPropertyOrder` for properties (not just classes)
* [#32](../../jackson-annotations/issues/32): Add `@JsonProperty.index` for indicating optional numeric index of the property.
* Add `JsonFormat.Value#timeZoneAsString` (needed by Joda module)
* Add `@JsonRootName.namespace` to allow specifying of namespace with standard Jackson annotations (not just XML-specific ones that dataformat-xml provides)

#### [Core Streaming](../../jackson-core)

* [#121](https://github.com/FasterXML/jackson-core/issues/121): Increase size of low-level byte[]/char[] input/output buffers (from 4k->8k for bytes, 2k->4k for chars)
* [#127](https://github.com/FasterXML/jackson-core/issues/127): Add `JsonGenerator.writeStartArray(int size)` for binary formats
* [#138](https://github.com/FasterXML/jackson-core/issues/138): Add support for using `char[]` as input source; optimize handling of `String` input as well.

#### [Core Databind](../../jackson-databind)

* [#88](../../jackson-databind/issues/88): Prevent use of type information for `JsonNode` via default typing
* [#149](../../jackson-databind/issues/149): Allow use of "stringified" indexes for Enum values
* [#176](../../jackson-databind/issues/176): Allow use external Object Id resolver (to use with @JsonIdentityInfo etc)
* [#335](../../jackson-databind/issues/335): Allow use of `@JsonPropertyOrder(alphabetic=true)` for Map properties
* [#351](../../jackson-databind/issues/351): ObjectId does not properly handle forward references during deserialization
* [#353](../../jackson-databind/issues/353): Problems with polymorphic types, `JsonNode` (related to #88)
* [#359](../../jackson-databind/issues/359): Converted object not using explicitly annotated serializer
* [#369](../../jackson-databind/issues/369): Incorrect comparison for renaming in `POJOPropertyBuilder`
* [#375](../../jackson-databind/issues/375): Add `readValue()`/`readPropertyValue()` methods in `DeserializationContext`
* [#381](../../jackson-databind/issues/381): Allow inlining/unwrapping of value from single-component JSON array
* [#390](../../jackson-databind/issues/390): Change order in which managed/back references are resolved (now back-ref first, then forward)
* [#407](../../jackson-databind/issues/407): Properly use null handlers for value types when serializer Collection and array types
* [#428](../../jackson-databind/issues/428): `PropertyNamingStrategy` will rename even explicit name from `@JsonProperty`
* [#434](../../jackson-databind/issues/434): Ensure that DecimalNodes with mathematically equal values are equal

### Changes, Data Formats

#### [CSV](../../jackson-dataformat-csv)

* [#26](../../jackson-dataformat-csv/issues/26): Inconsistent quoting of headers, values; add
  `CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING` to allow more optimal checks.
* [#32](../../jackson-dataformat-csv/issues/32): Allow disabling of quoteChar
 (suggested by Jason D)
* [#40](../../jackson-dataformat-csv/issues/40): Allow (re)ordering of columns of Schema, using `CsvSchema.sortedBy(...)`
* [#45](../../jackson-dataformat-csv/issues/45): Change outputting behavior to include final commas even if values are missing; also add `CsvGenerator.OMIT_MISSING_TAIL_COLUMNS`

#### [XML](../../jackson-dataformat-xml)

* [#76](https://github.com/FasterXML/jackson-dataformat-xml/issues/76): UnrecognizedPropertyException when containing object is a Collection
* [#83](https://github.com/FasterXML/jackson-dataformat-xml/issues/83): Add support for `@JsonUnwrapped`
* [#99](https://github.com/FasterXML/jackson-dataformat-xml/issues/99): Problem with `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES`, Lists
* [#108](https://github.com/FasterXML/jackson-dataformat-xml/issues/108): Unwrapped list ignore attributes if 2 lists in sequence
* [#111](https://github.com/FasterXML/jackson-dataformat-xml/issues/111): Make vanilla `JaxbAnnotationIntrospector` work, without having to use `XmlJaxbAnnotationIntrospector`

### Changes, Data Types

#### [Guava](../../jackson-datatype-guava)

* [#43](../../jackson-datatype-guava/43): Add support for `HostAndPort`

#### [Hibernate](../../jackson-datatype-hibernate)

* [#53](../../jackson-datatype-hibernate/53): When JPA annotations are not used, collections are always loaded (added `Hibernate4Module.Feature.REQUIRE_EXPLICIT_LAZY_LOADING_MARKER` with default value of `false`)

#### [JDK7 data types](../../jackson-datatype-jdk7)

The very first official release:

* Supports `java.nio.file.Path`

### Changes, JAX-RS

* [#49](https://github.com/FasterXML/jackson-jaxrs-providers/issues/49): Add `JaxRSFeature.ALLOW_EMPTY_INPUT`, disabling of which can prevent mapping of empty input into Java null value

### Changes, [Jackson jr](https://github.com/FasterXML/jackson-jr)

The very first official release!

### Changes, Other modules

#### [JAXB Annotations](../../jackson-module-jaxb-annotations)

#### [Parameter Names](../../jackson-module-parameter-names) (JDK8 only)

The very first official release!

* Support discovery of parameter names for JSON Creator methods.

#### [Scala](../../jackson-module-scala)