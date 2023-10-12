Patch version released on 01-Mar-2014. Following changes included.

### Changes, core

#### [Core Streaming](../../jackson-core)

* [#126](../../jackson-core/issues/126): Revert some 1.6 back to make core lib work with Android 2.2 (FroYo) (NOTE: temporary; not merged in 2.4)
* [#129](../../jackson-core/issues/129): Missing delegation method, `JsonParserDelegate.isExpectedStartArrayToken()`

#### [Core Databind](../../jackson-databind)

* [#378](../../jackson-databind/issues/378): Fix a problem with custom `Enum` deserializer construction
* [#379](../../jackson-databind/issues/379): Fix a problem with (re)naming of Creator properties; needed to make [Paranamer](../../jackson-module-paranamer/) module work with `NamingStrategy`
* [#398](../../jackson-databind/issues/398): Should deserialize empty (not null) URI from empty String
* [#406](../../jackson-databind/issues/406): `@JsonTypeIdResolver` not working with external type ids
* [#411](../../jackson-databind/issues/411): `NumberDeserializers` throws exception with `NaN` and +/- Infinity
* [#412](../../jackson-databind/issues/412): `ObjectMapper.writerWithType()` does not change root name being used
* Added `BeanSerializerBase._serializeObjectId()` needed by modules that override standard BeanSerializer; specifically, XML module.

### Changes, [JAX-RS](../../jackson-jaxrs-providers)

* [#40](../../jackson-jaxrs-providers/issues/40): Allow use of "text/x-json" content type by default
* [#42](../../jackson-jaxrs-providers/issues/42): Add CBOR provider (using new [Jackson CBOR module](../../jackson-dataformat-cbor))
* [#43](../../jackson-jaxrs-providers/issues/43): Verify that format-specific mappers are properly overridden (like `XmlMapper` for xml)

### Changes, Data formats

#### [CSV](../../jackson-dataformat-csv)

* [#15](../../jackson-dataformat-csv/issues/15): Problem with SmileGenerator._writeBytes(...), bounds checks

#### [XML](../../jackson-dataformat-xml)

* [#81](../../jackson-dataformat-xml/issues/81): Serialization of a polymorphic class As.Property with Identity info doesn't work (NOTE! Depends on a related fix #129 in `jackson-databind`, see above)
* [#91](../../jackson-dataformat-xml/issues/91): `@JsonPropertyOrder` not working correctly with attributes
* [#103](../../jackson-dataformat-xml/issues/103): Serialize wrapped 'null' Lists correctly

### Changes, Data types

#### [Joda](../../jackson-datatype-joda)

* [#16](../../jackson-datatype-joda/issues/16): Adjust existing Date/Time deserializers to support `DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE`

### Changes, other modules

#### Afterburner

* [#39](../../jackson-module-afterburner/issues/39): Afterburner does not respect `JsonInclude.Include.NON_EMPTY`
