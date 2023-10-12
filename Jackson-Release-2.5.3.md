Patch version of [2.5](Jackson Release 2.5). Was released on 24-Apr-2015.

### Changes, core

#### [Streaming](../../jackson-core)

* [#191](../../jackson-core/issues/191): Longest collision chain in symbol table now exceeds maximum, for single-quoted-character symbols

#### [Core Databind](../../jackson-databind)

* [#731](../../jackson-databind/issues/731): `XmlAdapter` result marshaling error in case of ValueType == `Object.class`
* [#742](../../jackson-databind/issues/742): Allow deserialization of `null` Object Id	(missing already allowed)
* [#744](../../jackson-databind/issues/744): Custom deserializer with parent object update failing
* [#745](../../jackson-databind/issues/745): `EnumDeserializer.deserializerForCreator()` fails when used to deserialize a Map key
* [#761](../../jackson-databind/issues/761): Builder deserializer: in-compatible type exception when return type is super type
* [#766](../../jackson-databind/issues/766): Fix Infinite recursion (StackOverflowError) when serializing a SOAP object

### Changes, datatypes

#### [JSR-310](../../jackson-datatype-jsr310)

* [#21](../../jackson-datatype-jsr310/issues/21): `ClassNotFoundException` in OSGi

### Changes, data formats

#### [CSV](../../jackson-dataformat-csv)

* [#75](../../jackson-dataformat-csv/issues/75): Support escapes at beginning of the file

### Changes, other modules

#### [Scala](../../jackson-module-scala)

* [#202](../../jackson-module-scala/issues/202): Getter is not detected correctly when method name is identical to variable name

#### [JAXB Annotations](../../jackson-module-jaxb-annotations)

* [#40](../../jackson-module-jaxb-annotations/issues/40): XmlElementRef ignored if inside XmlElementWrapper
