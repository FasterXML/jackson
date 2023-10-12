Patch version of [2.9](Jackson-Release-2.9), released 24-Jan-2018.

Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core)

* [#414](../../jackson-core/issues/414): Base64 MIME variant does not ignore white space chars as per RFC2045

#### [Databind](../../jackson-databind)

* [#1382](../../jackson-databind/issues/1382): `@JsonProperty(access=READ_ONLY)` unxepected behaviour with `Collections`
* [#1673](../../jackson-databind/issues/1673): Serialising generic value classes via Reference Types (like Optional) fails to include type information
* [#1729](../../jackson-databind/issues/1729): Integer bounds verification when calling `TokenBuffer.getIntValue()`
* [#1853](../../jackson-databind/issues/1853): Deserialize from Object (using Creator methods) returns field name instead of value
* [#1854](../../jackson-databind/issues/1854): NPE deserializing collection with `@JsonCreator` and `ACCEPT_CASE_INSENSITIVE_PROPERTIES`
* [#1855](../../jackson-databind/issues/1855): Blacklist for more serialization gadgets (dbcp/tomcat, spring)
* [#1859](../../jackson-databind/issues/1859): Issue handling unknown/unmapped Enum keys
* [#1868](../../jackson-databind/issues/1868): Class name handling for JDK unmodifiable Collection types changed
* [#1870](../../jackson-databind/issues/1870): Remove `final` on inherited methods in `BuilderBasedDeserializer` to allow overriding by subclasses
* [#1878](../../jackson-databind/issues/1878): `@JsonBackReference` property is always ignored when deserializing since 2.9.0
* [#1895](../../jackson-databind/issues/1895): Per-type config override "JsonFormat.Shape.OBJECT" for Map.Entry not working
* [#1899](../../jackson-databind/issues/1899): Another two gadgets to exploit default typing issue in jackson-databind
* [#1906](../../jackson-databind/issues/1906): Add string format specifier for error message in `PropertyValueBuffer`
* [#1907](../../jackson-databind/issues/1907): Remove `getClass()` from `_valueType` argument for error reporting

### Changes, dataformats

#### [Textual formats](../../jackson-dataformats-text/)

* [#62](../../jackson-dataformats-binary/issues/62): (yaml) SnakeYAML `base64Coder` is not OSGI exported
* [#65](../../jackson-dataformats-binary/issues/65): (yaml) `YAMLParser` incorrectly handles numbers with underscores in them

### Changes, other modules

#### [Afterburner](../../jackson-modules-base/tree/master/afterburner)

* [#38](../../jackson-modules-base/issues/38): (afterburner) Handle `DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES` correctly
