Patch version of [2.11](Jackson-Release-2.11), released 25th June 2020.

Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core)

#### [Databind](../../jackson-databind)

* [#2486](../../jackson-databind/issues/2486): Builder Deserialization with JsonCreator Value vs Array
* [#2725](../../jackson-databind/issues/2725): JsonCreator on static method in Enum and Enum used as key in map fails randomly
* [#2755](../../jackson-databind/issues/2755): `StdSubtypeResolver` is not thread safe (possibly due to copy not being made with `ObjectMapper.copy()`)
* [#2757](../../jackson-databind/issues/2757): "Conflicting setter definitions for property" exception for `Map` subtype during deserialization
* [#2758](../../jackson-databind/issues/2758): Fail to deserialize local Records
* [#2759](../../jackson-databind/issues/2759): Rearranging of props when property-based generator is in use leads to incorrect output
* [#2767](../../jackson-databind/issues/2767): `DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS` don't support `Map` type field
* [#2770](../../jackson-databind/issues/2770): JsonParser from MismatchedInputException cannot getText() for floating-point value

### Changes, data formats

#### [XML](../../jackson-dataformat-xml)

* [#86](../../jackson-dataformat-xml/issues/86): Can not deserialize unwrapped list when `@JacksonXmlProperty` localName matches `@JacksonXmlRootElement` localName
* [#294](../../jackson-dataformat-xml/issues/294): XML parser error with nested same element names
* [#301](../../jackson-dataformat-xml/issues/301): Problem deserializing POJO with unwrapped `List`, ignorable attribute value
* [#389](../../jackson-dataformat-xml/issues/389): Exception when serializing with type via mapper.writerFor(type).write(...)
* [#393](../../jackson-dataformat-xml/issues/393): `MismatchedInputException` for nested repeating element name in `List`
* [#399](../../jackson-dataformat-xml/issues/399): Can not deserialize unwrapped list when `@JacksonXmlProperty` localName matches the parent's localName
* [#404](../../jackson-dataformat-xml/issues/404): Make `@JacksonXmlElementWrapper` indicate XML property

#### [YAML](../../jackson-dataformats-text/yaml)

* [#51](../../jackson-dataformats-text/issues/51): `YAMLParser._locationFor()` does not use index available from `Mark`
* [#201](../../jackson-dataformats-text/issues/201): Improve `MINIMIZE_QUOTES` handling to avoid quoting for some uses of `#` and `:`

### Changes, other modules

#### Afterburner

* [#97](../../jackson-modules-base/issues/97): (partial fix) Afterburner breaks serialization of ObjectMapper 

### Changes, other

#### [jackson-jr](../../jackson-jr)

* [#72](../../jackson-jr/issues/72): Duplicate classes from `com.fasterxml.jackson.jr.ob` and `com.fasterxml.jackson.jr.type` in 2.11.0
