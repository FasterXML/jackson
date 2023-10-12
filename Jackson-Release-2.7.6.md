Patch version of [2.7](Jackson-Release-2.7), released on 23-Jul-2016

Following fixes will be included.

### Changes, core

#### [Databind](../../jackson-databind/)

* [#1215](../../jackson-databind/issues/1215): Problem with type specialization for Maps with `@JsonDeserialize(as=subtype)`
* [#1279](../../jackson-databind/issues/1279): Ensure DOM parsing defaults to not expanding external entities
* [#1288](../../jackson-databind/issues/1288): Type id not exposed for `JsonTypeInfo.As.EXTERNAL_PROPERTY` even when `visible` set to `true`
* [#1301](../../jackson-databind/issues/1301): Problem with `JavaType.toString()` for recursive (self-referential) types

### Changes, other modules

#### [Base](../../jackson-modules-base)

* [#7](../../jackson-modules-base/issues/7): (afterburner) Afterburner excludes serialization of some `null`-valued Object properties
* [#12](../../jackson-modules-base/issues/12): (mrbean) Problem deserializing Long into Calendar with mrBean
* [#13](../../jackson-modules-base/issues/13): (paranamer) Make `ParanamerAnnotationIntrospector` serializable
