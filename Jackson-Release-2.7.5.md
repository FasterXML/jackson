Patch version of [2.7](Jackson-Release-2.7), released on Jun 11th, 2016.

Following fixes were included.

### Changes, core

#### [Streaming](../../jackson-core)

* [#280](../../jackson-core/issues/280): `FilteringGeneratorDelegate.writeUTF8String()` should delegate to `writeUTF8String()`

#### [Databind](../../jackson-databind)

* [#1098](../../jackson-databind/issues/1098): DeserializationFeature.FAIL_ON_INVALID_SUBTYPE does not work with `JsonTypeInfo.Id.CLASS`
* [#1223](../../jackson-databind/issues/1223): `BasicClassIntrospector.forSerialization(...).findProperties` should respect MapperFeature.AUTO_DETECT_GETTERS/SETTERS
* [#1225](../../jackson-databind/issues/1225): `JsonMappingException` should override `getProcessor()`
* [#1228](../../jackson-databind/issues/1228): `@JsonAnySetter` does not deserialize null to Deserializer's `NullValue`
* [#1231](../../jackson-databind/issues/1231): `@JsonSerialize(as=superType)` behavior disallowed in 2.7.4
* [#1248](../../jackson-databind/issues/1248): `Annotated` returns raw type in place of Generic Type in 2.7.x
* [#1253](../../jackson-databind/issues/1253): Problem with context handling for `TokenBuffer`, field name
* [#1255](../../jackson-databind/issues/1255): JsonIdentityInfo incorrectly serializing forward references

### Data formats

#### [CSV](../../jackson-dataformat-csv)

* [#124](../../jackson-dataformat-csv/issues/124): jackson-annotations are not included as a dependency within jackson-dataformat-csv

### Datatypes

#### [Java 8 Datetime](../../jackson-datatype-jsr310)

* [#79](../../jackson-datatype-jsr310/issues/79): Can't deserialize `Instant` from JSON of serialized `java.util.Date`

### [Jackson jr](../../jackson-jr)

* [#42](../../jackson-jr/issues/42): Incorrect `jackson-core` dependency form parent pom leads to inclusion of non-shaded core jar in `jr-all`

### Changes, other modules

#### [JSON Schema](../../jackson-module-jsonSchema)

* [#104](../../jackson-module-jsonSchema/issues/104): HyperSchema does not generate links for nested objects

#### [Kotlin Module](https://github.com/FasterXML/jackson-module-kotlin)

* Update to Kotlin 1.0.2
* [Pull Request #5](https://github.com/FasterXML/jackson-module-kotlin/pull/5): Any fields present in the constructor do not have to be null 