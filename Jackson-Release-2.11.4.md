Patch version of [2.11](Jackson-Release-2.11), released December 12, 2020.

It may be the last full minor version for 2.11.x since 2.12.0 has been released.

Following fixes are included.

### Changes, core

#### [Streaming](../../jackson-core)

* [#647](../../jackson-core/issues/647): Fix NPE in `writeNumber(String)` method of `UTF8JsonGenerator`, `WriterBasedJsonGenerator`

#### [Databind](../../jackson-databind)

* [#2894](../../jackson-databind/issues/2894): Fix type resolution for static methods (regression in 2.11.3)
* [#2944](../../jackson-databind/issues/2944): `@JsonCreator` on constructor not compatible with `@JsonIdentityInfo`, `PropertyGenerator`

### Changes, data formats

#### CBOR

* [#186](../../jackson-dataformats-binary/issues/186): Eager allocation of byte buffer can cause `java.lang.OutOfMemoryError` exception (CVE-2020-28491)
