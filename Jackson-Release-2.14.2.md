Patch version of [2.14](Jackson-Release-2.14), released January 28, 2023.

Following fixes are included in this patch release.

### Changes, core

#### [Streaming](../../jackson-core)

* [#854](../../jackson-core/issues/854): Backport schubfach changes from v2.15
* [#882](../../jackson-core/issues/882): Allow TokenFIlter to skip last elements in arrays
* [#886](../../jackson-core/issues/886): Avoid instance creations in fast parser code
* [#890](../../jackson-core/issues/890): `FilteringGeneratorDelegate` does not create new `filterContext` if `tokenFilter` is null

#### [Databind](../../jackson-databind)

* [#1751](../../jackson-databind/issues/1751): `@JsonTypeInfo` does not work if the Type Id is an Integer value
* [#3063](../../jackson-databind/issues/3063): `@JsonValue` fails for Java Record
* [#3699](../../jackson-databind/issues/3699): Allow custom `JsonNode` implementations
* [#3711](../../jackson-databind/issues/3711): Enum polymorphism not working correctly with DEDUCTION
* [#3741](../../jackson-databind/issues/3741): `StdDelegatingDeserializer` ignores `nullValue` of `_delegateDeserializer`.

### Changes, data formats

#### TOML

* [#356](../../jackson-dataformats-text/pull/356): Fix TOML parse failure when number token hits buffer edge

### Changes, datatypes

#### JSONP/JSR-353

* [#28](../../jackson-datatypes-misc/issues/28): Add delegating serializers for `JsonPatch` and `JsonMergePatch`

### Changes, other modules

#### JAX-RS Providers

* [#167](../../jackson-jaxrs-providers/issues/166): `ProviderBase` class shows contention on synchronized block using `LRUMap` _writers instance

#### Jakarta RS Providers

* [#12](../../jackson-jakarta-rs-providers/pulls/12): Remove unnecessary synchronization from endpoint reader/writer caches
