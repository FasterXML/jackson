Patch version of [2.8](Jackson-Release-2.8), released on June 12, 2017.

Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core)

* [#382](../../jackson-core/issues/382): ArrayIndexOutOfBoundsException from `UTF32Reader.read()` on invalid input

#### [Databind](../../jackson-databind)

* [#1585](../../jackson-databind/issues/1585): Invoke ServiceLoader.load() inside of a privileged block when loading modules using `ObjectMapper.findModules()`
* [#1595](../../jackson-databind/issues/1595): `JsonIgnoreProperties.allowSetters` is not working in Jackson 2.8 
* [#1597](../../jackson-databind/issues/1597): Escape JSONP breaking characters
* [#1599](../../jackson-databind/issues/1599): Jackson Deserializer security vulnerability with default typing
* [#1607](../../jackson-databind/issues/1607): `@JsonIdentityReference` not used when setup on class only
* [#1629](../../jackson-databind/issues/1629): `FromStringDeserializer` ignores registered `DeserializationProblemHandler` for `java.util.UUID`
* [#1642](../../jackson-databind/issues/1642): Support READ_UNKNOWN_ENUM_VALUES_AS_NULL with @JsonCreator
* [#1647](../../jackson-databind/issues/1647): Missing properties from base class when recursive types are involved
* [#1648](../../jackson-databind/issues/1648): `DateTimeSerializerBase` ignores configured date format when creating contextual
* [#1651](../../jackson-databind/issues/1651): `StdDateFormat` fails to parse 'zulu' date when TimeZone other than UTC

### Changes, dataformats

##### [Protobuf](../../jackson-dataformats-binary)

* [#72](../../jackson-dataformats-binary/issues/72): parser fails with /* comment */
* [#85](../../jackson-dataformats-binary/issues/85): `_decode32Bits()` bug in `ProtobufParser`

##### [XML](../../jackson-dataformat-xml)

* [#228](../../jackson-dataformat-xml/issues/228): `XmlReadContext` should hold current value
* [#233](../../jackson-dataformat-xml/issues/233): `XmlMapper.copy()` doesn't properly copy internal configurations

### Changes, other

#### [Jackson jr](../../jackson-jr)

* [#50](../../jackson-jr/issues/50): Duplicate key detection does not work
