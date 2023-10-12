Patch version of [2.5](Jackson Release 2.5), released on February 6, 2015.

Also includes all fixes in 2.4.x up until [2.4.5](Jackson-Release-2.4.5).

### Changes, core

#### [Streaming](../../jackson-core)

* [#178](../../jackson-core/issues/178): Add `Lf2SpacesIndenter.withLinefeed` back to restore binary-compatibility with 2.4.x

#### [Core Databind](../../jackson-databind)

* [#667](../../jackson-databind/issues/667): Problem with bogus conflict between single-arg-String vs `CharSequence` constructor
* [#669](../../jackson-databind/issues/669): #669: JSOG usage of `@JsonTypeInfo` and `@JsonIdentityInfo(generator=JSOGGenerator.class)` fails
* [#671](../../jackson-databind/issues/671): Adding `java.util.Currency` deserialization support for maps
* [#674](../../jackson-databind/issues/674): Spring CGLIB proxies not handled as intended
* [#682](../../jackson-databind/issues/682): Class<?>-valued Map keys not serialized properly
* [#684](../../jackson-databind/issues/684): `FAIL_ON_NUMBERS_FOR_ENUMS` does not fail when integer value is quoted
* [#696](../../jackson-databind/issues/696): Copy constructor does not preserve `_injectableValues`

### Dataformats

#### [XML](../../jackson-dataformat-xml)

* [#133](../../jackson-dataformat-xml/issues/133): Performance regression (2.4->2.5), 10% slower write via databind

#### [CSV](../../jackson-dataformat-csv)

* [#65](../../jackson-dataformat-csv/issues/65): Buffer recycling not always working

### Datatypes

#### [Guava](../../jackson-datatype-guava)

* [#61](../../jackson-datatype-guava/issues/61): NPE serializing `Multimap`s with null values

#### [Joda](../../jackson-datatype-joda)

* [#51](../../jackson-datatype-joda/issues/51): Calling `JodaDateSerializerBase.isEmpty()` results in a `StackOverflowError`.

### Other modules

#### [Afterburner](../../jackson-module-afterburner)

* [#47](../../jackson-module-afterburner/issues/47): java.lang.VerifyError (Illegal type in constant pool ...)
