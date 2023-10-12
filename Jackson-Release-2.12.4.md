Patch version of [2.12](Jackson-Release-2.12), released 06-Jul-2021.

Following fixes will be included in this patch release.

### Changes, core

#### [Streaming](../../jackson-core)

* [#702](../../jackson-core/issues/702): `ArrayOutOfBoundException` at `WriterBasedJsonGenerator.writeString(Reader, int)`

#### [Databind](../../jackson-databind)

* [#3139](../../jackson-databind/issues/3139): Deserialization of "empty" subtype with `DEDUCTION` failed
* [#3146](../../jackson-databind/issues/3146): Merge findInjectableValues() results in AnnotationIntrospectorPair
* [#3171](../../jackson-databind/issues/3171): READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE doesn't work with empty strings

### Changes, data formats

#### [CBOR](../../jackson-dataformats-binary)

* [287](../../jackson-dataformats-binary/issues/287): Uncaught exception in CBORParser._nextChunkedByte2() (by ossfuzzer)
* [288](../../jackson-dataformats-binary/issues/288): Uncaught exception in CBORParser._findDecodedFromSymbols() (by ossfuzzer)

#### [XML](../../jackson-dataformat-xml)

* [#469](../../jackson-dataformat-xml/issues/469): Empty tags cause incorrect deserialization of unwrapped lists
* [#473](../../jackson-dataformat-xml/issues/473): Parsing of null Integer fields changed behavior between version 2.11 and 2.12
* [#482](../../jackson-dataformat-xml/issues/482): Use of non-Stax2-compatible Stax2 implementation fails when reading from byte[]

#### [YAML](../../jackson-dataformats-text)

* [#274](../../jackson-dataformats-text/issues/274): YAMLGenerator does not quote tilde (~) characters when MINIMIZE_QUOTES is enabled

### Changes, Java 8 modules

* [#214](../../jackson-modules-java8/issues/214): readerForUpdating(objectToUpdate).readValue(json) behaves unexpectedly
on Optional<List>

### Changes, other modules

#### Afterburner

* [#131](../../jackson-modules-base/issues/131): Failing to serialize `Thread` returned by `Thread.currentThread()` when Afterburner or Blackbird registered

#### Mr Bean

* [#132](../../jackson-modules-base/issues/132): (partial fix) Prevent materialization of `java.util.TimeZone`
