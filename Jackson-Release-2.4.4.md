Patch version of [2.4](Jackson Release 2.4), released on November 24, 2014.

### Changes, core

#### [Streaming](../../jackson-core)

* [#157](../../jackson-core/issues/157): `ArrayIndexOutOfBoundsException` for numbers longer than 200 characters (NOTE: regression, not affecting 2.3)
* [#158](../../jackson-core/issues/158): Setter confusion on assignable types (actually, belongs under `databind`)

#### [Databind](../../jackson-databind)

* [#245](../../jackson-databind/issues/245): Calls to `ObjectMapper.addMixInAnnotations()` on an instance returned by `ObjectMapper.copy()` don't work
* [#580](../../jackson-databind/issues/580): delegate deserializers choke on a (single) abstract/polymorphic parameter
* [#590](../../jackson-databind/issues/590): Binding invalid `Currency` gives nonsense at end of the message
* [#592](../../jackson-databind/issues/592): Wrong `TokenBuffer` delegate deserialization using `@JsonCreator`
* [#601](../../jackson-databind/issues/592): ClassCastException for a custom serializer for enum key in `EnumMap`
* [#604](../../jackson-databind/issues/604): `Map` deserializers were not being cached, causing performance issues due to lock congestion.
* [#610](../../jackson-databind/issues/610): Fix forward (Object Id) reference in hierarchies

### Changes, data formats

#### [CSV](../../jackson-dataformat-csv)

* [#54](../../jackson-dataformat-csv/issues/54): Encounter `ArrayIndexOutOfBoundsException` in the corner case delimiter or end-of-line happened to be the leading character of a segment buffer

#### [YAML](../../jackson-dataformat-yaml)

* [#27](../../jackson-dataformat-yaml/issues/27): OSGI bundle does not work due to shading

### Changes, other modules

#### [JAXB Annotations](../../jackson-module-jaxb-annotations)

* [#33](../../jackson-module-jaxb-annotations/issues/33)

#### [JSON Schema](../../jackson-module-jsonSchema)

* [#46](../../jackson-module-jsonSchema/issues/46): Incorrect number type for `Double`, `Float` and `BigDecimal`
* [#47](../../jackson-module-jsonSchema/issues/47): Static `VisitorContext` results in incomplete JSON schema output

#### [Scala](../../jackson-module-scala)

* [#149](../../jackson-module-scala/issues/149): Use type information to deser `Option` (courtesy of @orac)
* [#148](../../jackson-module-scala/issues/148): Performance regression in 2.2.3
* [#145](../../jackson-module-scala/issues/145): readValue for `Map[String, Any]` or `List[Any]` is very slow