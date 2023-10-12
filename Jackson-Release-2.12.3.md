Patch version of [2.12](Jackson-Release-2.12), released 12-Apr-2021.

Following fixes will be included in this patch release.

### Changes, core

#### [Streaming](../../jackson-core)

No changes since 2.12.2

#### [Databind](../../jackson-databind)

* [#3108](../../jackson-databind/issues/3108): `TypeFactory` cannot convert `Collection` sub-type without type parameters
to canonical form and back

### Changes, data formats

#### CBOR

* [#259](../../jackson-dataformats-binary/issues/259): Failed to handle case of alleged String with length of Integer.MAX_VALUE
* [#261](../../jackson-dataformats-binary/issues/261): `CBORParser` need to validate zero-length byte[] for `BigInteger`
* [#269](../../jackson-dataformats-binary/issues/269): CBOR loses `Map` entries with specific `long` Map key values (32-bit boundary)

#### Ion

* [#270](../../jackson-dataformats-binary/issues/270): Ion Polymorphic deserialization in 2.12 breaks wrt use of Native Type Ids
when upgrading from 2.8

#### Smile

* [#257](../../jackson-dataformats-binary/issues/257): Uncaught validation problem wrt Smile `BigDecimal` type
* [#258](../../jackson-dataformats-binary/issues/258): `ArrayIndexOutOfBoundsException` for malformed Smile header
* [#260](../../jackson-dataformats-binary/issues/260): Allocate `byte[]` lazily for longer Smile binary data payloads
* [#263](../../jackson-dataformats-binary/issues/263): Handle invalid chunked-binary-format length gracefully
* [#265](../../jackson-dataformats-binary/issues/265): Allocate `byte[]` lazily for longer Smile binary data payloads
* [#266](../../jackson-dataformats-binary/issues/266): `ArrayIndexOutOfBoundsException` in `SmileParser._decodeShortUnicodeValue()`
* [#268](../../jackson-dataformats-binary/issues/268): Handle sequence of Smile header markers without recursion

##### XML

* [#456](../../jackson-dataformat-xml/issues/456): Fix JsonAlias with unwrapped lists
* [#460](../../jackson-dataformat-xml/issues/460): Deserialization from blank (not empty) String fails for Collections

### Changes, other modules

#### Java 8 date/time

* [#207](../../jackson-modules-java8/issues/207): Fail to serialize `TemporalAdjuster` type with 2.12
