Patch version of [2.6](Jackson-Release-2.6), released January 19, 2016.

### Changes, core

#### [Databind](../../jackson-databind/issues)

* [#1052](../../jackson-databind/issues/1052): Don't generate a spurious `NullNode` after parsing an embedded object
* [#1061](../../jackson-databind/issues/1061): Problem with Object Id and Type Id as Wrapper Object
* [#1073](../../jackson-databind/issues/1073): Add try-catch around `java.sql` type serializers

### Changes, dataformats

#### [CBOR](../../jackson-dataformat-cbor)

* [#15](../../jackson-dataformat-cbor/issues/15): `CBORParser.getNumberType()` returns `DOUBLE` even if the generator has been fed with a float

#### [CSV](../../jackson-dataformat-csv)

* [#93](../../jackson-dataformat-csv/issues/93): CSV mapper does not support Views or filtering correctly for serialization
* [#96](../../jackson-dataformat-csv/issues/96): SortedBy only apply to headers and actual data
* [#100](../../jackson-dataformat-csv/issues/100): trim spaces: don't trim/strip separator character

#### [Smile](../../jackson-dataformat-smile)

* [#30](../../jackson-dataformat-smile/issues/30): Problem decoding "empty" Map key (String with length 0) with `nextFieldName()`

#### [XML](../../jackson-dataformat-xml)

* [#177](../../jackson-dataformat-xml/issues/177): Failure to deserialize unwrapped list where entry has empty content, attribute(s)

### Changes, datatypes

#### [Hibernate](../../jackson-datatype-hibernate)

* [#70](../../jackson-datatype-hibernate/issues/70): Infinite recursion due to `@JsonIgnoreProperties` not passed to property
