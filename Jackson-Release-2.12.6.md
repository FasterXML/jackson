Patch version of [2.12](Jackson-Release-2.12), released on December 15, 2021.
Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core)

* [#713](../../jackson-core/issues/713): Incorrect parsing of single-quoted surrounded String values containing double quotes

#### [Databind](../../jackson-databind)

* [#3280](../../jackson-databind/issues/3280): Can not deserialize json to enum value with Object-/Array-valued input,
  `@JsonCreator`
* [#3305](../../jackson-databind/issues/3305): ObjectMapper serializes `CharSequence` subtypes as POJO instead of
  as String (JDK 15+)
* [#3328](../../jackson-databind/issues/3328): Possible DoS if using JDK serialization to serialize `JsonNode`

### Changes, data formats

#### [Ion](../../jackson-dataformats-binary)

* [#302](../../jackson-dataformats-binary/issues/302): `IllegalArgumentException` in `IonParser.getEmbeddedObject()`

#### [XML](../../jackson-dataformat-xml)

* [#490](../../jackson-dataformat-xml/issues/490): Problem when using defaultUseWrapper(false) in combination with polymorphic types
