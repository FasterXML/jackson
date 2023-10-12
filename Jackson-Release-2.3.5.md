Patch version of [2.3](Jackson Release 2.3), released on January 13th, 2015.

[2.3](Jackson Release 2.3) branch as it is still open, being the last branch for both JDK 1.5 and Scala 2.9.

Following changes are included in this release.

### Core components

#### [Streaming](../../jackson-core)

* [#152](../../jackson-core/issues/152): Exception for property names longer than 256k
* [#173](../../jackson-core/issues/173): An exception is thrown for a valid JsonPointer expression
* [#176](../../jackson-core/issues/176): `JsonPointer` should not consider "00" to be valid index

#### [Core Databind](../../jackson-databind)

* [#496](../../jackson-databind/issues/496): Wrong result for `TextNode("false").asBoolean(true)`
* [#543](../../jackson-databind/issues/543): Problems resolving self-referential generic types.
* [#656](../../jackson-databind/issues/656): `defaultImpl` configuration is ignored for WRAPPER_OBJECT

### Datatypes

#### [Guava](../../jackson-datatype-guava)

* [#46](../../jackson-datatype-guava/issues/46): Can not serialize Guava `Iterable`s (backported from 2.4.2)
