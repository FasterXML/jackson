Patch version of [2.8](Jackson-Release-2.8), released on 24-Aug-2017.

Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core)

#### [Databind](../../jackson-databind)

* [#1657](../../jackson-databind/issues/1657): `StdDateFormat` deserializes dates with no tz/offset as UTC instead of configured timezone
* [#1658](../../jackson-databind/issues/1658): Infinite recursion when deserializing a class extending a Map, with a recursive value type
* [#1679](../../jackson-databind/issues/1679): `StackOverflowError` in Dynamic `StdKeySerializer`
* [#1711](../../jackson-databind/issues/1711): Delegating creator fails to work for binary data (`byte[]`) with binary formats (CBOR, Smile)
* [#1735](../../jackson-databind/issues/1735): Missing type checks when using polymorphic type ids
* [#1737](../../jackson-databind/issues/1737): Block more JDK types from polymorphic deserialization

### Changes, dataformats

##### [Protobuf](../../jackson-dataformats-binary)

* [#94](../../jackson-dataformats-binary/issues/94): Should _ensureRoom in ProtobufGenerator.writeString()

### Changes, datatypes

#### [Collections](../../jackson-datatype-collections)

* [#19](../../jackson-datatype-collections/issues/19): `Multimap` serializer produces wrong Schema structure

### Changes, [Java 8 support](../../jackson-modules-java8/)

* [#33](../../jackson-modules-java8/issues/33): `Jdk8Serializer.findReferenceSerializer()` leads to `StackOverflowError` in 2.8.9

### Changes, other

#### [Jackson jr](../../jackson-jr)

* [#53](../../jackson-jr/issues/53): `java.io.File` is not a valid source for anyFrom()/mapFrom()
