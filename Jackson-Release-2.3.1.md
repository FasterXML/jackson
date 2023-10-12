Patch release was made in 28-Dec-2013 and contains following fixes

### Changes, core

#### [Core Streaming](../../jackson-core)

No functional changes.

#### [Core Databind](../../jackson-databind)

* [#346](../../jackson-databind/issues/346): Fix problem deserializing `ObjectNode`, with `@JsonCreator`, empty JSON Object
* [#358](../../jackson-databind/issues/358): `IterableSerializer` ignoring annotated content serializer
* [#361](../../jackson-databind/issues/361): Reduce sync overhead for `SerializerCache` by using `volatile`, double-locking
* [#362](../../jackson-databind/issues/362): UUID output as Base64 String with `ObjectMapper.convertValue()`
* [#367](../../jackson-databind/issues/367): Make `TypeNameIdResolver` call `TypeResolver` for resolving base type.
* Related: fix for [Afterburner#38](../../jackson-module-afterburner/issues/38) -- need to remove `@JacksonStdImpl` from `RawSerializer`, to avoid accidental removal of proper handling.

### Changes, [JAX-RS](../../jackson-jaxrs-providers)

* [#37](../../jackson-jaxrs-providers/issues/37): Enable use of JAX-RS 2.0 API

### Changes, Data formats

#### [XML](../../jackson-dataformat-xml)

* [#84](../../jackson-dataformat-xml/issues/84): Problem with `@JacksonXmlText` when property output is suppressed

### Changes, Datatypes

#### [Joda](../../jackson-datatype-joda)

* [#21](../../jackson-datatype-joda/issues/21): `DateTimeSerializer` should take the configured time zone into account

### Changes, other modules

#### [Afterburner](../../jackson-module-afterburner)

* [#38](../../jackson-module-afterburner/issues/38): Handling of `@JsonRawValue` broken (requires 2.3.1 of `jackson-databind` as well)
