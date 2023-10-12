Patch version of [2.11](Jackson-Release-2.11), released on 02-Aug-2020.

Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core)

No changes

#### [Databind](../../jackson-databind)

* [#2783](../../jackson-databind/issues/2783): Parser/Generator features not set when using `ObjectMapper.createParser()`, `createGenerator()`
* [#2785](../../jackson-databind/issues/2785): Polymorphic subtypes not registering on copied ObjectMapper (2.11.1)
* [#2789](../../jackson-databind/issues/2789): Failure to read `AnnotatedField` value in Jackson 2.11
* [#2796](../../jackson-databind/issues/2796): `TypeFactory.constructType()` does not take `TypeBindings` correctly

### Changes, data formats

#### [Avro](../../jackson-dataformats-binary/)

* [#216](../../jackson-dataformats-binary/issues/216): Avro null deserialization

### Changes, datatypes

#### [Hibernate](../../jackson-datatype-hibernate)

* [#97](../../jackson-datatype-hibernate/issues/97): `@JsonUnwrapped` fails for HibernateProxy instances

### Changes, JVM Languages

#### [Scala](../../jackson-module-scala)

* [#454](../../jackson-module-scala/issues/454): Jackson module scala potentially breaks serialization for swagger Model

### Changes, other

#### [jackson-jr](../../jackson-jr)

* [#71](../../jackson-jr/issues/71): jackson-jr-stree-2.11.1.jar is missing util package classes
