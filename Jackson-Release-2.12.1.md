Patch version of [2.12](Jackson-Release-2.12), released on January 8th, 2021.

Following fixes are be included.

### Changes, core

#### [Streaming](../../jackson-core)

#### [Databind](../../jackson-databind)

* [#2962](../../jackson-databind/issues/2962): Auto-detection of constructor-based creator method skipped if there is an annotated factory-based creator method (regression from 2.11)
* [#2972](../../jackson-databind/issues/2972): `ObjectMapper.treeToValue()` no longer invokes `JsonDeserializer.getNullValue()`
* [#2973](../../jackson-databind/issues/2973): DeserializationProblemHandler is not invoked when trying to deserializing String
* [#2978](../../jackson-databind/issues/2978): Fix failing `double` JsonCreators in jackson 2.12.0
* [#2979](../../jackson-databind/issues/2979): Conflicting in POJOPropertiesCollector when having namingStrategy
* [#2990](../../jackson-databind/issues/2990): Breaking API change in `BasicClassIntrospector` (2.12.0)
* [#3005](../../jackson-databind/issues/3005): `JsonNode.requiredAt()` does NOT fail on some path expressions
* [#3009](../../jackson-databind/issues/3009): Exception thrown when `Collections.synchronizedList()` is serialized with type info, deserialized

### Changes, data formats

#### [Ion](../../jackson-dataformats-binary)

* [#282](../../jackson-dataformats-binary/issues/282): Allow disabling native type ids in IonMapper 

#### [XML](../../jackson-dataformat-xml/)

* [#435](../../jackson-dataformat-xml/issues/435): After upgrade to 2.12.0, NPE when deserializing an empty element to `ArrayList`

### Changes, datatypes

#### [Java 8 date/time](../../jackson-modules-java8)

* [#196](../../jackson-modules-java8/issues/196): `@JsonFormat` overriden features don't apply when there are no other
options while deserializing ZonedDateTime

#### [Joda](../../jackson-datatype-joda)

* [#120](../../jackson-datatype-joda/issues/120): Cache formatter with offset parsed (performance improvement)

#### [JSR-353](../../jackson-datatypes-misc)

* [#7](../../jackson-datatypes-misc/issues/7): Jackson JSR353 library is using wrong module name for javax json api

### Changes, other modules

#### Afterburner

* [#120](../../jackson-modules-base/issues/120): Afterburner does not support the new CoercionConfig (and same with Blackbird)

#### Blackbird

* [#123](../../jackson-modules-base/issues/123): BlackBird not support fluent setter

### Changes, JVM Languages

#### [Kotlin](../../jackson-module-kotlin)

* [#402](../../jackson-module-kotlin/issues/402): Remove implicitly-included `java.base` dep in `module-info.java`

#### [Scala](../../jackson-module-scala)

* No functionality changes but `ScalaObjectMapper` has been deprecated because it relies on `Manifest`s and these are not supported in Scala3.

### Changes, other

#### [jackson-jr](../../jackson-jr)

* [#76](../../jackson-jr/issues/76): Annotation-based introspector does not include super-class fields
