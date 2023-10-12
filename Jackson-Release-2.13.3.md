Patch version of [2.13](Jackson-Release-2.13), released on May 14, 2022.

Following fixes are included in this patch release.

### Changes, core

#### [Streaming](../../jackson-core)

* [#744](../../jackson-core/issues/744): Limit size of exception message in `BigDecimalParser`

None yet

#### [Databind](../../jackson-databind)

* [#2816](../../jackson-databind/issues/2816): Optimize UntypedObjectDeserializer wrt recursion [CVE-2020-36518]
* [#3412](../../jackson-databind/issues/3412): Version 2.13.2 uses `Method.getParameterCount()` which is not supported on Android before API 26
* [#3419](../../jackson-databind/issues/3419): Improve performance of `UnresolvedForwardReference` for forward reference resolution
* [#3446](../../jackson-databind/issues/3446): `java.lang.StringBuffer` cannot be deserialized
* [#3450](../../jackson-databind/pull/3450): DeserializationProblemHandler is not working with wrapper type when returning null

### Changes, data formats

#### Ion

* [#317](../../jackson-dataformats-binary/issues/317): IonValueDeserializer does not handle getNullValue correctly for a missing property

### Changes, Other modules

#### Blackbird

* [#169](../../jackson-modules-base/issues/169): Blackbird fails with LinkageError when the same class is used across two separate classloaders
