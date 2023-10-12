Patch version released on 17-Jul-2014. Following changes are included.

### Core components

#### [Core Databind](../../jackson-databind)

* [#459](../../jackson-databind/issues/459): `BeanDeserializerBuilder` copy constructor not copying `_injectables`
* [#462](../../jackson-databind/issues/462): Annotation-provided Deserializers are not contextualized inside CreatorProperties

### Data formats

#### [Avro](../../jackson-dataformat-avro)

* [#8](../../jackson-dataformat-avro/issues/8): Error in creating Avro Schema for `java.util.Date` (and related) type.

#### [CSV](../../jackson-dataformat-csv)

* [#38](../../jackson-dataformat-csv/issues/38): Extra column delimiter added when column is escaped and follows empty column
* [#41](../../jackson-dataformat-csv/issues/41): `CvsParser.getText()` not working for field name (causing failures for "untyped" deserialization)
* [#47](../../jackson-dataformat-csv/issues/47): UTF-8 BOM handling not working (reported by andrealexandre@github)

#### [Smile](../../jackson-dataformat-smile)

* [#18](../../jackson-dataformat-smile/issues/18): Shared keys can cause unescaped write of `BYTE_MARKER_END_OF_CONTENT`

### Datatypes

#### [Guava](../../jackson-datatype-guava)

* [#42](../../jackson-datatype-guava/issues/42): Polymorphic deserialization involving Guava Optional<T> is broken
