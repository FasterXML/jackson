Version 2.7 development started in August 2015; and the first official release happened on January 10, 2016.

## Status

Branch is closed and no new releases are planned, including micro-patches: the firm end-of-life for all kinds of releases is end of 2020.

## New Modules, status changes

* New datatype module: [jackson-datatype-pcollections](../../jackson-datatype-pcollections)
* Kotlin module become official with 2.7.2, as that is the first version based on the official Kotlin 1.0
* [jackson-module-jdk7](../../jackson-module-jdk7) was deprecated: all functionality was dynamically added directly in `jackson-databind`, as explained below.
    * no 2.7.x version was released (in retrospect, a no-op version might have made more sense)

## Patches

Beyond initial 2.7.0 (described here), following patch releases have been made:

* [2.7.1](Jackson-Release-2.7.1) (02-Feb-2016)
* [2.7.2](Jackson-Release-2.7.2) (27-Feb-2016)
* [2.7.3](Jackson-Release-2.7.3) (16-Mar-2016)
* [2.7.4](Jackson-Release-2.7.4) (29-Apr-2016)
* [2.7.5](Jackson-Release-2.7.5) (11-Jun-2016)
* [2.7.6](Jackson-Release-2.7.6) (23-Jul-2016)
* [2.7.7](Jackson-Release-2.7.7) (27-Aug-2016)
* [2.7.8](Jackson-Release-2.7.8) (26-Sep-2016)
* [2.7.9](Jackson-Release-2.7.9) (04-Feb-2017)

At this point branch is not open any more (that is, no more full patch releases are planned).

Following micro-patches have been released:

* `jackson-databind`
    * `2.7.9.1` (18-Apr-2017)
        * [#1599](../../jackson-databind/issues/1599): Jackson Deserializer security vulnerability
        * Minor robustification of method resolution in `AnnotatedClass` 
    * `2.7.9.2` (20-Dec-2017)
        * [#1607](../../jackson-databind/issues/1607): `@JsonIdentityReference` not used when setup on class only
        * [#1628](../../jackson-databind/issues/1628): Don't print to error stream about failure to load JDK 7 types
        * [#1680](../../jackson-databind/issues/1680): Blacklist couple more types for deserialization
        * [#1737](../../jackson-databind/issues/1737): Block more JDK types from polymorphic deserialization
        * [#1855](../../jackson-databind/issues/1855): Blacklist for more serialization gadgets (dbcp/tomcat, spring)
    * `2.7.9.3` (11-Feb-2018)
        * [#1872](../../jackson-databind/issues/1872): `NullPointerException` in `SubTypeValidator.validateSubType` when validating Spring interface
        * [#1931](../../jackson-databind/issues/1931): Two more `c3p0` gadgets to exploit default typing issue
    * `2.7.9.4` (08-Jun-2018)
         * [#2032](../../jackson-databind/issues/2032): CVE-2018-11307: Potential information exfiltration with default typing, serialization gadget from MyBatis
         * [#2052](../../jackson-databind/issues/2052): CVE-2018-12022: Block polymorphic deserialization of types from Jodd-db library
         * [#2058](../../jackson-databind/issues/2058): CVE-2018-12023: Block polymorphic deserialization of types from Oracle JDBC driver
    * `2.7.9.5` (23-Nov-2018)
         * [#1899](../../jackson-databind/issues/1899): Another two gadgets to exploit default typing issue in jackson-databind
         * [#2097](../../jackson-databind/issues/2097): Block more classes from polymorphic deserialization (CVE-2018-14718 - CVE-2018-14721)
         * [#2186](../../jackson-databind/issues/2186): Block more classes from polymorphic deserialization (CVE-2018-19360, CVE-2018-19361, CVE-2018-19362)
    * `2.7.9.6` (26-Jul-2019)
        * [#2326](../../jackson-databind/issues/2326): Block class for CVE-2019-12086
        * [#2334](../../jackson-databind/issues/2334): Block class for CVE-2019-12384
        * [#2341](../../jackson-databind/issues/2341): Block class for CVE-2019-12814
        * [#2387](../../jackson-databind/issues/2387): Block class for CVE-2019-14379
        * [#2389](../../jackson-databind/issues/2389): Block class for (CVE-2019-14439
    * `2.7.9.7` (10-Mar-2020)
        * [#2526](../../jackson-databind/issues/2526): Block two more gadget types (ehcache/JNDI - CVE-2019-20330)
        * [#2620](../../jackson-databind/issues/2620): Block one more gadget type (xbean-reflect/JNDI - CVE-2020-8840)
        * [#2631](../../jackson-databind/issues/2631): Block one more gadget type (shaded-hikari-config, CVE-2020-9546)
        * [#2634](../../jackson-databind/issues/2634): Block two more gadget types (ibatis-sqlmap, anteros-core; CVE-2020-9547 / CVE-2020-9548)
        * [#2642](../../jackson-databind/issues/2642): Block one more gadget type (javax.swing, CVE-to-be-allocated)
        * [#2648](../../jackson-databind/issues/2648): Block one more gadget type (shiro-core, CVE-to-be-allocated)
* `jackson-module-kotlin` `2.7.9.1` (10-Feb-2018)
    * Upgrade to work Kotlin 1.2(.21)

## Changes: compatibility

Starting with 2.7, JDK baseline will be Java 7 / JDK 7, with following exceptions:

* `jackson-annotations` and `jackson-core` (streaming) will remain Java 6
* No new language features are yet used (diamond pattern, try-with-resources); only JDK types
* All JDK 7 types are accessed dynamically
    * Deserializer for `java.nio.file.Path`
    * Handling of new annotations under `java.beans`: `@ConstructorProperties`, `@Transient` via `JacksonAnnotationIntrospector`, but

So it should still be possible to use Jackson 2.7 on Java 6, but not compile, build.
With Jackson 2.8, Java 7 languages features will be allowed

Other compatibility changes:

* `ObjectMapper` default timezone now `UTC`, not `GMT` (as with 2.6 and prior): usually the two behave the same, but may cause minor variation in serialized form
* As per `#952` (see below), behavior of `JsonInclude.Include.NON_EMPTY` will be reverted to 2.5 level: it only applies to `Collection`s, `Map`s, arrays and `String`s: default scalar values (like `0` for `int`) will NOT be considered "empty".
    * Behavior of `NON_EMPTY` was different only for 2.6: prior to that behavior was the same as with 2.7 and onwards

## Implemented major features

### Add support for dataformat-specific features

With Jackson 2.6, `ObjectReader` allows passing of `JsonParser.Feature` overrides, and `ObjectWriter` similarly `JsonGenerator.Feature` overrides. But there are some format-specific features for formats like `CSV` which were not being passed before 2.7.

Since `ObjectReader` and `ObjectWriter` are not easy to sub-class (an attempt to do so pointed out a few inconvenient quirks Java typing would impose), it would be most convenient to be able to pass opaque bitflags to actual parser/generator implementation, and this is what has been added.

### Fix (generic) type resolution mechanism

While Jackson has the best support for generic types of all Java JSON libraries, there are certain tricky edge cases that do not work. Specifically, type variable aliasing (case where variable name like `T` binds to different types at different points in hierarchy) is not correctly resolved, as variable binding is constructed globally and not hierarchically.

`java-classmate` library which was built based on my experiences with Jackson does handle all these cases correctly (to my knowledge), and could be used as a blueprint for improved system. It can not necessarily be used directly because Jackson's type system is more complicated and adds semantics that classmate does not use (like `Map` and `Collection` types being special), but should help as an example.

### Revert back expansion of `NON_EMPTY` handling

Although the intent has always been that `Include.NON_EMPTY` would apply not just to empty arrays, `Collection`s , `Map`s and `String`s, to include things like numbers with default values (`0` for `int`, for example). But since many `JsonSerializer`s did NOT properly check for these conditions, before Jackson 2.6 `NON_EMPTY` exclusion did not work as extensively as intended, and many users came to depend on this specific behavior.

With 2.6 serializers were improved to handle "emptiness" as originally envisioned. This confused some 
of users, leading to confusion and frustration on what seemed like arbitrary changes.

After lengthy discussions over this issue, it seems that instead of using extended definition of what is empty, it makes more sense to use another value, `NON_DEFAULT` for such concept, and keep `NON_EMPTY` to strictly ONLY exclude:

* Anything that is `null` (that is, a superset of `NON_NULL`)
* Anything that is "absent" (for `Optional`s and other "Reference Types" -- superset of `NON_ABSENT`)
* Empty container types with no elements:
    * `java.util.Collection`
    * `java.util.Map`
    * Java arrays
* Empty Strings

With that, `NON_DEFAULT` will have two modes:

* When applied on containing class, it will try to compare actual defaults values of the POJO for given properties
* Otherwise (when `NON_DEFAULT` is either default global or per-type value; or used as per-property override), use per-type criteria, where:
    * anything that would be considered "empty" is excluded, but also
    * default values for primitives (like `0` for `int`) and their wrappers (`Integer.valueOf(0)` for `Integer`) are also excluded


-----

### Changes, core

#### [Annotations](../../jackson-annotations)

* [#37](../../jackson-annotations/issues/37): Add `@JsonClassDescription`
* [#77](../../jackson-annotations/issues/77): Add a new `ObjectIdGenerator`, `StringIdGenerator`, to allow arbitrary `String` Object Id usage
* Major rewrite of merging of `JsonFormat.Value` and `JsonInclude.Value`, to allow for better multi-level defaults (global, per-type, property)

#### [Streaming](../../jackson-core)

* [#37](../../jackson-core/issues/37): `JsonParser.getTokenLocation()` doesn't update after field names
* [#198](../../jackson-core/issues/198): Add back-references to `JsonParser` / `JsonGenerator` for low-level parsing issues (via `JsonParseException`, `JsonGenerationException`)
* [#211](../../jackson-core/issues/211): Fix typo of function name `com.fasterxml.jackson.core.Version.isUknownVersion()` (add `isUnknownVersion()`, deprecated misspelled variant)
* [#229](../../jackson-core/issues/229): Array element and field token spans include previous comma.

#### [Databind](../../jackson-databind)

* [76](../../jackson-databind/issues/76): Problem handling datatypes Recursive type parameters
* [432](../../jackson-databind/issues/432): `StdValueInstantiator` unwraps exceptions, losing context
* [497](../../jackson-databind/issues/497): Add new JsonInclude.Include feature to exclude maps after exclusion removes all elements
* [803](../../jackson-databind/issues/803): Allow use of `StdDateFormat.setLenient()`
* [819](../../jackson-databind/issues/819): Add support for setting `FormatFeature` via `ObjectReader`, `ObjectWriter`
* [857](../../jackson-databind/issues/857): Add support for `java.beans.Transient`
* [905](../../jackson-databind/issues/905): Add support for `@ConstructorProperties`
* [909](../../jackson-databind/issues/909): Rename PropertyNamingStrategy `CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES` as `SNAKE_CASE`, `PASCAL_CASE_TO_CAMEL_CASE` as `UPPER_CAMEL_CASE`
* [915](../../jackson-databind/issues/915): ObjectMapper default timezone is GMT, should be UTC
* [918](../../jackson-databind/issues/918): Add `MapperFeature.ALLOW_EXPLICIT_PROPERTY_RENAMING`
* [924](../../jackson-databind/issues/924): `SequenceWriter.writeAll()` could accept `Iterable`
* [948](../../jackson-databind/issues/948): Support leap seconds, any number of millisecond digits for ISO-8601 Dates.
* [952](../../jackson-databind/issues/952): Revert non-empty handling of primitive numbers wrt `NON_EMPTY`; make `NON_DEFAULT` use extended criteria
* [957](../../jackson-databind/issues/957): Merge `datatype-jdk7` stuff in (java.nio.file.Path handling)
* [959](../../jackson-databind/issues/959): Schema generation: consider active view, discard non-included properties
* [963](../../jackson-databind/issues/963): Add PropertyNameStrategy `KEBAB_CASE`
* [978](../../jackson-databind/issues/978): ObjectMapper#canSerialize(Object.class) returns false even though FAIL_ON_EMPTY_BEANS is disabled
* [997](../../jackson-databind/issues/997): Add `MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS`
* [998](../../jackson-databind/issues/998): Allow use of `NON_DEFAULT` for POJOs without default constructor
* [1000](../../jackson-databind/issues/1000): Add new mapping exception type for enums and UUIDs
* [1010](../../jackson-databind/issues/1010): Support for array delegator
* [1043](../../jackson-databind/issues/1043): `@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)` does not work on fields
* [1044](../../jackson-databind/issues/1044): Add `AnnotationIntrospector.resolveSetterConflict(...)` to allow custom setter conflict resolution

### Changes, Data formats

#### [Avro](../../jackson-dataformat-avro)

* [#21](../../jackson-dataformat-avro/issues/21): Add `AVRO_BUFFERING` feature for `AvroParser`, `AvroGenerator` (enabled by default, same as existing pre-2.7 behavior)

#### [CSV](../../jackson-dataformat-csv)

* [#81](../../jackson-dataformat-csv/issues/81): Add alternative way to configure 'wrap-as-array', as `CsvParser` feature
* [#89](../../jackson-dataformat-csv/issues/89): Allow choice of using header-line declaration to reorder columns of explicit schema, with `CsvSchema.setReorderColumns`
* [#92](../../jackson-dataformat-csv/issues/92): Allow multi-character separator values
* [#94](../../jackson-dataformat-csv/issues/94): Change schema/mapping related `JsonParseException`s to proper `JsonMappingException`s
* [#95](../../jackson-dataformat-csv/issues/95): Add `CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE` to allow skipping of all extra, unmappable columns
* [#97](../../jackson-dataformat-csv/issues/97): Verify CSV headers are in the order as expected (added `strictHeaders` property in `CsvSchema`)
* [#103](../../jackson-dataformat-csv/issues/103): `JsonGenerator.Feature.IGNORE_UNKNOWN` does not prevent error when writing structured values
* [#106](../../jackson-dataformat-csv/issues/106): Null fields are always ignored when serializing list of 
* [#109](../../jackson-dataformat-csv/issues/109): Allow specifying (via `CsvSchema`) a way to map "extra" columns into specific key (to use via any setter)

#### [Protobuf](../../jackson-dataformat-protobuf)

* [#11](../../jackson-dataformat-protobuf/issues/11): dd Support for Generating Protobuf Schema From POJO Definition

#### [XML](../../jackson-dataformat-xml)

* [#156](../../jackson-dataformat-xml/issues/156): Add `XmlMapper.setDefaultUseWrapper()` for convenience.
* [#167](../../jackson-dataformat-xml/issues/167): Exception on deserializing empty element with an xsi attribute
* [#169](../../jackson-dataformat-xml/issues/169): Fail to deserialize "empty" polymorphic classes
* [#180](../../jackson-dataformat-xml/issues/180): Problem with deserialization of nested non-wrapped lists, with empty inner list

#### [YAML](../../jackson-dataformat-yaml)

* [38](../../jackson-dataformat-yaml/issues/38): Add MINIMIZE_QUOTES generator feature
* [50](../../jackson-dataformat-yaml/issues/50): Lack of SnakeYAML Resolver leaves some missing features

### Changes, Datatypes

#### [Guava](../../jackson-datatype-guava)

* [#79](../../jackson-datatype-guava/issues/79): New configuration for Guava Range default bound type.

#### [Java8 Date/time](../../jackson-datatype-jsr310)

* [#54](../../jackson-datatype-jsr310/issues/54): `LocalDateTimeSerializer` default constructor should use the same formatter as `LocalDateTimeDeserializer`

### Changes, JAX-RS

### Changes, other modules

### [JAXB Annotations](../../jackson-module-jaxb-annotations)

* [#52](../../jackson-module-jaxb-annotations/issues/52): Add a feature in `JaxbAnnotationIntrospector` to define meaning of `nillable=false` as "JsonInclude.NON_EMPTY"

#### [JSON Schema](../../jackson-module-jsonSchema)

* [#80](../../jackson-module-jsonSchema/issues/80): Support `NumberFormat#multipleOf`
* [#81](../../jackson-module-jsonSchema/issues/81): Deserialize `ArrayItems` and `JsonValueFormat` correctly

### Changes, [Jackson-jr](../../jackson-jr)

* [#28](../../jackson-jr/issues/28): Remove misspelled `JSON.Feature.USE_IS_SETTERS`
* [#29](../../jackson-jr/issues/29): Add `JSON.Feature.WRITE_DATES_AS_TIMESTAMP`, enabling of which allows serialization of `java.util.Date` as long
* [#30](../../jackson-jr/issues/30): Add initial version of jackson-jr - based Retrofit2 Converter
* [#31](../../jackson-jr/issues/31): Fix failure writing UUID, URL and URI
* [#34](../../jackson-jr/issues/34): Add basic read-only (immutable) tree model impementation (stree)

