Patch version of [2.4](Jackson Release 2.4), released on January 13th, 2015.
May be the last release version of 2.4.x branch, since 2.5.0 was released before this release; and since 2.3 will be a long-running branch (due to compatibility reasons). 2.4.x on the other hand may be retired after (or around time) 2.5.1 is released.

### Changes, core

#### [Core Databind](../../jackson-databind)

* [#635](../../jackson-databind/issues/635): Reduce cachability of `Map` deserializers, to avoid problems with per-property config changes
* [#656](../../jackson-databind/issues/656): `defaultImpl` configuration is ignored for `WRAPPER_OBJECT`

### Changes, data formats

#### [YAML](../../jackson-dataformat-yaml/)

* [#30](../../jackson-dataformat-yaml/issues/30): `YamlFactory.writeValue(File, Object)` busted

### Changes, data types

#### [Guava](../../jackson-datatype-guava)

* [#58](../../jackson-datatype-guava/issues/58): `FluentIterable` serialization doesn't work for Bean properties

#### [JSR-353](../../jackson-datatype-jsr353)

* [#5](../../jackson-datatype-jsr353/issues/5): binary nodes not supported during deserialization

### Changes, other modules

#### [Afterburner](../../jackson-module-afterburner)

* [#48](../../jackson-module-afterburner/issues/48): Problem passing custom `JsonSerializer`, causing an NPE
