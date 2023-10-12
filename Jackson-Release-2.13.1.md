Patch version of [2.13](Jackson-Release-2.13), released 19-Dec-2021.

Following fixes are included in this patch release.

### Changes, core

#### [Streaming](../../jackson-core)

* [#721](../../issues/jackson-core/issues/721): Incorrect parsing of single-quoted surrounded String values containing double quotes
* [#733](../../issues/jackson-core/issues/733): Add `StreamReadCapability.EXACT_FLOATS` to indicate whether parser reports exact floating-point values or not

#### [Databind](../../jackson-databind)

* [#3006](../../jackson-databind/issues/3006): Argument type mismatch for `enum` with `@JsonCreator` that takes String,
  gets JSON Number
* [#3299](../../jackson-databind/issues/3299): Do not automatically trim trailing whitespace from `java.util.regex.Pattern` values
* [#3305](../../jackson-databind/issues/3305): ObjectMapper serializes `CharSequence` subtypes as POJO instead of as String (JDK 15+)
* [#3308](../../jackson-databind/issues/3308): `ObjectMapper.valueToTree()` fails when `DeserializationFeature.FAIL_ON_TRAILING_TOKENS` is enabled
* [#3328](../../jackson-databind/issues/3328): Possible DoS if using JDK serialization to serialize `JsonNode`

### Changes, data formats

#### [CSV](../../jackson-dataformats-text)

* [#288](../../jackson-dataformats-text): Caching conflict when creating CSV schemas with different views for the same POJO

#### [Ion](../../jackson-dataformats-binary)

* [#302](../../jackson-dataformats-binary/issues/302): `IllegalArgumentException` in `IonParser.getEmbeddedObject()`

#### XML

* [#493](../../jackson-dataformat-xml/issues/493): SequenceWriter returns NPE when trying XML serialization

### Changes, JVM Languages

#### [Kotlin](../../jackson-module-kotlin)

* [#449](../../jackson-module-kotlin/issues/449): Refactor AnnotatedMethod.hasRequiredMarker()
* [#456](../../jackson-module-kotlin/issues/456): Refactor KNAI.findImplicitPropertyName()
* [#521](../../jackson-module-kotlin/issues/521): Fixed lookup of instantiators

#### [Scala](../../jackson-module-scala)

* [#561](../../jackson-module-scala/561): Move ScalaAnnotationIntrospector state to ScalaAnnotationIntrospectorModule

### Changes, Other modules

#### No-Ctor Deser

* [#155](../../jackson-modules-base/issues/155): Fix SPI metadata for `jackson-module-no-ctor-deser`
* [#159](../../jackson-modules-base/issues/159): NoCtorDeserModule missing - Do you mean NoCtorModule
