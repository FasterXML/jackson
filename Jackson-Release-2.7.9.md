Patch version of [2.7](Jackson-Release-2.7), released on 04 February 2017.

This is planned to be the last full 2.7 release of all components: further releases, if any, will be micro-releases (like next hypothetical one, `2.7.9.1`) of specific components with issues.

Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core)

#### [Databind](../../jackson-databind)

* [#1367](../../jackson-databind/issues/1367): No Object Id found for an instance when using `@ConstructorProperties`
* [#1392](../../jackson-databind/issues/1392): Custom UnmodifiableSetMixin Fails in Jackson 2.7+ but works in 
Jackson 2.6
* [#1411](../../jackson-databind/issues/1411): MapSerializer._orderEntries should check for null keys
* [#1432](../../jackson-databind/issues/1432): Off by 1 bug in PropertyValueBuffer
* [#1439](../../jackson-databind/issues/1439): NPE when using with filter id, serializing `java.util.Map` types
* [#1456](../../jackson-databind/issues/1456): `TypeFactory` type resolution broken in 2.7 for generic types when using `constructType` with context
* [#1476](../../jackson-databind/issues/1476): Wrong constructor picked up when deserializing object
* [#1506](../../jackson-databind/issues/1506): Missing `KeyDeserializer` for `CharSequence`

### Changes, dataformats

#### [Avro](../../jackson-dataformat-avro)

* Support multiple root-level value writes (via `SequenceWriter`)
* Support multiple root-level value reads (via `MappingIterator`)

#### [YAML](../../jackson-dataformat-yaml)

* [#80](../../jackson-dataformat-yaml/issues/80): Fix UTF8Writer when used in same thread

### Changes, [Jackson jr](../../jackson-jr)

* [#49](../../jackson-jr/issues/49): `ArrayIndexOutOfBoundsException` when parsing large Map

### Changes, other

#### [JAXB Annotations](../../jackson-module-jaxb-annotations)

* [#63](../../jackson-module-jaxb-annotations/issues/63): Error in type resolution of reference type (`Optional`)
