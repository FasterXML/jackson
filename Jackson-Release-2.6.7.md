Patch version of [2.6](Jackson-Release-2.6), released June 5th, 2016.

This is planned to be the last full 2.6 release of all components: further releases, if any, will be micro-releases of specific components with issues. They will be listed under [2.6.7.x](Jackson-Release-2.6.7.x).

### Changes, core

#### [Streaming](../../jackson-core)

* [#280](../../jackson-core/issues/280): FilteringGeneratorDelegate.writeUTF8String() should delegate to writeUTF8String()

#### [Databind](../../jackson-databind)

* [#1194](../../jackson-databind/issues/1194): Incorrect signature for generic type via `JavaType.getGenericSignature()`
* [#1228](../../jackson-databind/issues/1228): `@JsonAnySetter` does not deserialize null to Deserializer's NullValue
* [#1255](../../jackson-databind/issues/1255): `@JsonIdentityInfo` incorrectly serializing forward references

### [Kotlin](../../jackson-module-kotlin)

* Various fixes

