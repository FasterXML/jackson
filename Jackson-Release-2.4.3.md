Patch version of [2.4](Jackson Release 2.4), most components released on October 4th, 2014.

### Changes, core

#### [Streaming](../../jackson-core)

* [#152](../../jackson-core/issues/152): Exception for property names longer than 256k

#### [Databind](../../jackson-databind)

* [#496](../../jackson-databind/issues/496): Wrong result with	`new TextNode("false").asBoolean(true)`
* [#511](../../jackson-databind/issues/511): `DeserializationFeature.FAIL_ON_INVALID_SUBTYPE` does not work
* [#523](../../jackson-databind/issues/523): `MapDeserializer` and friends do not report the field/key name for mapping exceptions
* [#524](../../jackson-databind/issues/524): `@JsonIdentityReference(alwaysAsId = true)` Custom resolver 
is reset to `SimpleObjectIdResolver`.
* [#541](../../jackson-databind/issues/541): `@JsonProperty` in `@JsonCreator` is conflicting with POJOs getters/attributes
* [#543](../../jackson-databind/issues/543): Problem resolving self-referential generic types (work-around, not full fix)
* [#570](../../jackson-databind/issues/570): Add Support for parsing more compliant ISO-8601 Date Formats

### Changes, data formats

#### [CBOR](../../jackson-dataformat-cbor)

* [#5](../../jackson-dataformat-cbor/issues/5): Support binary (byte[]) Object keys (assuming UTF-8 encoding)
* [#6](../../jackson-dataformat-cbor/issues/6): Support 'self-describe' CBOR tag

### Changes, datatypes

#### [Joda](../../jackson-datatype-joda)

* [#45](../../jackson-datatype-joda/issues/45): Can't use `LocalTime`, `LocalDate` & `LocalDateTime` as Key type for a MapInterval deserialization fails for negative start instants
* [#46](../../jackson-datatype-joda/issues/46): `Interval` deserialization fails for negative start instants

#### [Guava](../../jackson-datatype-guava)

* [#50](../../jackson-datatype-guava/issues/50): Add support for `InternetDomainName`

### Changes, other modules

#### [JSON Schema](../../jackson-module-jsonSchema)

* [#34](../../jackson-module-jsonSchema/issues/34): NPE when generating schema for class with `@JsonValue` annotation over `array`/`Collection`

#### [Afterburner](../../jackson-module-afterburner)

* [#42](../../jackson-module-afterburner/issues/42): Exception trying to deserialize into `final` field
* [#43](../../jackson-module-afterburner/issues/43): Problem deserializing type with one polymorphic property that uses inclusion of `As.EXTERNAL_PROPERTY`
