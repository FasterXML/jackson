Version 2.5.0 was released January 1st, 2015.
It is a "minor" release following 2.4, meaning that it adds new functionality but be backwards compatible with earlier 2.x releases.

## Status

Branch is closed for all new releases (including micro-patches)

## Patches

Beyond initial 2.5.0 (described here), following patch releases were made:

* [2.5.1](Jackson-Release-2.5.1) (06-Feb-2015)
* [2.5.2](Jackson-Release-2.5.2) (29-Mar-2015)
* [2.5.3](Jackson-Release-2.5.3) (24-Apr-2015)
* [2.5.4](Jackson-Release-2.5.4) (09-Jun-2015)
* [2.5.5](Jackson-Release-2.5.5) (07-Dec-2015)

### Changes: compatibility

No changes to JDK requirements or baseline requirements/supports for external platforms (like Android or Scala versions).

One accidental change is with serialization of `Map.Entry` (see [#565](../../jackson-databind/issues/565) for details): before 2.5, serialization used default POJO approach, resulting in something like:

```
{ "key" : <key string>, "value" : <value object> }
```

but with #565 output was simplified to be:

```
{ <key string> : <value object> }
```

to support more natural serialization of things like `List<Map.Entry>`.
It is worth noting that before 2.5 only serialization would work; deserialization was not supported.
With 2.5, both serialization and deserialization work.

### Changes, core

#### [Core Annotations](../../jackson-annotations)

* [#47](../../jackson-annotations/issues/47): Add `@JsonCreator.mode` property to explicitly choose between delegating- and property-based creators
* [#48](../../jackson-annotations/issues/48): Allow `@JsonView` for (method) parameters too (not used by Jackson itself, but requested by frameworks)
* Added `@JsonInclude.content` to allow specifying inclusion criteria for `java.util.Map` entries separate from inclusion of `Map` values

#### [Core Streaming](../../jackson-core)

* [#47](../../jackson-core/issues/47): Support `@JsonValue` for (Map) key serialization
* [#148](../../jackson-core/issues/148): `BytesToNameCanonicalizer` can mishandle leading null byte(s).
* [#164](../../jackson-core/issues/164): Add `JsonGenerator.Feature.IGNORE_UNKNOWN` (but support via individual data format modules)

#### [Core Databind](../../jackson-databind)

* [#113](../../jackson-databind/issues/113): Problem deserializing polymorphic types with `@JsonCreator`
* [#165](../../jackson-databind/issues/165): Give custom Deserializers access to the resolved target Class of the currently deserialized object (via `DeserializationContext.getContextualType()` during `createContextual()` callback)
* [#421](../../jackson-databind/issues/421): `@JsonCreator` not used in case of multiple creators with parameter names
* [#521](../../jackson-databind/issues/521): Keep bundle annotations, prevent problems with recursive
* [#527](../../jackson-databind/issues/527): Add support for `@JsonInclude(content=Include.NON_NULL)` (and others) for Maps
* [#528](../../jackson-databind/issues/528): Add support for `JsonType.As.EXISTING_PROPERTY`
* [#539](../../jackson-databind/issues/539): Problem with post-procesing of "empty bean" serializer
* [#540](../../jackson-databind/issues/540): Support deserializing `[]` as null or empty collection when the java type is a not an object (aka "work around PHP issues")
* [#543](../../jackson-databind/issues/543): Problem resolving self-referential recursive types
* [#552](../../jackson-databind/issues/552): Improved handling for ISO-8601 (date) format
* [#560](../../jackson-databind/issues/560): `@JsonCreator` to deserialize BigInteger to Enum
* [#565](../../jackson-databind/issues/565): Add support for handling `Map.Entry`
* [#566](../../jackson-databind/issues/566): Add support for case-insensitive deserialization (`MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES`)
* [#571](../../jackson-databind/issues/571): Add support in ObjectMapper for custom `ObjectReader`, `ObjectWriter` (sub-classes)
* [#607](../../jackson-databind/issues/607): Allow (re)config of `JsonParser.Feature`s via `ObjectReader`
* [#608](../../jackson-databind/issues/608): Allow (re)config of `JsonGenerator.Feature`s via `ObjectWriter`
* [#622](../../jackson-databind/issues/622): Support for non-scalar `ObjectId` Reference deserialization (like JSOG)
* [#631](../../jackson-databind/issues/631): Update `current value` of `JsonParser`, `JsonGenerator` from standard serializers, deserializers
* [#638](../../jackson-databind/issues/638): Add annotation-based method(s) for injecting properties during serialization (using `@JsonAppend`, `VirtualBeanPropertyWriter`)
* [#654](../../jackson-databind/issues/654): Add support for (re)configuring `JsonGenerator.setRootValueSeparator()` via `ObjectWriter`
* [#653](../../jackson-databind/issues/653): Added `MapperFeature.USE_STD_BEAN_NAMING` (by default, Jackson property name handling differs from std if 2 first chars are upper case)
* [#655](../../jackson-databind/issues/655): Add `ObjectWriter.writeValues()` for writing value sequences
* [#660](../../jackson-databind/issues/660) `@JsonCreator`-annotated factory method is ignored if constructor exists

### Changes, Data Formats

#### [CSV](../../jackson-dataformat-csv)

* [#50](../../jackson-dataformat-csv/issues/50): Support `JsonGenerator.Feature.IGNORE_KNOWN` for CSV, to ignoring extra columns
* [#53](../../jackson-dataformat-csv/issues/53): Add a way to specify "null value" (String) for `CsvGenerator` to use when writing `null`s (part of `CsvSchema`; method `withNullValue()`)
* [#56](../../jackson-dataformat-csv/issues/56): Support comments (either via `CsvSchema`, or using std `JsonParser.Feature.ALLOW_YAML_COMMENTS.
* [#57](../../jackson-dataformat-csv/issues/57): Support simple array values
* [#61](../../jackson-dataformat-csv/issues/61): Add a feature to always quote non-numeric values: `CsvGenerator.Feature.ALWAYS_QUOTE_STRINGS`

#### [YAML](../../jackson-dataformat-yaml)

* [#22](../../jackson-dataformat-yaml/issues/22): Add support for disabling use of YAML native Type Ids 
* [#23](../../jackson-dataformat-yaml/issues/23): Add support for disabling use of YAML native Object Ids 

### Changes, other modules

#### [JSON Schema](../../jackson-module-jsonSchema)

* [#32](../../jackson-module-jsonSchema/issues/32): Add support for Bean Validation (JSR-303) annotations