Patch version of [2.11](Jackson-Release-2.11), released October 2nd, 2020.

Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core)

#### [Databind](../../jackson-databind)

* [#2795](../../jackson-databind/issues/2795): Cannot detect creator arguments of mixins for JDK types
* [#2815](../../jackson-databind/issues/2815): Add `JsonFormat.Shape` awareness for UUID serialization (`UUIDSerializer`)
* [#2821](../../jackson-databind/issues/2821): Json serialization fails or a specific case that contains generics and static methods with generic parameters (2.11.1 -> 2.11.2 regression)
* [#2822](../../jackson-databind/issues/2822): Using JsonValue and JsonFormat on one field does not work as expected
* [#2840](../../jackson-databind/issues/2840): `ObjectMapper.activateDefaultTypingAsProperty()` is not using parameter `PolymorphicTypeValidator`
* [#2846](../../jackson-databind/issues/2846): Problem deserialization "raw generic" fields (like `Map`) in 2.11.2

### Changes, data formats

#### [Avro](../../jackson-dataformats-binary/)

* [#219](../../jackson-dataformats-binary/issues/219): Cache record names to avoid hitting class loader

#### [CSV](../../jackson-dataformats-text/)

* [#217](../../jackson-dataformats-text/issues/217): Should quote strings with line separator under STRICT_CHECK_FOR_QUOTING mode

### Changes, datatypes

#### [Collections](../../jackson-datatypes-collections)

* [#71](../../jackson-datatypes-collections/issues/71): (eclipse-collections) can not deserialize concrete class instance inside nested immutable eclipse-collection

### Changes, other modules

#### Mr Bean

* [#109](../../jackson-modules-base/issues/109): Fix detection of inherited default method in Java 8+ interface

### Changes, JVM Languages

#### [Scala](../../jackson-module-scala)

* [#472](../../jackson-module-scala/issues/472): `Either` deserializers `Option[T]` with value None as `null`.
