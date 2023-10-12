Patch version of [2.9](Jackson-Release-2.9), released on May 16th 2019.

Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core)

* [#516](../../jackson-core/issues/516): _inputPtr off-by-one in UTF8StreamJsonParser._parseNumber2()
* [#531](../../jackson-core/issues/531): Non-blocking parser reports incorrect locations when fed with non-zero offset

#### [Databind](../../jackson-databind)

* [#1408](../../jackson-databind/issues/1408): Call to `TypeVariable.getBounds()` without synchronization unsafe on some platforms
* [#2221](../../jackson-databind/issues/2221): `DeserializationProblemHandler.handleUnknownTypeId()` returning `Void.class`, enableDefaultTyping causing NPE
* [#2251](../../jackson-databind/issues/2251): Getter that returns an abstract collection breaks a delegating `@JsonCreator`
* [#2265](../../jackson-databind/issues/2265): Inconsistent handling of Collections$UnmodifiableList vs Collections$UnmodifiableRandomAccessList
* [#2299](../../jackson-databind/issues/2299): Fix for using jackson-databind in an OSGi environment under Android
* [#2303](../../jackson-databind/issues/2303): Deserialize null, when java type is "TypeRef of TypeRef of T",
does not provide "Type(Type(null))"
* [#2324](../../jackson-databind/issues/2324): `StringCollectionDeserializer` fails with custom collection
* [#2326](../../jackson-databind/issues/2326): Block one more gadget type (CVE-2019-12086)

### Changes, data formats

#### Binary

* [#155](../../jackson-dataformats-binary/issues/155): (multiple) Inconsistent support for `StreamWriteFeature.FLUSH_PASSED_TO_STREAM`
* [#159](../../jackson-dataformats-binary/issues/159): (cbor) Use of longer-than-optimal encoding for String lengths

#### CSV

* [#122](../../jackson-dataformats-text/issues/122): `readValues(null)` causes infinite loop
* [#124](../../jackson-dataformats-text/issues/124): Add `CsvGenerator.Feature.ESCAPE_CONTROL_CHARS_WITH_ESCAPE_CHAR` for escaping non-printable characters in CSV output/input

#### XML

* [#333](../../jackson-dataformat-xml/issues/333): `OutputDecorator` not called with `XmlMapper`

#### YAML

* [#63](../../jackson-dataformats-text/issues/63): `null` Object Id serialized as anchor for YAML
* [#68](../../jackson-dataformats-text/issues/68): When field names are reserved words, they should be written out with quotes
* [#90](../../jackson-dataformats-text/issues/90): Exception when decoding Jackson-encoded `Base64` binary value in YAML
* [#123](../../jackson-dataformats-text/issues/123): YAML Anchor, reference fails with simple example

### Changes, datatypes

#### [Guava](../../jackson-datatypes-collections)

* [#45](../../jackson-datatypes-collections/issues/45): HostAndPortDeserializer rely on older version property name

#### [json-org](../../jackson-datatype-json-org)

* [#15](../../jackson-datatype-json-org/issues/15): Misleading exception when trying to deserialize JSON String as `org.json.JSONArray` value

### Changes, other modules

#### [Afterburner](../../jackson-modules-base/afterburner)

* [#49](../../jackson-modules-base/issues/49): Afterburner `MyClassLoader#loadAndResolve()` is not idempotent when `tryToUseParent` is true
* [#69](../../jackson-modules-base/issues/69): `ALLOW_COERCION_OF_SCALARS` ignored deserializing scalars with Afterburner

#### [Mr Bean](../../jackson-modules-base/mrbean)

* [#74](../../jackson-modules-base/issues/74): MrBean module should not materialize `java.io.Serializable`

### Changes, jackson-jr

* Fix an issue with Maps-of-Lists, Lists-of-Maps
