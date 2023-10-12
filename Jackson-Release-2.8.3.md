Patch version of [2.8](Jackson-Release-2.8), released Sep 18, 2016.

Following fixes are included; as well as all fixes from 2.7 branch up to 2.7.7 (and any added for 2.7.8 by release date).

### Changes, core

#### [Streaming](../../jackson-core/)

* [#318](../../jackson-core/issues/318): Add support for writing `byte[]` via `JsonGenerator.writeEmbeddedObject()`

#### [Databind](../../jackson-databind/)

* [#929](../../jackson-databind/issues/929): `@JsonCreator` not working on a factory with multiple arguments for a enum type
* [#1351](../../jackson-databind/issues/1351): `@JsonInclude(NON_DEFAULT)` doesn't omit null fields
* [#1353](../../jackson-databind/issues/1353): Improve error-handling for `java.net.URL` deserialization
* [#1361](../../jackson-databind/issues/1361): Change `TokenBuffer` to use new `writeEmbeddedObject()` if possible

### Changes, other

#### [JAXB Annotations](../../jackson-module-jaxb-annotations/)

* [#61](../../jackson-module-jaxb-annotations/issues/61): Transient fields serialized when `@XmlAccessorType(XmlAccessType.FIELD)` is present
