Patch version of [2.9](Jackson-Release-2.9), released on 08-Sep-2017.

Following fixes are included, in addition to all fixes included in [2.8.10](Jackson-Release-2.8.10).

### Changes, core

#### [Streaming](../../jackson-core)

* [#397](../../jackson-core/issues/397): Add `Automatic-Module-Name` ("com.fasterxml.jackson.core") for JDK 9 module system

#### [Databind](../../jackson-databind)

* [#1725](../../jackson-databind/issues/1725): `NPE` In `TypeFactory. constructParametricType(...)`
* [#1730](../../jackson-databind/issues/1730): InvalidFormatException` for `JsonToken.VALUE_EMBEDDED_OBJECT`
* [#1745](../../jackson-databind/issues/1745): `StdDateFormat`: accept and truncate millis larger than 3 digits
* [#1749](../../jackson-databind/issues/1749): `StdDateFormat`: performance improvement of '_format(..)' method
* [#1759](../../jackson-databind/issues/1759): Reuse `Calendar` instance during parsing by `StdDateFormat`

### Changes, dataformats

#### Binary formats

* [#102](../../jackson-dataformats-binary/issues/102): (ion) Make `IonValueModule` public for use outside of `IonValueMapper`
* [#108](../../jackson-dataformats-binary/issues/108): (protobuf) fix NPE in skip unknown nested key

#### Textual formats

* [#34](../../../jackson-dataformats-text/issues/34): (yaml) Problem with `YAMLGenerator.Feature.INDENT_ARRAYS`, nested Objects
