Patch version of [2.9](Jackson-Release-2.9), released 15th December, 2018.

Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core)

* [#488](../../jackson-core/issues/488): Fail earlier on coercions from "too big" `BigInteger` into fixed-size types (`int`, `long`, `short`)
* Improve exception message for missing Base64 padding

#### [Databind](../../jackson-databind)

* [#1662](../../jackson-databind/issues/1662): `ByteBuffer` serialization is broken if offset is not 0
* [#2155](../../jackson-databind/issues/2155): Type parameters are checked for equality while isAssignableFrom expected
* [#2167](../../jackson-databind/issues/2167): Large ISO-8601 Dates are formatted/serialized incorrectly
* [#2181](../../jackson-databind/issues/2181): Don't re-use dynamic serializers for property-updating copy constructors
* [#2183](../../jackson-databind/issues/2183): Base64 JsonMappingException: Unexpected end-of-input
* [#2186](../../jackson-databind/issues/2186): Block more classes from polymorphic deserialization (CVE-2018-19360, CVE-2018-19361, CVE-2018-19362)
* [#2197](../../jackson-databind/issues/2197): Illegal reflective access operation warning when using `java.lang.Void` as value type

### Changes, data formats

#### [Protobuf](../../jackson-dataformats-binary)

* [#140](../../jackson-dataformats-binary/issues/140): Stack overflow when generating Protobuf schema  on class containing cyclic type definition

#### [Smile](../../jackson-dataformats-binary)

* [#153](../../jackson-dataformats-binary/issues/153): Unable to set a compression input/output decorator to a `SmileFactory`

#### [XML](../../jackson-dataformat-xml)

* [#270](../../jackson-dataformat-xml/issues/270): Add support for `writeBinary()` with `InputStream` to `ToXMLGenerator`
* [#323](../../jackson-dataformat-xml/issues/323): Replace slow string concatenation with faster `StringBuilder` (for long text content)

#### [YAML](../../jackson-dataformats-text)

* [#99](../../jackson-dataformats-text/issues/99): `YamlGenerator` closes the target stream when configured not to

### Changes, datatypes

#### [Java 8](../../jackson-modules-java8)

* [#90](../../jackson-modules-java8/issues/90): Performance issue with malicious `BigDecimal` input, `InstantDeserializer`, `DurationDeserializer` (note: `CVE-2018-1000873`)

