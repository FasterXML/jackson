The first patch version of [2.8](Jackson-Release-2.8), released July 20th, 2016.

Following fixes are included.

### Changes, core

#### [Databind](../../jackson-databind/)

* [#1256](../../jackson-databind/issues/1256): `Optional.empty()` not excluded if property declared with type `Object`
* [#1288](../../jackson-databind/issues/1288): Type id not exposed for `JsonTypeInfo.As.EXTERNAL_PROPERTY` even when `visible` set to `true`
* [#1289](../../jackson-databind/issues/1289): Optimize construction of `ArrayList`, `LinkedHashMap` instances
* [#1291](../../jackson-databind/issues/1291): Backward-incompatible behaviour of 2.8: deserializing enum types with two static factory methods fail by default
* [#1297](../../jackson-databind/issues/1297): Deserialization of generic type with `Map.class`
* [#1302](../../jackson-databind/issues/1302): NPE for `ResolvedRecursiveType` in 2.8.0 due to caching

### Changes, [JAX-RS](../../jackson-jaxrs-providers)

* [#87](../../jackson-jaxrs-providers/issues/87): `JacksonJaxbJsonProvider` should use the real "value.getClass()" to build the root type
