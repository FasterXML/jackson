Patch version of [2.8](Jackson-Release-2.8), released February 21st, 2017.

Following fixes are included.

### Changes, core

#### [Databind](../../jackson-databind)

* [#935](../../jackson-databind/issues/935): `@JsonProperty(access = Access.READ_ONLY)` - unexpected behaviour
* [#1317](../../jackson-databind/issues/1317): '@JsonIgnore' annotation not working with creator properties, serialization
* [#1367](../../jackson-databind/issues/1367): No Object Id found for an instance when using `@ConstructorProperties`
* [#1505](../../jackson-databind/issues/1505): `@JsonEnumDefaultValue` should take precedence over `DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS`
* [#1506](../../jackson-databind/issues/1506): Missing `KeyDeserializer` for `CharSequence`
* [#1513](../../jackson-databind/issues/1513): `MapSerializer._orderEntries()` throws NPE when operating on `ConcurrentHashMap`

### Changes, dataformats

#### [Avro](../../jackson-dataformats-binary)

* [#34](../../jackson-dataformats-binary/issues/34): Reading Avro with specified reader and writer schemas
* [#35](../../jackson-dataformats-binary/issues/35): Serialization of multiple objects (`SequenceWriter`)
* [#38](../../jackson-dataformats-binary/issues/38): Deserialization of multiple (root) values from Avro
* [#39](../../jackson-dataformats-binary/issues/39): Problem decoding Maps with union values

#### [XML](../../jackson-dataformat-xml)

* [#220](../../jackson-dataformat-xml/issues/220): Avoid root-level wrapping for Map-like types, not just Maps
* [#222](../../jackson-dataformat-xml/issues/222): `DefaultXmlPrettyPrinter` indentation configuration not preserved

### Changes, datatypes

#### [Hibernate](../../jackson-datatype-hibernate)

* [#102](../../jackson-datatype-hibernate/issues/102): `NoSuchMethodException` with Hibernate 5.2
