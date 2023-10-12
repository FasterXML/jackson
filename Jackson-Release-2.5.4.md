Patch version of [2.5](Jackson Release 2.5), released Jun 9, 2015.

### Changes, core

#### [Databind](../../jackson-databind)

* [#676](../../jackson-databind/issues/676): Deserialization of class with generic collection inside depends on how is was deserialized first time
* [#771](../../jackson-databind/issues/771): Annotation bundles ignored when added to Mixin
* [#774](../../jackson-databind/issues/774): `NPE` from `SqlDateSerializer` as `_useTimestamp` is not checked for being null
* [#785](../../jackson-databind/issues/785): Add handling for classes which are available in `Thread.currentThread().getContextClassLoader()`
* [#792](../../jackson-databind/issues/792): Ensure Constructor Parameter annotations are linked with those of Field, Getter, or Setter
* [#793](../../jackson-databind/issues/793): `ObjectMapper.readTree()` does not work with defaultTyping enabled
* [#801](../../jackson-databind/issues/801): Using `@JsonCreator` cause generating invalid path reference in `JsonMappingException`
* [#815](../../jackson-databind/issues/815): Presence of `PropertyNamingStrategy` makes deserialization fail
* [#816](../../jackson-databind/issues/816): Allow date-only ISO strings to have no time zone.

### Changes, datatypes

#### [Joda](../../jackson-datatype-joda)

* [#60](../../jackson-datatype-joda/issues/60): Configured date/time format not considered when serializing Joda Instant

#### [JSR-310](../../jackson-datatype-jsr310)

* [#24](../../jackson-datatype-jsr310/24): ZoneId type information improperly handled when default typing enabled

### Changes, JAX-RS

* [#63](../../jackson-jaxrs-providers/issues/63): Support JAX-RS 2.0 in OSGi environment for Smile, CBOR too
