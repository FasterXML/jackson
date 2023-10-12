Patch version of [2.15](Jackson-Release-2.15), under development as of October 2023.

Following fixes will be included in this patch release.

### Changes, core

#### [Streaming](../../jackson-core)

* [#1111](../../jackson-core/pull/1111): Call the right `filterFinishArray()`/`filterFinishObject()` from `FilteringParserDelegate`

#### [Databind](../../jackson-databind)

* [#3968](../../jackson-databind/issues/3968): Records with additional constructors failed to deserialize

### Changes, dataformats

#### Smile

* [#384](../../jackson-dataformats-binary/issues/384): `Smile` decoding issue with `NonBlockingByteArrayParser`, concurrency

#### YAML

* [#400](../../jackson-dataformats-text/issues/400): `IllegalArgumentException` when attempting to decode invalid UTF-8 surrogate by SnakeYAML
* [#406](../../jackson-dataformats-text/issues/406): `NumberFormatException` from SnakeYAML due to int overflow for corrupt YAML version
* [#426](../../jackson-dataformats-text/issues/426): Update to SnakeYAML 2.1

### Changes, other

#### [Jackson-jr](../../jackson-jr)

* [#107](../../jackson-jr/issues/107): Cannot deserialize `byte[]` from JSON `null` value

