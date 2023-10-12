Patch version of [2.6](Jackson-Release-2.6),  released on September 15, 2015.

### Changes, core

#### [Streaming](../../jackson-core)

* [#213](../../jackson-core/issues/213): Parser is sometimes wrong when using `CANONICALIZE_FIELD_NAMES`
* [#216](../../jackson-core/issues/216): `ArrayIndexOutOfBoundsException` when repeatedly serializing to a byte array

#### [Databind](../../jackson-databind)

* [#894](../../jackson-databind/issues/894): When using `withFactory` on `ObjectMapper`, the created Factory has a `TypeParser` which still has the original `TypeFactory`
* [#899](../../jackson-databind/issues/899): Problem serializing `ObjectReader` (and possibly `ObjectMapper`)
* [#913](../../jackson-databind/issues/913): `ObjectMapper.copy()` does not preserve `MappingJsonFactory` features
* [#922](../../jackson-databind/issues/922): `ObjectMapper.copy()` does not preserve `_registeredModuleTypes`
* [#928](../../jackson-databind/issues/928): Problem deserializing External Type Id if type id comes before POJO

### Changes, data formats

#### [CBOR](../../jackson-dataformat-cbor)

* [#13](../../jackson-dataformat-cbor/issues/13): Bug in boundary checking in `CBORParser`

#### [Smile](../../jackson-dataformat-smile)

* [#26](../../jackson-dataformat-smile/issues/26): Bug in boundary checking in `SmileParser`

### Changes, datatypes

#### [Guava](../../jackson-datatype-guava)

* [#52](../../jackson-datatype-guava/issues/52): Guava collection types do not allow null values
* [#80](../../jackson-datatype-guava/issues/80): Relax OSGi version constraints for Guava dependency.
* [#82](../../jackson-datatype-guava/issues/82): Problem with polymorphic value types for `Optional`, with 2.6

#### [JDK8](../../jackson-datatype-jdk8)

* [#14](../../jackson-datatype-jdk8/issues/14): Missing `@type` when serializing `Optional<Interface>`

#### [Joda](../../jackson-datatype-joda)

* [#71](../../jackson-datatype-joda/issues/71): Adjust LocalDate / LocalDateTime deserializer to support `DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE`

### Changes, other modules

#### [Afterburner](../../jackson-module-afterburner)

* [#56](../../jackson-module-afterburner/issues/56): Afterburner does not respect `DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS`
* [#57](../../jackson-module-afterburner/issues/57): `@JsonAppend` causes `NullPointerException`

#### [JAXB Annotations](../../jackson-module-jaxb-annotations)

* [#47](../../jackson-module-jaxb-annotations/issues/47): `JaxbAnnotationIntrospector` does not pick up 'required' property of `@XmlAttribute`

#### [JSON Schema](../../jackson-module-jsonSchema)

* [#27](../../jackson-module-jsonSchema/issues/27): Object schema properties should adhere to `@JsonPropertyOrder` and `@XmlType(propOrder)`