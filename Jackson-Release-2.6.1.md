Patch version of [2.6](Jackson-Release-2.6), released Aug 9, 2015.

### Changes, core

#### [Streaming](../../jackson-core)

* [#207](../../jackson-core/issues/207): `ArrayIndexOutOfBoundsException` in `ByteQuadsCanonicalizer`

#### [Databind](../../jackson-databind)

* [#873](../../jackson-databind/issues/873): add missing OSGi import
* [#881](../../jackson-databind/issues/881): BeanDeserializerBase having issues with non-CreatorProperty properties.
* [#884](../../jackson-databind/issues/884): ArrayIndexOutOfBoundException in 2.6.0 for `BeanPropertyMap`
* [#889](../../jackson-databind/issues/889): Configuring `ObjectMapper`'s `DateFormat` changes `TimeZone` configuration as well (backwards incompatible with 2.5)
* [#890](../../jackson-databind/issues/890): Exception deserializing a `byte[]` when the target type comes from an annotation

#### Changes, [JAX-RS](../../jackson-jaxrs-providers)

* [#71](../../jackson-jaxrs-providers/issues/71): add missing OSGi import (for `json`)

### Changes, data formats

#### [CSV](../../jackson-dataformat-csv)

* [#87](../../jackson-dataformat-csv/issues/87): Serialization of single Float or Double value leads to incorrect CSV when schema is used

### Changes, datatypes

#### [Java8 Date](../../jackson-datatype-jsr310) (aka JSR-310)

* [#34](../../jackson-datatype-jsr310/issues/34): Allow direct instantiation of standards deserializers, with explicit `DateTimeFormatter` to use, to allow for registering custom-formatter variants
* [#35](../../jackson-datatype-jsr310/issues/35): `LocalTimeDeserializer` does not use configured formatter
* [#37](../../jackson-datatype-jsr310/issues/37): Cannot parse Javascript date using `LocalDateDeserializer`

#### [Joda](../../jackson-datatype-joda)

* [#70](../../jackson-datatype-joda/issues/70): Default DateTime parser format is stricter than previous versions, causing incompatibility

### Changes, other modules

#### [Parameter names](../../jackson-module-parameter-names) (java 8)

 * [#24](../../jackson-module-parameter-names/issues/24): Remove override of json creator mode