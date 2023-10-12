Patch version of [2.5](Jackson Release 2.5). Released on March 30, 2015.

### Changes, core

#### [Core Databind](../../jackson-databind)

* [#609](../../jackson-databind/issues/609): Problem resolving locally declared generic type
* [#691](../../jackson-databind/issues/691): `NullSerializer` for `MapProperty` failing
* [#703](../../jackson-databind/issues/703): Multiple calls to `ObjectMapper#canSerialize(Object.class)` return different values
* [#705](../../jackson-databind/issues/705): `JsonAnyGetter` doesn't work with `@JsonSerialize` (except with keyUsing)
* [#728](../../jackson-databind/issues/728): `TypeFactory._fromVariable()` returns `unknownType()` even though it has enough information to provide a more specific type
* [#733](../../jackson-databind/issues/733): `MappingIterator` should move past errors or not return hasNext() == true

### Changes, dataformat modules

#### [CSV](./../jackson-dataformat-csv)

* [#66](../../jackson-dataformat-csv/issues/66): Deserializing an empty string as an array field return a non-empty list of one empty String 
* [#69](../../jackson-dataformat-csv/issues/69): `SequenceWriter#write(null)` writes a single null, not an entire row of nulls

### Changes, datatype modules

#### [Guava](../../jackson-datatype-guava)

* [#62](../../jackson-datatype-guava/issues/62): Add `com.google.common.hash` to OSGi import list

#### [Java8 Date](../../jackson-datatype-jsr310)

* [#20](../../jackson-datatype-jsr310/issues/20): Enhance YearMonth Serialization/Deserialization to	allow custom formatting with `@JsonFormat`

### Changes, JAX-RS provide

* [#61](../../jackson-jaxrs-providers/pull/61): Fix disabling of `JaxRSFeature` (was always enabling features)

### Changes, other modules

#### [Afterburner](../../jackson-module-afterburner)

* [#52](../../jackson-module-afterburner/issues/52): Invalidating `SerializationInclusion.NON_NULL` of other modules

#### [JSON Schema](../../jackson-module-jsonSchema)

* [#20](../../jackson-module-jsonSchema/issues/20): Support handling of "unwrapped" POJOs

#### [Mr Bean](../../jackson-module-mrbean)

* [#20](../../jackson-module-mrbean/issues/20): Serialized beans have extra parameters in JSON
